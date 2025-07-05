package com.example.eta.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.eta.R;
import com.example.eta.util.LocationHelper;
import com.example.eta.util.LocationResultListener;

public class LocationService extends Service implements LocationResultListener {

    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final int NOTIFICATION_ID = 12345;

    // --- 통신을 위한 변수 ---
    private final IBinder binder = new LocalBinder();
    private LocationClientListener clientListener;

    // --- 위치 관련 변수 ---
    private LocationHelper locationHelper;
    private MapDBInsertService mapDBInsertService;
    private long lastLogTime = 0;
    private String chatRoomId;
    private String userId;
    private String endAddr;
    private double endLat = -1; // [추가] 목적지 위도
    private double endLon = -1; // [추가] 목적지 경도


    /**
     * 액티비티와 서비스가 통신하기 위한 인터페이스
     */
    public interface LocationClientListener {
        void onLocationUpdated(Location location);
    }

    /**
     * 액티비티에 서비스 인스턴스를 반환하기 위한 Binder
     */
    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationHelper = new LocationHelper(this);
        mapDBInsertService = new MapDBInsertService();
        createNotificationChannel();
        Log.i(TAG, "LocationService 생성됨");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "LocationService 시작됨");

        // Intent로부터 채팅방 ID와 유저 ID를 받음
        if (intent != null) {
            this.chatRoomId = intent.getStringExtra("roomId");
            this.userId = intent.getStringExtra("userId");
            this.endAddr = intent.getStringExtra("endAddr");
            // [추가] endAddr 문자열을 파싱하여 위도, 경도로 변환
            if (this.endAddr != null && !this.endAddr.isEmpty()) {
                try {
                    String[] coords = this.endAddr.split(",");
                    if (coords.length == 2) {
                        this.endLat = Double.parseDouble(coords[0].trim());
                        this.endLon = Double.parseDouble(coords[1].trim());
                        Log.i(TAG, "목적지 좌표 설정됨: " + endLat + ", " + endLon);
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "잘못된 형식의 목적지 좌표입니다: " + this.endAddr, e);
                    // 파싱 실패 시, 도착 처리 로직이 실행되지 않도록 초기값(-1)을 유지
                    this.endLat = -1;
                    this.endLon = -1;
                }
            }
        }

        // 포그라운드 서비스 시작
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            // 포그라운드 서비스 위치 타입 지정
            startForeground(NOTIFICATION_ID, createNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        // 위치 추적 시작
        if (locationHelper != null && !locationHelper.isTracking()) {
            locationHelper.startLocationTracking(this);
        }

        // 서비스가 시스템에 의해 종료될 경우 재시작하도록 설정
        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locationHelper != null) {
            locationHelper.stopLocationTracking();
        }
        Log.i(TAG, "LocationService 중지됨");
    }

    /**
     * 액티비티에서 리스너를 등록하기 위한 메서드
     * @param listener 액티비티 자신 (this)
     */
    public void setClientListener(LocationClientListener listener) {
        this.clientListener = listener;
    }


    // --- LocationResultListener 구현부 ---
    @Override
    public void onLocationSuccess(Location location) {
        if (location == null) return;

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        Log.d(TAG, "서비스에서 현위치 수신: " + latitude + ", " + longitude);




        // 1. DB에 위치 정보 전송 (3초 간격)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime > 3000) {
            lastLogTime = currentTime;
            if (chatRoomId != null && userId != null) {
                mapDBInsertService.insertGPS(chatRoomId, userId, latitude + "," + longitude);
            }
        }

        // 2. 연결된 액티비티(클라이언트)에 위치 정보 전달
        if (clientListener != null) {
            clientListener.onLocationUpdated(location);
        }

        //3. 도착 처리
        // 목적지 좌표가 유효하게 설정되었는지 확인
        if (endLat != -1 && endLon != -1) {
            // 현재 위치와 목적지 위치의 위도, 경도 차이의 합을 계산
            double latDiff = Math.abs(latitude - endLat);
            double lonDiff = Math.abs(longitude - endLon);

            // 차이의 합이 0.01 이하이면 도착으로 판단
            // 참고: 위도/경도 0.01 차이는 대략 1km 내외의 거리입니다.
            if ((latDiff + lonDiff) <= 0.01) {
                Log.i(TAG, "목적지에 도착했습니다! 위치 서비스를 종료합니다.");
                mapDBInsertService.insertEnd(chatRoomId, userId);
                // 서비스 종료. onDestroy()가 호출되어 위치 추적도 중단됨.
                stopSelf();
            }
        }
    }

    @Override
    public void onLocationFailure(String errorMessage) {
        Log.e(TAG, "위치 오류: " + errorMessage);
        // 필요시 액티비티에 오류 전달 로직 추가 가능
    }

    @Override
    public void onPermissionNeeded() {
        Log.w(TAG, "위치 권한 필요. 액티비티에서 처리해야 함.");
        // 서비스는 직접 권한을 요청할 수 없으므로, 액티비티에서 처리해야 함
    }


    // --- 포그라운드 서비스 알림 관련 ---
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("위치 추적 중")
                .setContentText("앱이 현재 위치를 추적하고 있습니다.")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: 적절한 아이콘으로 변경
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}