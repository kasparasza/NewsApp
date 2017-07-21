package com.example.kasparasza.newsapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Implementation of a custom List adapter which will populate a layout with data on Book objects
 */

// constructor of the adapter
public class NewsArticleListAdapter extends ArrayAdapter<NewsArticle> {
    public NewsArticleListAdapter(Context context, int resource, List<NewsArticle> newsArticles) {
        super(context, 0, newsArticles);
    }

    // overriding getView method that will create ListView items
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        // instruct the method to reuse the views
        View listViewItem = convertView;
        if (listViewItem == null) {
            listViewItem = LayoutInflater.from(getContext()).inflate(R.layout.news_article_item_in_list_view, parent, false);
        }

        // initialise the views that will be populated with data
        TextView headlineView = (TextView) listViewItem.findViewById(R.id.headline_view);
        TextView authorView = (TextView) listViewItem.findViewById(R.id.author_view);
        TextView startTextView = (TextView) listViewItem.findViewById(R.id.start_text_view);
        TextView publishingTimeView = (TextView) listViewItem.findViewById(R.id.publishing_time_view);
        TextView sectionView = (TextView) listViewItem.findViewById(R.id.section_view);
        ImageView imageView = (ImageView) listViewItem.findViewById(R.id.image_view);


        // before getting items from the list, we want to make sure that the position we request is within the size of the list
        if (position < getCount()) {

            // get each item from the List
            NewsArticle currentListItem = getItem(position);

            // populate the views with data
            headlineView.setText(currentListItem.getHeadline());
            authorView.setText(currentListItem.getAuthor());
            startTextView.setText(Html.fromHtml(currentListItem.getStartText())); // Html.fromHtml() formats the TextView to use Html tags
            publishingTimeView.setText(currentListItem.getTimePublished());
            sectionView.setText(currentListItem.getSection());
            // use of Picasso library to set ImageView
            // at first we check, whether the String with image link is not empty
            if (!currentListItem.getImageLink().matches("")) {
                Picasso.with(getContext())
                        .load(currentListItem.getImageLink())
                        .resize((int) getContext().getResources().getDimension(R.dimen.width_of_article_image), (int) getContext().getResources().getDimension(R.dimen.height_of_article_image))
                        .placeholder(R.drawable.image_placeholder)
                        .error(R.drawable.no_image_to_download)
                        .centerCrop()
                        .into(imageView);
            } else {
                Picasso.with(getContext())
                        .load(R.drawable.no_image_to_download)
                        .resize((int) getContext().getResources().getDimension(R.dimen.width_of_article_image), (int) getContext().getResources().getDimension(R.dimen.height_of_article_image))
                        .centerCrop()
                        .into(imageView);
            }
        }

        // returns an inflated ListView Item
        return listViewItem;
    }
}
