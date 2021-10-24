package com.goodlucklee.practice;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private GpsTracker gpsTracker;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSION_REQUEST_CODE = 100;
    String[] REQUESTED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!checkLocationServiceStatus()) {
            showDialogForLocationServiceSetting();
        } else {
            checkRunTimePermission();
        }
        final TextView textView_address = (TextView) findViewById(R.id.textview_location);

        Button ShowLocationButton = (Button) findViewById(R.id.btn_test_loaction);
        ShowLocationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                gpsTracker = new GpsTracker(MainActivity.this);

                double latitute = gpsTracker.getLatitude();
                double longitude = gpsTracker.getLongitude();

                String address = getCurrentAddress(latitute, longitude);
                textView_address.setText(address);

                Toast.makeText(MainActivity.this,
                        "현재위치\n위도" + latitute + "\n경도" + longitude,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ActivityCompat.requestPermission을 사용한 퍼미션 요청 결과 리턴
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE
                && grantResults.length == REQUESTED_PERMISSIONS.length) {
            // 요청 코드가 PERMISSION_REQUEST_CODE이고, 요청한 퍼미션 개수만큼 수신 시
            boolean check_result = true;

            // 모든 퍼미션 체크했는지 확인
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            if (check_result) {
                // 위치 값을 가져올 수 있
                ;
            } else {
                // 거부한 퍼미션이 있으면 앱을 사용할 수 없는 이유 설명 후 앱 종류, 2가지 케이스
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUESTED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUESTED_PERMISSIONS[1])) {
                    Toast.makeText(MainActivity.this,
                            "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    void checkRunTimePermission() {
        // 런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            // 2. 이미 퍼미션을 가지고 있다면
            // 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요 없어서 이미 허용된 것으로 인식

            // 3. 위치 값을 가져올 수 있
        } else {
            // 2. 퍼미션 요청을 허용한 적이 ㅇ벗다면 퍼미션 요청이 필요, 3-1/4-1 두가지 경우 존재
            // 3-1 사용자가 퍼미션을 거부한 적이 있는 경우
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    REQUESTED_PERMISSIONS[0])) {
                // 3-2. 요청을 진행하기 전에 사용자에게 퍼미션이 필요한 이유 설명
                Toast.makeText(MainActivity.this,
                        "이 앱을 실항하기 위해서는 위치 접근 권한이 필요합니다.",
                        Toast.LENGTH_LONG).show();
                // 3-3. 사용자에게 퍼미션 요청, 요청 결과는 onRequestPermissionResult에서 수신
                ActivityCompat.requestPermissions(MainActivity.this, REQUESTED_PERMISSIONS,
                        PERMISSION_REQUEST_CODE);
            } else {
                // 4-1. 사용자가 퍼미션 거부한 적이 없는 경우 바로 퍼미션 요청
                ActivityCompat.requestPermissions(MainActivity.this, REQUESTED_PERMISSIONS,
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    public String getCurrentAddress(double latitude, double longitude) {
        // 지오코더로 GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            // 네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가",
                    Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불기";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표",
                    Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견",
                    Toast.LENGTH_LONG).show();
            return "주소 미발견";
        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString() + "\n";
    }

    // 하기 메소드들은 GPS 활성화에 사용
    private void showDialogForLocationServiceSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n" +
                "위치 설정을 수정하시겠습니까?");
        builder.setCancelable(true);
        builder.setPositiveButton("허용", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
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
                // 사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServiceStatus()) {
                    Log.d("@@@", "onActivityResult: GPS 활성화");
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
}