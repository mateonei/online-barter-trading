package com.example.onlinebartertrading;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PreferenceClass implements Serializable {

    private ArrayList<Integer> tags;
    private int minValue;
    private int maxValue;
    private int distance;
    private String localArea;

    public PreferenceClass() {
        //Needed
    }

    public PreferenceClass(List<Integer> tags, int minValue, int maxValue, int distance){
        this.tags = new ArrayList<>(tags);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.distance = distance;
    }

    public List<Integer> getTags() {
        return tags;
    }

    public int getMinValue() {
        return minValue;
    }


    public int getMaxValue() {
        return maxValue;
    }

    public int getDistance() {
        return distance;
    }

}
