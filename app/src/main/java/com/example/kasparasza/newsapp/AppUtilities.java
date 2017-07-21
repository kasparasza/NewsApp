package com.example.kasparasza.newsapp;

import android.app.Activity;
import android.content.Context;
import java.util.Calendar;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.JsonReader;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Custom class that holds static variables and methods required by other classes & activities of the app.
 */

public class AppUtilities {

    // String constants used:
    private static final String LOG_TAG = AppUtilities.class.getSimpleName();
    private static final String AUTHOR_PREFIX = "by ";

    /**
     * Create a private constructor because no one should ever create a {@link AppUtilities} object.
     * This class is only meant to hold static variables and methods, which can be accessed
     * directly from the class name AppUtilities (and an object instance of AppUtilities is not needed).
     */
    private AppUtilities() {
    }


    ////
    // Utility methods that are used to implement an http query and read information from it:
    ////

    /**
     * Executes calls to helper methods and returns a List of NewsArticle objects
     * @param stringWithHttpQuery string that contains URL query
     * @return List<NewsArticle> a List of NewsArticle objects
     */
    static List<NewsArticle> getDataFromHttp(String stringWithHttpQuery) {
        String JSONString = "";
        List<NewsArticle> newsArticleList = new ArrayList<NewsArticle>();

        // check whether input String is valid
        if (stringWithHttpQuery == null) {
            return newsArticleList;
        } else {
            // call a method that transforms String into Url
            URL url = createUrl(stringWithHttpQuery);

            // get http response as a JSON String
            try {
                JSONString = performHttpConnection(url);
                newsArticleList = extractFromJSONString(JSONString);
            } catch (IOException exc_03) {
                Log.e(LOG_TAG, "Http connection was not successful " + exc_03);
            }
        }
        return newsArticleList;
    }

    /**
     * Creates an URL object from an input String
     *
     * @param stringWithHttpQuery string that contains URL query
     * @return URL object
     */
    static private URL createUrl(String stringWithHttpQuery) {
        URL urlWithHttpQuery = null;
        try {
            urlWithHttpQuery = new URL(stringWithHttpQuery);
        } catch (MalformedURLException exc_01) {
            Log.e(LOG_TAG, "The app was not able to create a URL request from the query " + exc_01);
        }
        return urlWithHttpQuery;
    }

    /**
     * Uses URL object to create and execute Http connection, obtains InputStream and calls a helper method to read it
     *
     * @param url URL query
     * @return received JSON response in a String format
     */
    static private String performHttpConnection(URL url) throws IOException {
        String JSONResponse = "";
        if (url == null) {
            return JSONResponse;
        }
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;
        try {
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setReadTimeout(10000 /* milliseconds */);
            httpURLConnection.setConnectTimeout(15000 /* milliseconds */);
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.connect();
            // check whether the connection response code is appropriate (in this case == 200)
            if (httpURLConnection.getResponseCode() == 200) {
                inputStream = httpURLConnection.getInputStream();
                JSONResponse = readInputStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Bad response from the server was received - response code: " + httpURLConnection.getResponseCode());
            }
        } catch (IOException exc_02) {
            Log.e(LOG_TAG, "IOE exception was encountered when trying to connect to http " + exc_02);
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return JSONResponse;
    }


    /**
     * Reads InputStream and parses it into a String
     *
     * @param stream InputStream
     * @return String
     */
    static private String readInputStream(InputStream stream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        if (stream != null) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, Charset.forName("UTF-8")));
            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line);
                line = bufferedReader.readLine();
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Reads JSONString and extracts relevant data from it
     * @param JSONString - result of the previous http query parsed into String format
     * @return List<NewsArticle> a list of NewsArticle objects
     */
    static private List<NewsArticle> extractFromJSONString(String JSONString) {
        List<NewsArticle> newsArticleList = new ArrayList<NewsArticle>();
        try {
            // convert String to a JSONObject
            JSONObject jsonObject = new JSONObject(JSONString);

            // extract: 1) "response" JSONObject and 2) "response" JSONArray
            // check whether "response" JSONObject -> "response" JSONArray are available at all
            // if true - the parsing continues, else - we return an empty ArrayList, as there actually is no data to display
            if(jsonObject.getJSONObject("response").has("results")){
                JSONArray resultsArray = jsonObject.getJSONObject("response").getJSONArray("results");

                // Loop through each item in the array
                // Get NewsArticle JSONObject at position i
                int item;
                for (item = 0; item < resultsArray.length(); item++) {
                    JSONObject newsArticleInfo = resultsArray.getJSONObject(item);

                    // extract "sectionName" for the section NewsArticle belongs to
                    String section;
                    if(newsArticleInfo.has("sectionName")){
                        section = newsArticleInfo.getString("sectionName");
                    } else {
                        section = NewsArticle.NO_SECTION_STRING;
                    }

                    // extract "webPublicationDate" for the publishing time of NewsArticle
                    String timeUnformatted;
                    String timePublished;
                    if(newsArticleInfo.has("webPublicationDate")){
                        timeUnformatted = newsArticleInfo.getString("webPublicationDate"); // format in the JSON response "2017-07-17T23:01:03Z"
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // convert the String into Date
                        try {
                            Date date = format.parse(timeUnformatted);
                            timePublished = (String) android.text.format.DateFormat.format("MMM" + " " + "dd" + ", " + "HH:mm", date); // format the date and cast it to String again
                        } catch (ParseException exc_05) {
                            Log.e(LOG_TAG, "An exception was encountered while trying to parse a date " + exc_05);
                            timePublished = "";
                        }

                    } else {
                        timePublished = "";
                    }

                    // extract "webTitle" for the headline of NewsArticle
                    String headline;
                    if(newsArticleInfo.has("webTitle")){
                        headline = newsArticleInfo.getString("webTitle");
                        // some headline strings have format: "headline | author", we discard the author part
                        if (headline.contains("|")){
                            headline = headline.substring(0, headline.indexOf("|") - 1);
                        }
                    } else {
                        headline = NewsArticle.NO_HEADLINE_STRING;
                    }

                    // extract "webUrl" for the Url link of NewsArticle
                    String articleLink;
                    if(newsArticleInfo.has("webUrl")){
                        articleLink = newsArticleInfo.getString("webUrl");
                    } else {
                        articleLink = "";
                    }

                    // extract "trailText" for the startText of NewsArticle
                    String startText;
                    if(newsArticleInfo.getJSONObject("fields").has("trailText")){
                        startText = newsArticleInfo.getJSONObject("fields").getString("trailText");
                    } else {
                        startText = "";
                    }

                    // extract "byline" for the author of NewsArticle
                    String author;
                    if(newsArticleInfo.getJSONObject("fields").has("byline")){
                        author = AUTHOR_PREFIX + newsArticleInfo.getJSONObject("fields").getString("byline");
                    } else {
                        author = NewsArticle.NO_AUTHOR_STRING;
                    }

                    // extract "thumbnail" for the image link of NewsArticle
                    String imageLink;
                    if(newsArticleInfo.getJSONObject("fields").has("thumbnail")){
                        imageLink = newsArticleInfo.getJSONObject("fields").getString("thumbnail");
                    } else {
                        imageLink = "";
                    }

                    // create NewsArticle object from the extracted data
                    NewsArticle newsArticle = new NewsArticle(headline, author, timePublished, startText, imageLink, articleLink, section);

                    // add the object to List
                    newsArticleList.add(newsArticle);
                }
            }
        } catch (JSONException exc_04) {
            Log.e(LOG_TAG, "An exception was encountered while trying to read JSONString " + exc_04);
        }
        // return result of the method
        return newsArticleList;
    }

    ////
    // Other various Utility methods:
    ////

    /**
    * Method that read the selection parameter set in Preferences and returns a Date in a String format
    * as a final input parameter to URL query
    * @param input a string that is equal to one of the values from a String Array
    * @return Date in a String format
    */
    static String prepare_From_Date_Parameter(String input){
        String from_dateParameter = "";
        Calendar calendar = Calendar.getInstance(); // gets the current time
        switch (input){
            case "today": calendar.add(Calendar.DATE, 0); // no manipulation performed
                break;
            case "today and yesterday": calendar.add(Calendar.DATE, -1); // current time minus 1 day
                break;
            case "last week": calendar.add(Calendar.DATE, -6); // current time minus 6 days
                break;
            case "last two weeks": calendar.add(Calendar.DATE, -13); // current time minus 13 days
                break;
            case "last 30 days": calendar.add(Calendar.DATE, -30); // current time minus 30 days
                break;
            default: calendar.add(Calendar.DATE, 0); // no manipulation performed
        }
        from_dateParameter = (String) android.text.format.DateFormat.format("yyyy-MM-dd", calendar); // format the date and cast it to String again
        return from_dateParameter;
    }
}