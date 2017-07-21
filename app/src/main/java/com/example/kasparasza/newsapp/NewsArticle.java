package com.example.kasparasza.newsapp;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class where each member of the class is a NewsArticle
 */

public class NewsArticle implements Parcelable{

    // private members of the class
    private String headline;
    private String author;
    private String timePublished;
    private String startText;
    private String imageLink;
    private String articleLink;
    private String section;

    // declaration of String constants used by the class
    protected static final String NEWS_ARTICLE = "NEWS ARTICLE";
    protected static final String NO_HEADLINE_STRING = "no headline";
    protected static final String NO_AUTHOR_STRING  = "author not given";
    protected static final String NO_SECTION_STRING  = "general";


    // constructors of the class
    public NewsArticle (String mHeadline, String mAuthor, String mTimePublished, String mStartText, String mImageLink,
                        String mArticleLink, String mSection){
        headline = mHeadline;
        author = mAuthor;
        timePublished = mTimePublished;
        startText = mStartText;
        imageLink = mImageLink;
        articleLink = mArticleLink;
        section = mSection;
    }

    // implementation of getter methods
    public String getHeadline() {
        return headline;
    }

    public String getAuthor() {
        return author;
    }

    public String getTimePublished() {
        return timePublished;
    }

    public String getStartText() {
        return startText;
    }

    public String getImageLink() {
        return imageLink;
    }

    public String getArticleLink() {
        return articleLink;
    }

    public String getSection() {
        return section;
    }

    //// implementation of Parcelable methods:

    private NewsArticle(Parcel in) {
        // The order must match the order in writeToParcel()
        headline = in.readString();
        author = in.readString();
        timePublished = in.readString();
        startText = in.readString();
        imageLink = in.readString();
        articleLink = in.readString();
        section = in.readString();
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(headline);
        out.writeString(author);
        out.writeString(timePublished);
        out.writeString(startText);
        out.writeString(imageLink);
        out.writeString(articleLink);
        out.writeString(section);
    }

    // method required to be implemented by Parcelable
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<NewsArticle> CREATOR = new Parcelable.Creator<NewsArticle>() {
        public NewsArticle createFromParcel(Parcel in) {
            return new NewsArticle(in);
        }

        public NewsArticle[] newArray(int size) {
            return new NewsArticle[size];
        }
    };



}

