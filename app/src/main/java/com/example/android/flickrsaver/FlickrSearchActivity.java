package com.example.android.flickrsaver;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class FlickrSearchActivity extends AppCompatActivity {

    // Constants
    private static final String
            TAG = FlickrSearchActivity.class.getSimpleName();
    private static final String BASE_FLICKR_URL =
            "https://api.flickr.com/";

    static final String URL_KEY = "imgUrl";

    // Constant used to prevent the response being wrapped in a JSON callback
    private static final int JSON_CALLBACK = 1;

    // Urls to Flickr search results
    private ArrayList<String> mImageUrls;

    // View to show if no search results are available
    private TextView mEmptyTextView;

    private FlickrImageAdapter mAdapter;

    // Track page number and search query
    private int mCurrentPage;
    private String mQuery;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flickr_search);
        initializeDataAndViews();

        // TODO: implement persistence of some kind (SharedPrefs, SQLLite)
    }

    private void initializeDataAndViews() {
        mEmptyTextView = findViewById(R.id.emptyText);
        mImageUrls = new ArrayList<>();
        mAdapter = new FlickrImageAdapter(mImageUrls);
        setUpRecyclerView();
    }

    private void setUpRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        final int numColumns = getResources().getInteger(R.integer.num_columns);
        GridLayoutManager layoutManager = new GridLayoutManager(this,
               numColumns);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);
    }

    private void handleIntent(Intent intent) {
        // Handle the search, starting with page 1
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            mQuery = intent.getStringExtra(SearchManager.QUERY);
            mCurrentPage = 1;
            performSearch(mQuery, mCurrentPage);
        }
    }

    /**
     * Launch mode is singleTop, to preserve data when navigating back
     * @param intent The incoming search intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    /**
     * Executes an image search on the Flickr API, and adds the resulting url
     * to the data list
     *
     * @param query The search terms
     * @param page  The page of results to return
     */
    private void performSearch(String query, int page) {
        // Build the endpoint using Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_FLICKR_URL).build();
        FlickrEndpoint flickrEndpoint = retrofit.create(FlickrEndpoint.class);

        // Set up the search call
        Call<ResponseBody> call = flickrEndpoint.getSearchResults(
                getString(R.string.photo_search_method),
                getString(R.string.api_key),
                query, getString(R.string.json_format),
                JSON_CALLBACK, page);

        setLoadingUI();

        // Obtain the response in the callback
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call,
                    Response<ResponseBody> response) {

                // Try to parse the JSON results and obtain the image urls
                // using the GSON library
                try {
                    // Remove the loading UI
                    mImageUrls.remove(getString(R.string.loading));
                    mAdapter.notifyItemRemoved
                            (mImageUrls.indexOf(getString(R.string.loading)));

                    // Remove the load more button (will be added at the end
                    // of the data)
                    mImageUrls.remove(getString(R.string.load_more));
                    mAdapter.notifyItemRemoved
                            (mImageUrls.indexOf(getString(R.string.load_more)));

                    // Get the image urls
                    JSONObject jsonResponse = new
                            JSONObject(response.body().string());
                    JSONArray photos = jsonResponse.getJSONObject("photos")
                            .getJSONArray("photo");
                    Gson gson = new GsonBuilder().create();
                    for (int i = 0; i < photos.length(); i++) {
                        JSONObject photo = photos.getJSONObject(i);
                        FlickrSearchResponse searchResponse =
                                gson.fromJson(photo.toString(),
                                        FlickrSearchResponse.class);

                        String url = getString(R.string.image_url,
                                searchResponse.getFarm(),
                                searchResponse.getServer(),
                                searchResponse.getId(),
                                searchResponse.getSecret());

                        // Add the URLs to the data set an update the UI
                        mImageUrls.add(url);
                        mAdapter.notifyItemInserted(mImageUrls.size());
                    }


                    // Add an extra line, used to setup the load more button
                    // Usually I would use a wrapper object with a ViewType
                    mImageUrls.add(getString(R.string.load_more));
                    mAdapter.notifyDataSetChanged();

                    // TODO: catch when the API runs out of results
                } catch (JSONException | IOException e) {
                    /* Catch and display errors in parsing.
                    A more robust error handling would be preferred,
                    including the case when there are no more results*/
                    mEmptyTextView.setText(R.string.json_error);
                    Log.e(TAG, "onResponse: ", e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Show error message if there are connection problems
                mEmptyTextView.setText(t.getMessage());
                Log.e(TAG, "onFailure: ", t);
            }
        });
    }

    private void setLoadingUI() {
        mImageUrls.add(getString(R.string.loading));
        mEmptyTextView.setText("");
        mAdapter.notifyItemInserted(mImageUrls.size());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_options_menu, menu);

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();

        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    // The Retrofit endpoint configuration including all parameters
    private interface FlickrEndpoint {
        @GET("services/rest/")
        Call<ResponseBody> getSearchResults(@Query("method") String method,
                @Query("api_key") String apiKey, @Query("text") String query,
                @Query("format") String format,
                @Query("nojsoncallback") int callback, @Query("page") int page);

    }

    // Custom adapter for the list of images
    private class FlickrImageAdapter extends RecyclerView.Adapter
            <FlickrImageAdapter.ViewHolder> {

        // The Data set if image URLs.
        private ArrayList<String> mImageUrls;


        FlickrImageAdapter(ArrayList<String> imageUrls) {
            this.mImageUrls = imageUrls;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent,
                int viewType) {
            // Create ViewHolders, of the image type by default
            View view = LayoutInflater.from(FlickrSearchActivity.this)
                    .inflate(R.layout.image_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            if (mImageUrls.get(position)
                    .equals(getString(R.string.loading))) {
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.imageView.setImageDrawable(null);
            } else if (mImageUrls.get(position).equals(getString(R.string.load_more))) {
                holder.imageView.setImageDrawable(ContextCompat
                        .getDrawable(FlickrSearchActivity.this,
                        R.drawable.ic_add));
                holder.imageView.setBackgroundColor(
                        ContextCompat.getColor(FlickrSearchActivity.this,
                                R.color.addMoreBg));
                holder.progressBar.setVisibility(View.GONE);
            } else {
                holder.progressBar.setVisibility(View.VISIBLE);
                RequestOptions options = new RequestOptions().centerCrop();
                Glide.with(FlickrSearchActivity.this)
                        .load(mImageUrls.get(position))
                        // Sets a listener to show a loading progressbar
                        // while the image is retrieved
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(
                                    @Nullable GlideException e,
                                    Object model,
                                    Target<Drawable> target,
                                    boolean isFirstResource) {
                                Log.e(TAG, "onLoadFailed: ", e);
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(
                                    Drawable resource,
                                    Object model,
                                    Target<Drawable> target,
                                    DataSource dataSource,
                                    boolean isFirstResource) {
                                holder.progressBar.setVisibility(
                                        View.GONE);
                                holder.imageView.setImageDrawable(
                                        resource);
                                // Best practice to create your own
                                // caching logic, using the default for this
                                // example.
                                return false;
                            }
                        })
                        .apply(options)
                        .into(holder.imageView);
            }
        }

        @Override
        public int getItemCount() {
            return mImageUrls.size();
        }

        // ViewHolder classes handle their own clicks.
        class ViewHolder extends RecyclerView.ViewHolder {

            ImageView imageView;
            ProgressBar progressBar;

            ViewHolder(View itemView) {
                super(itemView);
                progressBar = itemView.findViewById(R.id.progressBar);
                imageView = itemView.findViewById(R.id.imageView);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Clicking on the image starts the detail flow,
                        // or loads more images if it's the placeholder
                        //image.
                        if (mImageUrls.get(getAdapterPosition())
                                .equals(getString(R.string.load_more))) {
                            mCurrentPage++;
                            performSearch(mQuery, mCurrentPage);
                        } else {
                            Intent detailIntent =
                                    new Intent(FlickrSearchActivity.this,
                                            FlickrDetailActivity.class);
                            detailIntent.putExtra(URL_KEY, mImageUrls.get(
                                    getAdapterPosition()));
                            startActivity(detailIntent);
                        }
                    }
                });
            }
        }
    }
}
