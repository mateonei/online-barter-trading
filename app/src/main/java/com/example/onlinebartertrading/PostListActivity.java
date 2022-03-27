package com.example.onlinebartertrading;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SearchView;

import com.google.android.material.chip.Chip;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

/**
 * This class represents the details activity
 * which appears when a user enters a product post
 **/
public class PostListActivity extends AppCompatActivity implements View.OnClickListener{
    ListView listGoods;
    /*
    These lists hold the posts that are needed to be displayed. These are passed to PostListAdapter.java to add the the ArrayAdaptor.
     */
    ArrayList<String> name = new ArrayList<>();
    ArrayList<String> detail = new ArrayList<>();
    ArrayList<String> value = new ArrayList<>();
    ArrayList<String> category = new ArrayList<>();
    ArrayList<String> email = new ArrayList<>();
    ArrayList<String> distance = new ArrayList<>();
    Button showButton;
    PostListAdapter postListAdapter;
    Chip chip;
    LatLng position;
    /*
    This holds the posts after applying filter and keyword search.
     */
    ArrayList<DataSnapshot> values;
    DatabaseReference reference;
    String searchKeyword = "";
    User user;

    // Necessary for location provider
    private LocationProvider locationProvider;
    int numPosts = 0;

    /**
     * View setup and value listeners
     **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        user = (User) getIntent().getSerializableExtra("user");
        user.setLocationProvider(new LocationProvider(this));

        searchKeyword = getSearchQuery(getIntent());

        //listview layout
        setContentView(R.layout.activity_listmain);
        listGoods = findViewById(R.id.listView);
        listGoods.addFooterView(getLayoutInflater().inflate(R.layout.footer_view, null));
        listGoods.addHeaderView(getLayoutInflater().inflate(R.layout.header_view, null));

        showButton = findViewById(R.id.btn);
        showButton.setOnClickListener(this);

        //Swipe down to refresh lets the user automatically show any new posts that are made by other users.
        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(
                () -> {
                    finish();
                    overridePendingTransition( 0, 0);
                    Intent intent = getIntent();
                    intent.putExtra("user", user);
                    startActivity(intent);
                    overridePendingTransition( 0, 0);
                    swipeRefreshLayout.setRefreshing(false);
                }
        );

        position = user.getLocation();

        reference = FirebaseDatabase.getInstance().getReference().child("posts");

        Query query = reference.orderByKey();

        query.addListenerForSingleValueEvent(new ValueEventListener() {

            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onDataChange(@NonNull DataSnapshot datasnapshot) {
                //get data from firebase
                Iterator<DataSnapshot> snapshots = datasnapshot.getChildren().iterator();
                ArrayList<DataSnapshot> list = new ArrayList<>();
                //Get all values from iterator
                snapshots.forEachRemaining(list::add);
                values = applyFilters(list);
                extractPosts();
                if(user.getPreferences() == null) {
                    chip.setVisibility(View.GONE);
                }

                position = user.getLocation();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // TODO: handle this exception
            }
        });

        chip = findViewById(R.id.filterChip);
        chip.setOnCloseIconClickListener(view -> {
            user.setPreferences(null);
            Intent intent = getIntent();
            intent.putExtra(SearchManager.QUERY, "");
            intent.putExtra("user", user);
            intent.putExtra("query", "");
            startActivity(intent);
        });
    }

    /**
     * Specify the settings of the options menu
     * @param menu - the object for the options menu.
     * @return a boolean representing successful creation of the options menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        MenuItem search = menu.findItem(R.id.search);

        MenuItem filter = menu.findItem(R.id.setting);
        //Behaviour for when filter button is clicked. The user will be taken to the preferences activity.
        filter.setOnMenuItemClickListener(item -> {
         Intent intent = new Intent(getBaseContext(), PreferenceActivity.class);
         intent.putExtra("user", user);
         startActivity(intent);
         return false;
        });

        SearchView searchView =
                (SearchView) search.getActionView();
        searchView.setIconifiedByDefault(false);

        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Intent intent = new Intent(getBaseContext(), PostListActivity.class);
                intent.putExtra("query", query);
                intent.putExtra("user", user);
                startActivity(intent);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return true;
    }

    /**
     * Retrieve search keyword from intent
     * @param intent
     * @return the search keyword entered by the user.
     */
    private String getSearchQuery(Intent intent) {
        if (intent.getStringExtra("query") == null) return "";
        return intent.getStringExtra("query");
    }

    /**
     * Apply filters to modify the values ArrayList that is used to show results in ShowDetails activity.
     * @param data containing all the results of the query
     * @return ArrayList containing only the posts that match the filters
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private ArrayList<DataSnapshot> applyFilters(ArrayList<DataSnapshot> data) {
        ArrayList<String> categories = new ArrayList<>();
        int valueUpperLimit =  1000000;
        int valueLowerLimit = 0;
        int distance = 1000;

        if(user.getPreferences() != null) {
            categories = user.getPreferences().getCategories();
            //The upper limit of the price
            valueUpperLimit = user.getPreferences().getMaxValue();
            valueLowerLimit = user.getPreferences().getMinValue();
            distance = user.getPreferences().getDistance() * 1000;

            //Upper limit of distance. This should be in meters
        }
        //This is the current location, this would be taken from the intent from the activity that detects location.
        LatLng current_position = position;

        ArrayList<DataSnapshot> list = new ArrayList<>();
        for(DataSnapshot value : data) {
            double post_lat = Double.parseDouble(value.child("latitude").getValue().toString());
            double post_long = Double.parseDouble(value.child("longitude").getValue().toString());
            LatLng post_position = new LatLng(post_lat, post_long);

            double distance_between = SphericalUtil.computeDistanceBetween(current_position, post_position);
//            System.out.println(distance_between);
            //Refactor
            if ((value.child("title").getValue().toString().contains(searchKeyword) || value.child("desc").getValue().toString().contains(searchKeyword))) {
                if(user.getPreferences() == null || ((categories.isEmpty() || categories.contains(value.child("category").getValue().toString()))  && distance_between <= distance && valueLowerLimit <= Integer.parseInt(value.child("value").getValue().toString()) && Integer.parseInt(value.child("value").getValue().toString()) <= valueUpperLimit)) {
                    list.add(value);
                }
            }
        }
        return list;
    }

    /**
     * Method that displays search results in a multiple of 4 depending on the number of clicks of Show More button.
     * Modifies numPosts which keeps track of the total posts currently on page.
     */
    private void extractPosts() {
        int batchCount = 0;
        //Start before the postings already on screen.
        for (int i = values.size() - numPosts -1; i >= 0; i--) {
            DataSnapshot snapshot = values.get(i);
            String tit, de, val, cat, posterEmail;
            LatLng itemPosition;

            try {
                tit = Objects.requireNonNull(snapshot.child("title").getValue()).toString();
                de = Objects.requireNonNull(snapshot.child("desc").getValue()).toString();
                val = Objects.requireNonNull(snapshot.child("value").getValue()).toString();
                cat = Objects.requireNonNull(snapshot.child("category").getValue()).toString();
                posterEmail = Objects.requireNonNull(snapshot.child("posterEmail").getValue()).toString();
                itemPosition = new LatLng(Double.parseDouble(Objects.requireNonNull(snapshot.child("latitude").getValue()).toString()), Double.parseDouble(snapshot.child("longitude").getValue().toString()));
            } catch (NullPointerException e) {
                continue;
            }

            LatLng currentPosition = position;
            int distance = (int) (SphericalUtil.computeDistanceBetween(itemPosition, currentPosition));
            String distVal = "<"+(distance/1000+10)/10 *10;

            if(!tit.isEmpty() && !de.isEmpty() && !val.isEmpty()) {
                batchCount++;
                name.add(tit);
                detail.add(de);
                value.add(val);
                category.add(cat);
                email.add(posterEmail);
                this.distance.add(distVal);
            }
            if(batchCount == 5) break;
        }

        numPosts += batchCount;
        //Disable search button if we did not get 4 posts or we get all of the posts.
        if(batchCount < 5 || numPosts == values.size()) {
            showButton.setVisibility(View.GONE);
        }

        //Send to adapter and make data in each layout
        if(postListAdapter == null) {
            postListAdapter = new PostListAdapter(PostListActivity.this, name, detail, value, category, distance, email);
        }
        else {
            postListAdapter.notifyDataSetChanged();
        }

        listGoods.setAdapter(postListAdapter);
    }

    /**
     * Behavior when the show more button is clicked.
     * @param view
     */
    @Override
    public void onClick(View view) {
        extractPosts();
    }

}