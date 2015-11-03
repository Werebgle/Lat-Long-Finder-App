package com.itstime2gohunting.latlongfinder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener, LocationListener {
    // LogCat tag
    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private Location mLastLocation;

    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;

    private LocationRequest mLocationRequest;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters

    public double latitude;
    public double longitude;

    private TextView mLocationTextView;
    private TextView mSunriseTextView;
    private TextView mSunsetTextView;
    private TextView mDayLengthTextView;
    private Button mLocationButton;
    private Button mLocUpdatesButton;
    private Button mMapButton;

    //    private Button mDayButton;
    private SunriseSunset mSunriseSunset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        JodaTimeAndroid.init(this);

        mLocationTextView = (TextView) findViewById(R.id.locationTextView);
        mSunriseTextView = (TextView) findViewById(R.id.sunriseTextView);
        mSunsetTextView = (TextView) findViewById(R.id.sunsetTextView);
        mDayLengthTextView = (TextView) findViewById(R.id.dayLengthTextView);
        mLocationButton = (Button) findViewById(R.id.locationButton);
        mLocUpdatesButton = (Button) findViewById(R.id.locUpdatesButton);
        mMapButton = (Button) findViewById(R.id.mapButton);
//        mDayButton = (Button) findViewById(R.id.dayButton);

        // First we need to check availability of play services
        if (checkPlayServices()) {

            // Building the GoogleApi client
            buildGoogleApiClient();

            createLocationRequest();

        }

        // Show location button click listener
        mLocationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                displayLocation();
                getData();
            }
        });

        // Toggling the periodic location updates
        mLocUpdatesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                togglePeriodicLocationUpdates();
            }
        });

        mMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("latitude", latitude);
                intent.putExtra("longitude", longitude);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkPlayServices();

        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    /**
     * Method to display the location on UI
     */
    private void displayLocation() {

        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();

            mLocationTextView.setText(latitude + ", " + longitude);
        } else {
            mLocationTextView
                    .setText("(Couldn't get the location. Make sure location is enabled on the device)");
        }
    }

    /**
     * Method to toggle periodic location updates
     */
    private void togglePeriodicLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            // Changing the button text
            mLocUpdatesButton
                    .setText(getString(R.string.stop_loc_update_button));

            mRequestingLocationUpdates = true;

            // Starting the location updates
            startLocationUpdates();

            Log.d(TAG, "Periodic location updates started!");

        } else {
            // Changing the button text
            mLocUpdatesButton
                    .setText(getString(R.string.loc_update_button));

            mRequestingLocationUpdates = false;

            // Stopping the location updates
            stopLocationUpdates();

            Log.d(TAG, "Periodic location updates stopped!");
        }
    }

    /**
     * Creating google api client object
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Creating location request object
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    /**
     * Method to verify google play services on the device
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Starting the location updates
     */
    protected void startLocationUpdates() {

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        displayLocation();
        getData();

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        // Assign the new location
        mLastLocation = location;

        Toast.makeText(getApplicationContext(), "Location changed!",
                Toast.LENGTH_SHORT).show();

        // Displaying the new location on UI
        displayLocation();
    }

    private void getData() {
        String dataUrl = "http://api.sunrise-sunset.org/json?lat="
                + latitude + "&lng=" + longitude + "&formatted=0";

        if (networkIsAvailable()) {
            OkHttpClient client = new OkHttpClient();
            com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
                    .url(dataUrl)
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(com.squareup.okhttp.Request request, IOException e) {

                }

                @Override
                public void onResponse(com.squareup.okhttp.Response response) throws IOException {
                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mSunriseSunset = getCurrentDetails(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplaySunrise();
                                    updateDisplaySunset();
                                    updateDisplayDayLength();
                                }
                            });
                        } else {
                            alertUserAboutError();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception Caught: ", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "Exception Caught: ", e);
                    }
                }
            });
        } else {
            Toast.makeText(this, R.string.network_not_available_message, Toast.LENGTH_LONG).show();
        }
    }

    private void updateDisplaySunrise() {
        StringBuilder stringBuilderSunrise = new StringBuilder();
        stringBuilderSunrise.append(mSunriseSunset.getSunrise());
        stringBuilderSunrise.delete(17, 24).append(".0Z");
        String result = stringBuilderSunrise.toString();
        DateTimeFormatter fmtSunrise = ISODateTimeFormat.dateTime();
        DateTime dt = fmtSunrise.parseDateTime(result);
        StringBuilder finalTimeSunrise = new StringBuilder();
        finalTimeSunrise.append(dt);
        finalTimeSunrise.delete(0, 11).delete(5, 19);
        String finalResultSunrise = finalTimeSunrise.toString();
        mSunriseTextView.setText(finalResultSunrise);
    }

    private void updateDisplaySunset() {
        StringBuilder stringBuilderSunset = new StringBuilder();
        stringBuilderSunset.append(mSunriseSunset.getSunset());
        stringBuilderSunset.delete(17, 24).append(".0Z");
        String result = stringBuilderSunset.toString();
        DateTimeFormatter fmtSunset = ISODateTimeFormat.dateTime();
        DateTime dt = fmtSunset.parseDateTime(result);
        StringBuilder finalTimeSunset = new StringBuilder();
        finalTimeSunset.append(dt);
        finalTimeSunset.delete(0, 11).delete(5, 19);
        String finalResultSunset = finalTimeSunset.toString();
        mSunsetTextView.setText(finalResultSunset);
    }

    private void updateDisplayDayLength() {
        double dayLength = mSunriseSunset.getDayLength();
        dayLength = dayLength / 3600;
        dayLength = (double) Math.round(dayLength * 100) / 100;
        String finalResultDayLength = Double.toString(dayLength);
        Log.i(TAG, finalResultDayLength);
        mDayLengthTextView.setText(finalResultDayLength.toString() + " hours");
    }

    private SunriseSunset getCurrentDetails(String jsonData) throws JSONException {
        JSONObject root = new JSONObject(jsonData);
        JSONObject results = root.getJSONObject("results");

        SunriseSunset sunriseSunset = new SunriseSunset();
        sunriseSunset.setSunrise(results.getString("sunrise"));
        sunriseSunset.setSunset(results.getString("sunset"));
        sunriseSunset.setDayLength(results.getDouble("day_length"));

        return sunriseSunset;
    }

    private boolean networkIsAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailble = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailble = true;
        }
        return isAvailble;
    }

    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(), "error_dialog");
    }


}
