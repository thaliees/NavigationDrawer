package com.thaliees.navigationdrawer;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {
    private static final int REQUEST_CHECK_SETTINGS = 1000;
    private static final int REQUEST_PERMISSION_CODE = 1001;
    private static final int GEOFENCE_RADIUS_IN_METERS = 100;
    private static final int MILLISECONDS = 1000;
    private static final int SECONDS = 60;
    private static final long GEOFENCE_DURATION_IN_MILLISECONDS = 60 * SECONDS * MILLISECONDS; // 1 Hour

    private GoogleMap mMap;
    private FusedLocationProviderClient fProviderClient;
    private LocationRequest locRequest;
    private LocationCallback locCallback;
    private GeofencingClient geoClient;
    private ArrayList<Geofence> geoList;
    private PendingIntent geoPendingIntent;
    private Marker locationMarker;

    private TextView textLocation;
    private boolean isAddGeofencesMarker;

    public static Intent makeNotificationIntent(Context geofenceService) {
        return new Intent(geofenceService, MapsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        textLocation = findViewById(R.id.location);
        isAddGeofencesMarker = false;

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Create an instance of the Fused Location Provider Client
        fProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Implement the LocationCallBack interface
        locCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Toast.makeText(MapsActivity.this, "Updating...", Toast.LENGTH_SHORT).show();
                for (Location location : locationResult.getLocations()) {
                    setTextLocation(location);
                }
            }
        };

        geoPendingIntent = null;
        // Create the geofences list
        geoList = new ArrayList<>();
        getGeofences();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Determine the scale of the map
        mMap.setMinZoomPreference(1);
        mMap.setMaxZoomPreference(20);

        getGeofences();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                settingLocationRequest();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!checkPermissions()) requestPermissions();
        else settingLocationRequest();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        fProviderClient.removeLocationUpdates(locCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fProviderClient.removeLocationUpdates(locCallback);
        removeGeofences();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == REQUEST_CHECK_SETTINGS && requestCode == RESULT_OK)
            getLastKnownLocation();
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, REQUEST_PERMISSION_CODE);
    }

    private List<LatLng> createGeofences(){
        List<LatLng> points = new ArrayList<>();
        points.add(new LatLng(16.773577, -93.112314));
        points.add(new LatLng(16.771954, -93.1122589));
        points.add(new LatLng(16.7664486, -93.1160911));
        points.add(new LatLng(16.784028, -93.111808));
        points.add(new LatLng(16.7848428, -93.1139568));
        points.add(new LatLng(16.7551746, -93.1235983));

        return points;
    }

    private void getGeofences() {
        List<LatLng> points = createGeofences();
        // Create a geofence or a list of geofences. Use coordinates near your location.
        // Note: On single-user devices, there is a limit of 100 geofences per app. For multi-user devices, the limit is 100 geofences per app per device user.
        int count = 1;
        geoList.clear();
        for (LatLng region : points) {
            geoList.add(new Geofence.Builder().
                    setRequestId("Point " + count).
                    setCircularRegion(region.latitude, region.longitude, GEOFENCE_RADIUS_IN_METERS).
                    setExpirationDuration(GEOFENCE_DURATION_IN_MILLISECONDS).
                    setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT).
                    setLoiteringDelay(1).
                    build());
            count++;
        }

        // Show on the map, the geofences
        setGeofencesMarker(points);
    }

    private void setGeofencesMarker(List<LatLng> points) {
        if (mMap != null && !isAddGeofencesMarker) {
            isAddGeofencesMarker = true;

            int count = 1;
            for (LatLng latLng : points) {
                Marker marker = mMap.addMarker(new MarkerOptions().position(latLng).title("Point " + count));
                drawRadius(marker);
                count++;
            }
            // Add our list of geofences to a geofencing client
            geoClient = LocationServices.getGeofencingClient(this);
            startGeofence();
        }
    }

    private void drawRadius(Marker marker) {
        mMap.addCircle(new CircleOptions().center(marker.getPosition()).strokeColor(Color.BLUE).fillColor(Color.TRANSPARENT).radius(GEOFENCE_RADIUS_IN_METERS));
    }

    private void startGeofence() { if (isAddGeofencesMarker) addGeofences(); }

    private void addGeofences() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        geoClient.addGeofences(getGeofenceRequest(), createGeofencePendingIntent())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MapsActivity.this, "Add Geofences: Success", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MapsActivity.this, "Add Geofences: Failure", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeGeofences(){
        geoClient.removeGeofences(createGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MapsActivity.this, "Remove Geofences: Success", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MapsActivity.this, "Remove Geofences: Failure", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofenceRequest() {
        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        // Add the geofences to be monitored by geofencing service.
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_DWELL)
                .addGeofences(geoList)
                .build();
    }

    private PendingIntent createGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geoPendingIntent != null) return geoPendingIntent;

        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling addGeofences()
        geoPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geoPendingIntent;
    }

    private void settingLocationRequest() {
        locRequest = LocationRequest.create();
        locRequest.setInterval(10 * MILLISECONDS);          // 10 Seconds
        locRequest.setFastestInterval(5 * MILLISECONDS);    // 5 Seconds
        locRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize location requests here.
                getLastKnownLocation();
            }
        });
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                        resolvableApiException.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) { }
                }
            }
        });
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // Get the last known location of a user's device.
        fProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) setTextLocation(location);
                startLocationUpdates();
            }
        });
    }

    private void setTextLocation(Location location) {
        String extra = "Lat: " + location.getLatitude() + " Lng: " + location.getLongitude();
        textLocation.setText(getString(R.string.location, extra));
        setLocation(location);
    }

    private void setLocation(Location location) {
        if (mMap != null) {
            if (locationMarker != null) locationMarker.remove();

            LatLng current = new LatLng(location.getLatitude(), location.getLongitude());

            float zoom = 19f;

            locationMarker = mMap.addMarker(new MarkerOptions().position(current).title("You are here!"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, zoom));
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fProviderClient.requestLocationUpdates(locRequest, locCallback, null);
    }

    @Override
    public void onLocationChanged(Location location) {
        Toast.makeText(this, "Location changed", Toast.LENGTH_SHORT).show();
        if (location != null) setTextLocation(location);
    }
}
