package com.example.eta.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * 위치 정보 관리를 위한 헬퍼 클래스입니다. (FusedLocationProviderClient 기반)
 * 현재 위치 가져오기, 지속적인 위치 업데이트 시작/중지 기능을 제공합니다.
 */
public class LocationHelper {

    private static final String TAG = "LocationHelper";

    private final FusedLocationProviderClient fusedLocationClient;
    private final Context context;
    private LocationCallback locationCallback;
    private LocationResultListener listener; // ETA 프로젝트의 리스너 인터페이스 사용
    private boolean isTracking = false;

    // 기본 위치 요청 설정 값 (protoMap의 GPS2.java 참고)
    private static final long DEFAULT_INTERVAL_MILLIS = 10000L; // 10초
    private static final long DEFAULT_FASTEST_INTERVAL_MILLIS = 5000L; // 5초
    private static final int DEFAULT_PRIORITY = Priority.PRIORITY_HIGH_ACCURACY;

    public LocationHelper(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);
    }

    public boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestLocationPermissions(@NonNull Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                requestCode);
    }

    public void getCurrentLocation(@NonNull final LocationResultListener listener) {
        this.listener = listener;
        if (!hasLocationPermissions()) {
            Log.d(TAG, "getCurrentLocation: 위치 권한 없음. 권한 요청 필요.");
            if (this.listener != null) {
                this.listener.onPermissionNeeded();
            }
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            Log.d(TAG, "getCurrentLocation: 마지막 위치 성공: " + location.getLatitude() + ", " + location.getLongitude());
                            if (this.listener != null) {
                                this.listener.onLocationSuccess(location);
                            }
                        } else {
                            Log.d(TAG, "getCurrentLocation: 마지막 위치 null. 새 위치 요청 시도.");
                            requestSingleNewLocation();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "getCurrentLocation: 마지막 위치 가져오기 실패", e);
                        if (this.listener != null) {
                            this.listener.onLocationFailure("마지막 위치 정보 가져오기 실패: " + e.getMessage());
                        }
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "getCurrentLocation: 보안 예외 발생", e);
            if (this.listener != null) {
                this.listener.onLocationFailure("보안 예외 발생 (권한 문제): " + e.getMessage());
            }
        }
    }

    private void requestSingleNewLocation() {
        if (!hasLocationPermissions()) {
            Log.d(TAG, "requestSingleNewLocation: 위치 권한 없음.");
            if (this.listener != null) {
                this.listener.onPermissionNeeded();
            }
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(DEFAULT_PRIORITY, 100)
                .setMinUpdateIntervalMillis(100)
                .setWaitForAccurateLocation(true)
                .setMaxUpdates(1)
                .build();

        LocationCallback singleUpdateCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Log.d(TAG, "requestSingleNewLocation: 새 위치 수신 성공: " + location.getLatitude() + ", " + location.getLongitude());
                    if (LocationHelper.this.listener != null) {
                        LocationHelper.this.listener.onLocationSuccess(location);
                    }
                } else {
                    Log.d(TAG, "requestSingleNewLocation: 새 위치 결과가 null");
                    if (LocationHelper.this.listener != null) {
                        LocationHelper.this.listener.onLocationFailure("새로운 위치 정보를 가져오는데 실패했습니다 (결과 null).");
                    }
                }
            }

            @Override
            public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                if (!locationAvailability.isLocationAvailable()) {
                    Log.d(TAG, "requestSingleNewLocation: 위치 사용 불가능.");
                    if (LocationHelper.this.listener != null) {
                        LocationHelper.this.listener.onLocationFailure("현재 위치를 사용할 수 없습니다. (GPS 설정 등 확인)");
                    }
                }
            }
        };

        try {
            Log.d(TAG, "requestSingleNewLocation: 단일 위치 업데이트 요청 시작");
            fusedLocationClient.requestLocationUpdates(locationRequest, singleUpdateCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "requestSingleNewLocation: 보안 예외 발생", e);
            if (this.listener != null) {
                this.listener.onLocationFailure("보안 예외 발생 (실시간 업데이트 권한 문제): " + e.getMessage());
            }
        }
    }

    public void startLocationTracking(@NonNull final LocationResultListener listener, long intervalMillis, long fastestIntervalMillis, int priority) {
        this.listener = listener;

        if (!hasLocationPermissions()) {
            Log.d(TAG, "startLocationTracking: 위치 권한 없음. 권한 요청 필요.");
            if (this.listener != null) {
                this.listener.onPermissionNeeded();
            }
            return;
        }

        if (isTracking) {
            Log.d(TAG, "startLocationTracking: 이미 위치 추적 중입니다.");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(priority, intervalMillis)
                .setMinUpdateIntervalMillis(fastestIntervalMillis)
                .build();

        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    Location lastLocation = locationResult.getLastLocation();
                    if (lastLocation != null) {
                        if (LocationHelper.this.listener != null) {
                            LocationHelper.this.listener.onLocationSuccess(lastLocation);
                        }
                    } else {
                        Log.d(TAG, "startLocationTracking: onLocationResult - 위치 정보 없음.");
                    }
                }

                @Override
                public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
                    if (!locationAvailability.isLocationAvailable()) {
                        Log.w(TAG, "startLocationTracking: onLocationAvailability - 위치 사용 불가능 (예: GPS 꺼짐).");
                        if (LocationHelper.this.listener != null) {
                            // LocationHelper.this.listener.onLocationFailure("현재 위치를 사용할 수 없습니다. GPS 설정을 확인해주세요.");
                        }
                    }
                }
            };
        }

        try {
            Log.i(TAG, "startLocationTracking: 위치 추적 시작 요청.");
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            isTracking = true;
        } catch (SecurityException e) {
            Log.e(TAG, "startLocationTracking: 위치 추적 시작 실패 (보안 예외)", e);
            if (this.listener != null) {
                this.listener.onLocationFailure("위치 추적 시작 실패 (보안 예외): " + e.getMessage());
            }
            isTracking = false;
        }
    }

    public void startLocationTracking(@NonNull final LocationResultListener listener) {
        startLocationTracking(listener, DEFAULT_INTERVAL_MILLIS, DEFAULT_FASTEST_INTERVAL_MILLIS, DEFAULT_PRIORITY);
    }

    public void stopLocationTracking() {
        if (!isTracking) {
            Log.d(TAG, "stopLocationTracking: 이미 추적이 중지된 상태입니다.");
            return;
        }
        if (locationCallback != null) {
            try {
                Log.i(TAG, "stopLocationTracking: 위치 추적 중지 요청.");
                fusedLocationClient.removeLocationUpdates(locationCallback);
                isTracking = false;
            } catch (Exception e) {
                Log.e(TAG, "stopLocationTracking: 위치 업데이트 제거 중 오류 발생", e);
            }
        } else {
            Log.w(TAG, "stopLocationTracking: isTracking이 true이지만 locationCallback이 null입니다. 상태를 강제로 false로 변경합니다.");
            isTracking = false;
        }
    }

    public boolean isTracking() {
        return isTracking;
    }
}