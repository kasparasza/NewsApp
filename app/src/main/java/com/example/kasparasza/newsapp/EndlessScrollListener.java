package com.example.kasparasza.newsapp;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.AbsListView;
import android.widget.Toast;

/**
 * An abstract class that will be implemented by our ListView.
 * It will load automatically more items as the user scrolls through the items (aka infinite scroll).
 * This is done by triggering a request for more data once the user crosses a threshold of remaining items before they've hit the end.
 * Source: https://github.com/codepath/android_guides/wiki/Endless-Scrolling-with-AdapterViews-and-RecyclerView
 */

public abstract class EndlessScrollListener implements AbsListView.OnScrollListener {
    // The minimum number of items to have below your current scroll position
    // before loading more.
    private int visibleThreshold = 5;
    // The current offset index of data you have loaded
    private int currentPage = 0;
    // The total number of items in the dataset after the last load
    private int previousTotalItemCount = 0;
    // True if we are still waiting for the last set of data to load.
    private boolean loading = true;
    // Sets the starting page index
    private int startingPageIndex = 0;
    // context will be needed in order to access SystemService - CONNECTIVITY_SERVICE
    private Context context;

    public EndlessScrollListener(Context mContext) {
        context = mContext;
    }

    public EndlessScrollListener(int visibleThreshold) {
        this.visibleThreshold = visibleThreshold;
    }

    public EndlessScrollListener(int visibleThreshold, int startPage) {
        this.visibleThreshold = visibleThreshold;
        this.startingPageIndex = startPage;
        this.currentPage = startPage;
    }

    // custom constructor
    public EndlessScrollListener(int startPage, Context mContext) {
        this.startingPageIndex = startPage;
        this.currentPage = startPage;
        context = mContext;
    }

    /**
     * Method that checks whether there is a network connection
     * @return boolean that is true is there is a connection
     */
    private boolean checkNetworkConnection() {
        ConnectivityManager cm =
                (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        return isConnected;
    }


    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
    {
        // actions are being performed only if there is a Network Connection present
        // without this check a network request might be initiated (even if there is no network connection),
        // and the value of currentPage would increase
        if(checkNetworkConnection()){
            // If the total item count is zero and the previous isn't, assume the
            // list is invalidated and should be reset back to initial state
            if (totalItemCount < previousTotalItemCount) {
                this.currentPage = this.startingPageIndex;
                this.previousTotalItemCount = totalItemCount;
                if (totalItemCount == 0) { this.loading = true; }
            }


            // If it's still loading, we check to see if the dataset count has
            // changed, if so we conclude it has finished loading and update the current page
            // number and total item count.
            if (loading && (totalItemCount > previousTotalItemCount)) {
                loading = false;
                previousTotalItemCount = totalItemCount;
                currentPage++;
            }

            // If it isn't currently loading, we check to see if we have breached
            // the visibleThreshold and need to reload more data.
            // If we do need to reload some more data, we execute onLoadMore to fetch the data.
            if (!loading && (firstVisibleItem + visibleItemCount + visibleThreshold) >= totalItemCount ) {
                loading = onLoadMore(currentPage + 1, totalItemCount);
            }
        }
    }

    // Defines the process for actually loading more data based on page
    // Returns true if more data is being loaded; returns false if there is no more data to load.
    public abstract boolean onLoadMore(int page, int totalItemsCount);

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // Don't take any action on changed
    }

}
