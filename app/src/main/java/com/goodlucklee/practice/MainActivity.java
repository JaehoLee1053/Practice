package com.goodlucklee.practice;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.widget.CompassView;
import com.naver.maps.map.widget.LocationButtonView;
import com.naver.maps.map.widget.ScaleBarView;
import com.naver.maps.map.widget.ZoomControlView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private final String TAG = "9999";
    private GpsTracker gpsTracker;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSION_REQUEST_CODE = 100;
    String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private FusedLocationSource mLocationSource;
    private NaverMap mNaverMap;

    double latitude;
    double longitude;

    LatLng currentLatLng;
    LatLng emartLatLng = new LatLng(37.3983, 126.9353);

    double distance;

    Marker marker1 = new Marker();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, LoadingActivity.class);
        startActivity(intent);

        // GPS ?????? ???
        gpsTracker = new GpsTracker(MainActivity.this);

        latitude = gpsTracker.getLatitude();
        longitude = gpsTracker.getLongitude();
        currentLatLng = new LatLng(latitude, longitude);

        if (!checkLocationServiceStatus()) {
            showDialogForLocationServiceSetting();
        } else {
            checkRunTimePermission();
        }
        final TextView textView_address = (TextView) findViewById(R.id.textview_location);

        Button ShowLocationButton = (Button) findViewById(R.id.btn_test_location);
        ShowLocationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                latitude = gpsTracker.getLatitude();
                longitude = gpsTracker.getLongitude();
                currentLatLng = new LatLng(latitude, longitude);

                String address = getCurrentAddress(latitude, longitude);
                textView_address.setText(address);
                onMapReady(mNaverMap);

                Toast.makeText(MainActivity.this,
                        "????????????\n??????" + latitude + "\n??????" + longitude,
                        Toast.LENGTH_LONG).show();
            }
        });

        // ??????
        Button btnMarker1 = (Button) findViewById(R.id.btn_check_point1);
        btnMarker1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                setMarker(marker1, emartLatLng.latitude, emartLatLng.longitude, R.drawable.ic_baseline_fastfood_24, 0);
                Toast.makeText(getApplication(), "??????1", Toast.LENGTH_LONG).show();
                marker1.setOnClickListener(new Overlay.OnClickListener() {
                    @Override
                    public boolean onClick(@NonNull Overlay overlay) {
                        distance = currentLatLng.distanceTo(emartLatLng) / 1000;
                        Toast.makeText(getApplication(),
                                "??????" + latitude + "\n??????" + longitude + "\n??????" + (double)Math.round(distance * 100) / 100 + "km",
                                Toast.LENGTH_LONG).show();
                        return false;
                    }
                });

            }
        });


        // ?????? ?????? ??????
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map_fragment);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map_fragment, mapFragment).commit();
        }
        // getMapAsync??? ???????????? ???????????? onMapReady ?????? ????????? ??????
        // onMapReady?????? NaverMap ????????? ??????
        mapFragment.getMapAsync(this);

        // ????????? ???????????? ???????????? FusedLocationSource ??????
        mLocationSource = new FusedLocationSource(this, PERMISSION_REQUEST_CODE);

    }

    // ActivityCompat.requestPermission??? ????????? ????????? ?????? ?????? ??????
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE
                && grantResults.length == REQUESTED_PERMISSIONS.length) {
            // ?????? ????????? PERMISSION_REQUEST_CODE??????, ????????? ????????? ???????????? ?????? ???
            boolean check_result = true;

            // ?????? ????????? ??????????????? ??????
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            if (check_result) {
                // ?????? ?????? ????????? ??? ???
                ;
            } else {
                // ????????? ???????????? ????????? ?????? ????????? ??? ?????? ?????? ?????? ??? ??? ??????, 2?????? ?????????
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUESTED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUESTED_PERMISSIONS[1])) {
                    Toast.makeText(MainActivity.this,
                            "???????????? ?????????????????????. ?????? ?????? ???????????? ???????????? ??????????????????.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            "???????????? ?????????????????????. ??????(??? ??????)?????? ???????????? ???????????? ?????????.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    void checkRunTimePermission() {
        // ????????? ????????? ??????
        // 1. ?????? ???????????? ????????? ????????? ??????
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // 2. ?????? ???????????? ????????? ?????????
            // ??????????????? 6.0 ?????? ????????? ????????? ???????????? ?????? ????????? ?????? ????????? ????????? ??????

            // 3. ?????? ?????? ????????? ??? ???
        } else {
            // 2. ????????? ????????? ????????? ?????? ???????????? ????????? ????????? ??????, 3-1/4-1 ????????? ?????? ??????
            // 3-1 ???????????? ???????????? ????????? ?????? ?????? ??????
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    REQUESTED_PERMISSIONS[0])) {
                // 3-2. ????????? ???????????? ?????? ??????????????? ???????????? ????????? ?????? ??????
                Toast.makeText(MainActivity.this,
                        "??? ?????? ???????????? ???????????? ?????? ?????? ????????? ???????????????.",
                        Toast.LENGTH_LONG).show();
                // 3-3. ??????????????? ????????? ??????, ?????? ????????? onRequestPermissionResult?????? ??????
                ActivityCompat.requestPermissions(MainActivity.this, REQUESTED_PERMISSIONS,
                        PERMISSION_REQUEST_CODE);
            } else {
                // 4-1. ???????????? ????????? ????????? ?????? ?????? ?????? ?????? ????????? ??????
                ActivityCompat.requestPermissions(MainActivity.this, REQUESTED_PERMISSIONS,
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    public String getCurrentAddress(double latitude, double longitude) {
        // ??????????????? GPS??? ????????? ??????
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            // ???????????? ??????
            Toast.makeText(this, "???????????? ????????? ????????????",
                    Toast.LENGTH_LONG).show();
            return "???????????? ????????? ????????????";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "????????? GPS ??????",
                    Toast.LENGTH_LONG).show();
            return "????????? GPS ??????";
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "?????? ?????????",
                    Toast.LENGTH_LONG).show();
            return "?????? ?????????";
        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString() + "\n";
    }

    // ?????? ??????????????? GPS ???????????? ??????
    private void showDialogForLocationServiceSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("?????? ????????? ????????????");
        builder.setMessage("?????? ???????????? ???????????? ?????? ???????????? ???????????????.\n" +
                "?????? ????????? ?????????????????????????");
        builder.setCancelable(true);
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case GPS_ENABLE_REQUEST_CODE:
                // ???????????? GPS ?????? ???????????? ??????
                if (checkLocationServiceStatus()) {
                    Log.d("@@@", "onActivityResult: GPS ?????????");
                    checkRunTimePermission();
                    return;
                }
                break;
        }
    }

    public boolean checkLocationServiceStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        Log.d(TAG, "onMapReady");

//        UiSettings uiSettings = mNaverMap.getUiSettings();
//        uiSettings.setCompassEnabled(true);
//        uiSettings.setScaleBarEnabled(true);
//        uiSettings.setZoomControlEnabled(true);
//        uiSettings.setLocationButtonEnabled(true);
//
//        CompassView compassView = findViewById(R.id.compass_view);
//        compassView.setMap(mNaverMap);
//        ScaleBarView scaleBarView = findViewById(R.id.scale_bar);
//        scaleBarView.setMap(mNaverMap);
//        ZoomControlView zoomControlView = findViewById(R.id.zoom_control);
//        zoomControlView.setMap(mNaverMap);
//        LocationButtonView locationButtonView = findViewById(R.id.location_button);
//        locationButtonView.setMap(mNaverMap);

        // ???????????? ?????? ??????
        Marker marker = new Marker();
        LatLng latlng = new LatLng(latitude, longitude);
        marker.setPosition(latlng);
        marker.setMap(naverMap);

        // NaverMap ?????? ????????? NaverMap ????????? ?????? ?????? ??????
        mNaverMap = naverMap;
        mNaverMap.setLocationSource(mLocationSource);

        CameraPosition cameraPosition = new CameraPosition(latlng, 15, 0, 0);
        naverMap.setCameraPosition(cameraPosition);

        // ????????????. ????????? onRequestPerMissionResult ?????? ????????? ??????
        ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
    }

    private void setMarker(Marker marker, double latitude, double longitude, int resourceID, int zIndex) {
        // ????????? ??????
        marker.setIconPerspectiveEnabled(true);
        // ????????? ??????
        marker.setIcon(OverlayImage.fromResource(resourceID));
        // ?????? ?????????
        marker.setAlpha(0.8f);
        // ?????? ??????
        marker.setPosition(new LatLng(latitude, longitude));
        // ?????? ????????????
        marker.setZIndex(zIndex);
        // ?????? ??????
        marker.setMap(mNaverMap);
    }
}