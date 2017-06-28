package com.google.hvz.hvz_app;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String APP_SERVICE_URL = "http://humansvszombies-24348.appspot.com/api";
    private static final MediaType APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8");;
    private FusedLocationProviderClient mFusedLocationClient;

    public MainActivity() {
        super();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init Location service
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            checkPermission();
        }

        // Register Button event
        final Button button = (Button)findViewById(R.id.location_button);
        button.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                  sendCurrentDeviceLocation();
              }
          }
        );
    }

    private void sendCurrentDeviceLocation() {

        try {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                double longitude, latitude;
                                longitude = location.getLongitude();
                                latitude = location.getLatitude();

                                Log.i("sendCurrentLocation",
                                        "longitude: " + longitude + "  latitude: " + latitude);

                                sendUserDeviceLocation(longitude, latitude);
                            }
                        }
                    });
        } catch (SecurityException ex) {
            Log.e("sendCurrentLocation", "Permission for location was not granted by user!");
        }
    }

    private void sendUserDeviceLocation(double longitude, double latitude) {
        UpdateLocationThread updateLocationThread = new UpdateLocationThread(longitude, latitude);
        updateLocationThread.start();
    }

    private class UpdateLocationThread extends Thread {
        private OkHttpClient client = new OkHttpClient();
        private static final String ThreadTAG = "UploadLocation";

        public UpdateLocationThread(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }

        private double longitude, latitude;

        @Override
        public void run() {
            OkHttpClient httpClient = new OkHttpClient();

            try {
                String json = String.format("{" +
                        "  \"longitude\": %s," +
                        "  \"latitude\": %s," +
                        "  \"gameId\": \"%s\"," +
                        "  \"playerId\": \"%s\"," +
                        "  \"requestingUserIdJwt\": \"%s\"," +
                        "  \"requestingUserId\": \"%s\"," +
                        "  \"requestingPlayerId\": \"%s\"" +
                        "}", longitude, latitude,
                        getCurrentGameId(), getCurrentPlayerId(), getRequestingUserIdJwt(),
                        getRequestingUserId(), getCurrentPlayerId());

                RequestBody body = RequestBody.create(APPLICATION_JSON, json);
                Request request = new Request.Builder()
                        .url(String.format("%s/updatePlayerMarkers", APP_SERVICE_URL))
                        .post(body)
                        .build();
                Response response = httpClient.newCall(request).execute();
                Log.i("sendLocationReq", json);
                if (response.code() != 200) {
                    Log.e("sendLocationReq", "Invalid response status: " + response.code());
                }
                else {
                    Log.i("sendLocationReq", "Success!");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Log.e("sendCurrentLocation", ex.toString());
            }
        }
    }

    private String getCurrentGameId() {
        return "game-test-3274632959878935-1";
    }

    private String getCurrentPlayerId() {
        return "publicPlayer-test-3274632959878935-1";
    }

    private String getRequestingUserId() {
        return "user-test-3274632959878935-1";
    }

    private String getRequestingUserIdJwt() {
        return "1337";
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    public void checkPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ){//Can add more as per requirement

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},
                    123);
        }
    }
}
