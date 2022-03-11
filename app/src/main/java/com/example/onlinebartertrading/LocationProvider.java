package com.example.onlinebartertrading;

import static androidx.core.app.ActivityCompat.requestPermissions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;

/**
 * Represents a user location provider
 */
public class LocationProvider {
    private final Context context;
    private final LocationManager locationManager;

    /**
     * @param activity TODO: figure out if this is all we need
     */
    public LocationProvider(Activity activity) {
        this.context = activity.getApplicationContext();
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        getLocationPermissions();
    }

    /**
     * Requests a single location update.
     * @return Latitude in Double[0], Longitude in Double[1], null if unavailable
     */
    @SuppressLint("MissingPermission") // we do it ourselves
    public Double[] getLocationUpdate() {
        Double[] results = new Double[2];
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Criteria locationMode = new Criteria();

        // Establish location accuracy mode
        if (getAccuracyLevel() == 2 && gpsEnabled) {
            locationMode.setAccuracy(Criteria.ACCURACY_FINE);
        } else if (getAccuracyLevel() > 0 && networkEnabled) {
            locationMode.setAccuracy(Criteria.ACCURACY_COARSE);
        } else return null;

        // Get location update and store latitude and longitude in results
        locationManager.requestSingleUpdate(locationMode, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                results[0] = location.getLatitude();
                results[1] = location.getLongitude();
            }

            @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override public void onProviderEnabled(String provider) { }
            @Override public void onProviderDisabled(String provider) { }
        }, null);

        return results;
    }

    /**
     * Requests runtime location permissions,
     * only if none have been granted so far.
     * @return true if at least coarse location permission has been granted
     */
    private boolean getLocationPermissions() {
        if (getAccuracyLevel() > 0) return true;

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION};

        requestPermissions((Activity) context, permissions, 100);
        return getAccuracyLevel() >= 1;
    }

    /**
     * Requests background location permissions if not granted
     * @return true if granted
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private boolean getBackgroundPermissions() {
        int bgLocation = context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        if (bgLocation == PackageManager.PERMISSION_GRANTED) return true;

        String[] permissions = {Manifest.permission.ACCESS_BACKGROUND_LOCATION};

        requestPermissions((Activity) context, permissions,100);

        return context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Checks the maximum allowed location precision
     * @return 2 for FINE, 1 for COARSE, 0 for NONE
     */
    public int getAccuracyLevel() {
        int fineLocation = context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        int coarseLocation = context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);

        if (fineLocation == PackageManager.PERMISSION_GRANTED) return 2;
        if (coarseLocation == PackageManager.PERMISSION_GRANTED) return 1;
        return 0;
    }
}