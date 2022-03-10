package com.example.onlinebartertrading;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


/**
 * This class provides a way for users to select their preferences
 */
public class PreferenceActivity extends AppCompatActivity implements View.OnClickListener {
    //km
    public static final int MAX_DISTANCE = 1000;
    public static String areaText;

    private DatabaseReference userRef;

    /**
     * Sets the new view up
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);

        setPreferences();

        Button enterButton = findViewById(R.id.preferenceButton);
        enterButton.setOnClickListener(this);

    }

    protected void initializeUserDBRef() {
        Intent intent = getIntent();
        String email = intent.getStringExtra("email");

        DatabaseReference dbRef = FirebaseDatabase
                .getInstance(FirebaseConstants.FIREBASE_URL)
                .getReference();

        String uuid = UUID.nameUUIDFromBytes(email.getBytes()).toString();
        userRef = dbRef.child(FirebaseConstants.USERS_COLLECTION).child(uuid);
    }

    // Checks if user has saved preferences and sets them if true
    protected void setPreferences() {

        initializeUserDBRef();

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String message;
                if (snapshot.hasChild("preferences")) {
                    PreferenceClass preferences = snapshot
                            .child("preferences")
                            .getValue(PreferenceClass.class);

                    setTags(preferences.getTags());
                    setMinValue(preferences.getMinValue());
                    setMaxValue(preferences.getMaxValue());
                    setDistance(preferences.getDistance());

                    message = "Preferences loaded";
                }
                else {
                    message = "No preferences found for user";
                }
                Toast.makeText(PreferenceActivity.this, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                System.out.println("DATABASE ERROR: " + error.getMessage());
            }
        });
    }

    protected void setTags(List<Integer> tags) {
        return;
    }

    /**
     * sets minimum value the user wants to filter by
     * @param minValue
     */
    protected void setMinValue(int minValue) {
        EditText minTextBox = findViewById(R.id.minValue);
        minTextBox.setText(String.format(Locale.ENGLISH, "%d", minValue));
    }

    /**
     * sets maximum value the user wants to filter by
     * @param maxValue
     */
    protected void setMaxValue(int maxValue) {
        EditText maxTextBox = findViewById(R.id.maxValue);
        maxTextBox.setText(String.format(Locale.ENGLISH, "%d", maxValue));
    }

    protected void setDistance(int distance) {
        return;
    }

    /**
     * Sets an error message if the user enters incorrect details
     * @param message
     */
    protected void setStatusMessage(String message){
        TextView statusLabel = findViewById(R.id.prefStatusLabel);
        statusLabel.setText(message.trim());
    }

    /**
     * returns the preferences selected
     * @return
     */
    protected List<Integer> getPreferences(){
        ChipGroup pref = findViewById(R.id.allChips);
        List<Integer> checkedChips = pref.getCheckedChipIds();
        return checkedChips;
    }

    /**
     * Gets the minimum value eneterd by the user
     * @return
     */
    protected int getMinValue(){
        EditText minTextBox = findViewById(R.id.minValue);
        int minValue;
        try {
            minValue = Integer.parseInt(minTextBox.getText().toString().trim());
        }
        catch (Exception e){
            //invalid integer so return error value
            minValue = -1;
        }
        return minValue;
    }

    /**
     * Gets the maximum value entered by the user
     * @return
     */
    protected int getMaxValue(){
        EditText minTextBox = findViewById(R.id.maxValue);
        int maxValue;
        try {
            maxValue = Integer.parseInt(minTextBox.getText().toString().trim());
        }
        catch (Exception e){
            //invalid integer so return error value
            maxValue = -2;
        }
        return maxValue;
    }

    /**
     * Gets the distance the user selected from the chips
     * @return
     */
    protected int getDistance(){
        ChipGroup pref = findViewById(R.id.distanceChips);
        int checkedChip = pref.getCheckedChipId();
        int maxDistance = MAX_DISTANCE;
        if (checkedChip == R.id.tenDist){
            maxDistance = 10;
        }
        else if (checkedChip == R.id.twentyFiveDist){
            maxDistance = 25;
        }
        else if (checkedChip == R.id.fiftyDist){
            maxDistance = 50;
        }
        else if (checkedChip == R.id.hundredDist){
            maxDistance = 100;
        }
        else if (checkedChip == R.id.twoHundredDist){
            maxDistance = 200;
        }
        return maxDistance;
    }

    /**
     * checked if minimum value is not too small or too large
     * @param value
     * @return
     */
    protected boolean isValidMinValue(int value){
        if (value>=0 && value<MakePostActivity.maxValue){
            return true;
        }
        return false;
    }

    /**
     * Checks if maximum is not too small or too large
     * @param value
     * @return
     */
    protected boolean isValidMaxValue(int value){
        if (value>=1 && value<MakePostActivity.maxValue){
            return true;
        }
        return false;
    }

    /**
     * Checks the minimum is less than the maximum
     * @param min
     * @param max
     * @return
     */
    protected boolean minIsLessThanMax(int min, int max){
        if (min<= max){
            return true;
        }
        return false;
    }

    /**
     * When button is pressed, preferences are saved if they are valid or an error message is
     * displayed
     * @param view
     */
    @Override
    public void onClick(View view) {
        List<Integer> selectedTags = getPreferences();
        int minValue = getMinValue();
        int maxValue = getMaxValue();
        int maxDistance = getDistance();
        String errorMessage = "";

        if (!isValidMinValue(minValue)){
            errorMessage =  getResources().getString(R.string.EMPTY_FIELD);
        }

        else if (!isValidMaxValue(maxValue)){
            errorMessage = getResources().getString(R.string.EMPTY_FIELD);
        }

        else if (!minIsLessThanMax(minValue,maxValue)){
            errorMessage = getResources().getString(R.string.MIN_LESS_MAX);
        }

        setStatusMessage(errorMessage);

        if (errorMessage.equals("")){
            // Saves preferences to DB for specific user
            Map<String, Object> preferences = new HashMap<>();
            PreferenceClass userPref =
                    new PreferenceClass(selectedTags, minValue, maxValue, maxDistance);
            preferences.put("preferences", userPref);

            userRef.updateChildren(preferences);

            //switch to new activity

        }
    }
}