package com.example.kasparasza.newsapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<NewsArticle>> {

    // String constants used:
    private static final String LOG_TAG = AppUtilities.class.getSimpleName();
    private static final String LIST_VIEW_ITEM_INDEX = "LIST_VIEW_ITEM_INDEX";
    private static final String LIST_VIEW_TOP = "LIST_VIEW_TOP";
    private static final String EMPTY_VIEW_STATE = "EMPTY_VIEW_STATE";
    private static final String NEWS_ARTICLE_PARCELABLE_LIST = "NEWS_ARTICLE_PARCELABLE_LIST";
    private static final String PAGE_NUMBER_IN_QUERY = "PAGE_NUMBER_IN_QUERY";
    // the initial static url query
    public static final String URL_STRING_BASE_PART_1 = "http://content.guardianapis.com/search?";
    public static final String URL_STRING_BASE_PART_2 = "&show-fields=trailText%2Cbyline%2Cthumbnail";
    public static final String URL_STRING_BASE_PART_3 = "&page-size=20&api-key=test";

    // declaration of layout views
    private ListView newsArticlesListView;
    private TextView headlineView;
    private TextView authorView;
    private TextView startTextView;
    private TextView publishingTimeView;
    private TextView sectionView;
    private ImageView imageView;
    private TextView noArticlesView;
    private ProgressBar progressBar;
    private ImageView noNetworkConnection;

    // declaration of class members:
    // ArrayList that will store NewsArticle objects
    private ArrayList<NewsArticle> newsArticleList = new ArrayList<NewsArticle>();
    // integer that stores page number for the URL query parameters;
    int pageNumberInQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialisation of Views;
        newsArticlesListView = (ListView) findViewById(R.id.news_articles_list_view);
        headlineView = (TextView) findViewById(R.id.headline_view);
        authorView = (TextView) findViewById(R.id.author_view);
        startTextView = (TextView) findViewById(R.id.start_text_view);
        publishingTimeView = (TextView) findViewById(R.id.publishing_time_view);
        sectionView = (TextView) findViewById(R.id.section_view);
        imageView = (ImageView) findViewById(R.id.image_view);
        noArticlesView = (TextView) findViewById(R.id.empty_state_text);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        noNetworkConnection = (ImageView) findViewById(R.id.no_connection_image);

        // if the activity is not being recreated (that is we have clean start with no savedInstance state),
        // we: 1) set OnScrollListener to the ListView; 2) set the initial value for the page number for the URL query.
        // Otherwise: 1) we already have the OnScrollListener - no action; 2) we get the relevant page number for the URL query from the Bundle

        // setting of OnScrollListener:
        // if the activity is being recreated: 1) we get the relevant page number for the URL query from the Bundle;
        // 2) pas this number to the OnScrollListener that we attach here
        if (savedInstanceState != null){

            // get the relevant page number for the URL query from the Bundle
            pageNumberInQuery = savedInstanceState.getInt(PAGE_NUMBER_IN_QUERY, 0);

            // the listener is attached if we have a network connection
            if (checkNetworkConnection()){

                // Attach the onScrollListener to the ListView
                // the Listener will trigger additional NewsArticles to be loaded,
                // when we scroll down to the bottom of the ListView
                // @param: pageNumber for the startPage; context
                newsArticlesListView.setOnScrollListener(new EndlessScrollListener(pageNumberInQuery - 1, this) {

                    // Required implementation of an abstract method onLoadMore
                    @Override
                    public boolean onLoadMore(int page, int totalItemsCount) {

                        // we let the pageNumberInQuery to be controlled by the Listener
                        pageNumberInQuery = page;

                        // Triggered only when new data needs to be appended to the list
                        loadNextDataFromApi(page);

                        return true; // ONLY if more data is actually being loaded; false otherwise.
                    }
                });
            }
        } else {
            // if the activity is not being recreated: 1) set the initial value for the page number for the URL query,
            // set OnScrollListener to the ListView.

            // set the initial value for the page number for the URL query
            pageNumberInQuery = 1;

            // the listener is attached if we have a network connection
            if (checkNetworkConnection()){

                // Attach the onScrollListener to the ListView,
                // the Listener will trigger additional NewsArticles to be loaded,
                // when we scroll down to the bottom of the ListView
                newsArticlesListView.setOnScrollListener(new EndlessScrollListener(this) {

                    // Required implementation of an abstract method onLoadMore
                    @Override
                    public boolean onLoadMore(int page, int totalItemsCount) {

                        // in the initial run both values are equal to "1",
                        // in further runs we let the pageNumberInQuery to be controlled by the Listener
                        pageNumberInQuery = page;

                        // Triggered only when new data needs to be appended to the list
                        loadNextDataFromApi(page);

                        return true; // ONLY if more data is actually being loaded; false otherwise.
                    }
                });

                // a Loader is initialised with a query for initial content
                initiateLoader(pageNumberInQuery);
                // while the query is ongoing - a progress bar is shown
                progressBar.setVisibility(View.VISIBLE);
            } else {
                // if there is no network connection at the time of initial onCreate, call a method that informs the user
                // Note: this check is for initial onCreate only; onRestore state is implemented separately
                informAboutNoNetworkConnection();
                progressBar.setVisibility(View.GONE);
            }
        }

        newsArticlesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String articleUrlLink = newsArticleList.get(position).getArticleLink();
                Intent openArticleInWeb = new Intent(Intent.ACTION_VIEW);
                openArticleInWeb.setData(Uri.parse(articleUrlLink));
                startActivity(openArticleInWeb);
            }
        });


    }

    ////
    // Methods that implement OnScrollListener functionality:
    ////

    /**
     * Method that sends out a network request / initiates a Loader
     * @param offset is a "page" parameter (page number) that will be used in the URL query
     * the parameter is controlled by OnScrollListener
     */
    public void loadNextDataFromApi(int offset) {
        initiateLoader(offset);
    }


    ////
    // Methods that implement Loader functionality:
    ////

    /**
     * Method that initiates a Loader
     * @param queryParamPage "page" parameter (page number) that will be used in the URL query
     */
    public void initiateLoader(int queryParamPage) {

        // We create a Bundle that stores additional arguments to supply to the loader at construction
        Bundle argsForLoader = new Bundle();
        // We put extra parameters to the Bundle
        argsForLoader.putInt(PAGE_NUMBER_IN_QUERY, queryParamPage);

        // finally, we initiate a Loader
        // restartLoader() is called instead of initLoader(), as in case the Loader already exists,
        // additional arguments can not be passed to it with initLoader()
        getSupportLoaderManager().restartLoader(0, argsForLoader, this);
    }


    // implementation of methods that are required by otherwise abstract LoaderManager interface:
    // #1) onCreateLoader, #2) onLoadFinished, #3) onLoaderReset
    // #1) onCreateLoader - create an instance of a Loader if there is no previous one
    @Override
    public Loader<List<NewsArticle>> onCreateLoader(int id, Bundle args) {

        // first we read users preferences made in the SettingsActivity
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // preference of articles to be show from date
        String fromDate = sharedPrefs.getString(
                getString(R.string.settings_from_date_key),
                getString(R.string.settings_from_date_default)
        );
        fromDate = AppUtilities.prepare_From_Date_Parameter(fromDate);

        // preference of sort order
        String orderBy = sharedPrefs.getString(
                getString(R.string.settings_order_by_key),
                getString(R.string.settings_order_by_default)
        );

        // preference of production office order
        String productionOffice = sharedPrefs.getString(
                getString(R.string.settings_production_office_key),
                getString(R.string.settings_production_office_default)
        );

        // we obtain "page" parameter to be used in the URL
        int page = args.getInt(PAGE_NUMBER_IN_QUERY, 1);
        String pageString = String.valueOf(page);

        // using the preferences, we create a request URL
        Uri baseUri = Uri.parse(URL_STRING_BASE_PART_1);
        Uri.Builder uriBuilder = baseUri.buildUpon();
        uriBuilder.appendQueryParameter("production-office", productionOffice);
        uriBuilder.appendQueryParameter("from-date", fromDate);
        uriBuilder.appendQueryParameter("order-by", orderBy);
        baseUri = Uri.parse(uriBuilder.toString() + URL_STRING_BASE_PART_2);
        uriBuilder = baseUri.buildUpon();
        uriBuilder.appendQueryParameter("page", pageString);
        String fullUrlQuery = uriBuilder.toString() + URL_STRING_BASE_PART_3;

        // finally, having the URL request, we initiate a Loader
        return new NewsArticleLoader(this, fullUrlQuery);
    }

    // #2) onLoadFinished - populate UI with the data obtained from http query
    @Override
    public void onLoadFinished(Loader<List<NewsArticle>> loader, List<NewsArticle> data) {
        // when the query is finalized - the progress bar is hidden
        progressBar.setVisibility(View.GONE);

        // if the ListView to be populated has no adapter attached - we create and attach one
        // (this is the case e.g. when there was no orientation change, or change in
        // shared preferences)
        if (newsArticlesListView.getAdapter() == null) {

            // set the data to the ArrayList defined previously
            newsArticleList = (ArrayList<NewsArticle>) data;

            // create an instance of an adapter which will populate the layout with data on NewsArticle objects
            NewsArticleListAdapter listAdapter = new NewsArticleListAdapter(this, 0, newsArticleList);

            // connect the adapter with the root List layout & with the ArrayList data
            newsArticlesListView.setAdapter(listAdapter);

        } else {
            // there is already an adapter attached
            // we append the new data objects to the existing set of items inside the ArrayList
            // notify the adapter about the new items added

            // we get the adapter
            NewsArticleListAdapter adapter = (NewsArticleListAdapter) newsArticlesListView.getAdapter();

            // append the data
            newsArticleList.addAll(data);

            // notify the adapter
            adapter.notifyDataSetChanged();
        }

        // if input List is empty, we have zero NewsArticles to display; an appropriate message is displayed
        newsArticlesListView.setEmptyView(noArticlesView);
        noArticlesView.setText(R.string.no_articles_message);
    }

    // #3) onLoaderReset - clear data on reset
    @Override
    public void onLoaderReset(Loader<List<NewsArticle>> loader) {
        loader.reset();
    }


    //////
    // Methods that check for network connection and provide the feedback regarding it:
    //////

    /**
     * Method that checks whether there is a network connection
     *
     * @return boolean that is true is there is a connection
     */
    public boolean checkNetworkConnection() {
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        return isConnected;
    }

    /**
     * Method that informs User about no network connectivity
     */
    public void informAboutNoNetworkConnection() {
        // if there is no ArrayList of NewsArticles in the memory - show an ImageView with no connectivity message
        if (newsArticleList.size() < 1) {
            newsArticlesListView.setVisibility(View.GONE);
            noNetworkConnection.setImageResource(R.drawable.no_network_image);
            Toast.makeText(this, R.string.no_internet_connection_message, Toast.LENGTH_SHORT).show();
        } else {
            // if there is an ArrayList of NewsArticles in the memory - keep displaying it
            // just inform user via Toast message
            Toast.makeText(this, R.string.no_internet_connection_message, Toast.LENGTH_SHORT).show();
        }
    }


    //////
    // Methods that are used to implement Options Menu:
    //////

    // method that inflates options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


    // method that starts SettingsActivity after the user selects any option item
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        // starts SettingsActivity after the user selects any option item
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        // Restarts MainActivity == reloads the ListView and resets all other members,
        // use: to update the ListView with new articles
        if (id == R.id.action_restart) {
            Intent restartActivity = new Intent(this, MainActivity.class);
            this.finish();
            startActivity(restartActivity);
        }
        return super.onOptionsItemSelected(item);
    }

    //////
    // Methods that are used to save the state of Activity:
    //////

    /**
     * Method that records the state of the Activity if there is a configuration change
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // record the state of the ListView
        // get index and top positions of the ListView
        // index - returns the top visible list item
        int index = newsArticlesListView.getFirstVisiblePosition();
        View view = newsArticlesListView.getChildAt(0);
        // returns relative offset from the top of the list
        int top = (view == null) ? 0 : (view.getTop() - newsArticlesListView.getPaddingTop());

        // get visibility state of noArticlesView(empty == false)
        boolean emptyViewState = noArticlesView.getText().toString().equals("");

        // save items to a bundle
        outState.putInt(LIST_VIEW_ITEM_INDEX, index);
        outState.putInt(LIST_VIEW_TOP, top);
        outState.putBoolean(EMPTY_VIEW_STATE, emptyViewState);
        outState.putParcelableArrayList(NEWS_ARTICLE_PARCELABLE_LIST, newsArticleList);
        outState.putInt(PAGE_NUMBER_IN_QUERY, pageNumberInQuery);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(outState);
    }

    /**
     * Method that restores the state of the Activity after a configuration change
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // Superclass that is being always called
        super.onRestoreInstanceState(savedInstanceState);

        // get information form the Bundle
        if (savedInstanceState != null) {
            // data about the state of the ListView
            // index and top positions of the ListView
            int index = savedInstanceState.getInt(LIST_VIEW_ITEM_INDEX, 0);
            int top = savedInstanceState.getInt(LIST_VIEW_TOP, 0);

            // the ListView itself is being recreated
            newsArticleList = savedInstanceState.getParcelableArrayList(NEWS_ARTICLE_PARCELABLE_LIST);
            NewsArticleListAdapter adapter = new NewsArticleListAdapter(this, 0, newsArticleList);
            newsArticlesListView.setAdapter(adapter);

            // set / restore the position of the ListView
            newsArticlesListView.setSelectionFromTop(index, top);


            //// calibration of progressBarr and noArticlesView:

            // get state of the noArticlesView (empty == false)
            boolean emptyViewState = savedInstanceState.getBoolean(EMPTY_VIEW_STATE);
            // if 1) it is empty and 2) ListView is empty -> set empty view  is called
            if (newsArticleList.size() == 0 && !emptyViewState) {
                newsArticlesListView.setEmptyView(noArticlesView);
                noArticlesView.setText(R.string.no_articles_message);
                progressBar.setVisibility(View.GONE);
            } else if (newsArticleList.size() == 0 && checkNetworkConnection() && emptyViewState){
                // if 1) it is not empty, 2) there is network connection and 3) ListView is empty -> progress bar is shown
                // that is we assume that the previuos query is yet to finnish
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        //// calibration of progressBarr and noArticlesView (continued):

        // set visibility of the ProgressBar and inform the User if there is no network
        // depending on the state of the network and whether the ListView is empty or not

        // if ListView is not empty - progress bar is not shown
        if (newsArticleList.size() > 0) {
            progressBar.setVisibility(View.GONE);
        } else if (newsArticleList.size() == 0 && !checkNetworkConnection()){
            // if 1) ListView is empty, and 2) there is no network connection - progress bar is not shown
            // this also means that noArticlesView (empty != false)
            informAboutNoNetworkConnection();
            progressBar.setVisibility(View.GONE);
        }
    }
}
