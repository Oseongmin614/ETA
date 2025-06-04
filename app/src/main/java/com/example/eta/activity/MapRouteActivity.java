package com.example.eta.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.eta.R;
import com.example.eta.util.Keyholder;
import com.example.eta.util.LocationHelper; // 수정된 LocationHelper 사용
import com.example.eta.util.LocationResultListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import java.util.ArrayList;
import java.util.List;
// import java.util.Locale; // 기존 ETA의 formatDuration, formatDistance는 Locale 불필요

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class MapRouteActivity extends AppCompatActivity implements LocationResultListener {

    private static final String TAG = "RouteMapActivity";
    private static final String ROUTE_API_BASE = "https://apis.openapi.sk.com";
    private static final int APP_LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // UI 컴포넌트
    private FrameLayout mapContainer;
    private TextView textRouteInfo;
    private TMapView tMapView;

    // 데이터 (경로 안내 관련 - ETA 스타일 유지)
    private String startAddr;
    private String endAddr;
    private String nickname; // 사용되지 않으면 제거 가능
    private String userId;   // 사용되지 않으면 제거 가능
    private TMapPoint startPoint;
    private TMapPoint endPoint;

    // 위치 관련 (protoMap 스타일 적용)
    private LocationHelper locationHelper;
    private TMapMarkerItem markergps = new TMapMarkerItem(); // 현위치 마커

    // 네트워크 (경로 안내 관련 - ETA 스타일 유지)
    private RouteService routeService;

    // Retrofit 인터페이스 (경로 안내 관련 - ETA 스타일 유지)
    interface RouteService {
        @Headers({
                "Accept: application/json",
                "Content-Type: application/json"
        })
        @POST("/transit/routes")
        Call<JsonObject> getRoute(@Header("appKey") String apiKey, @Body JsonObject body);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_map);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("경로 안내");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        // 위치 표시 로직 초기화 (protoMap 스타일)
        initLocationHelperAndMarkerIcon(); // LocationHelper 초기화 및 현위치 마커 아이콘 설정
        // 경로 안내 로직 초기화 (ETA 스타일)
        initTMapAndRetrofit(); // TMapView 및 Retrofit 초기화
        getIntentData(); // Intent 데이터 가져오기

        if (startAddr != null && endAddr != null) {
            calculateRoute(); // 기존 ETA 경로 계산 로직 호출
        }
        // 위치 업데이트 시작은 onResume으로 이동 (protoMap 스타일)
    }

    private void initViews() {
        mapContainer = findViewById(R.id.tmapcon);
        textRouteInfo = findViewById(R.id.text_route_info);
        textRouteInfo.setTextColor(getResources().getColor(R.color.text_primary));
        textRouteInfo.setBackgroundColor(getResources().getColor(R.color.surface_color));
    }

    // 위치 표시 관련 초기화 (protoMap 스타일)
    private void initLocationHelperAndMarkerIcon() {
        locationHelper = new LocationHelper(this); // FusedLocationProviderClient 기반 LocationHelper
        // 현위치 마커 아이콘 설정 (protoMap 스타일)
        Drawable drawable = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation); // ETA 기존 아이콘 사용
        if (drawable != null) {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            markergps.setIcon(bitmap);
            markergps.setName("현위치");
            markergps.setPosition(0.5f, 1.0f); // 아이콘의 중심을 하단 중앙으로 (protoMap 스타일)
        } else {
            Log.e(TAG, "현위치 마커 아이콘 로드 실패");
        }
    }

    // 경로 안내 관련 초기화 (ETA 스타일)
    private void initTMapAndRetrofit() {
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(Keyholder.getAppKey()); // 기존 Keyholder 사용
        tMapView.setZoomLevel(16); // 초기 줌 레벨
        mapContainer.addView(tMapView);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ROUTE_API_BASE)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        routeService = retrofit.create(RouteService.class);
    }

    // 경로 안내 관련 (ETA 스타일 유지)
    private void getIntentData() {
        Intent intent = getIntent();
        endAddr = intent.getStringExtra("end");
        startAddr = intent.getStringExtra("start");
        nickname = intent.getStringExtra("nickname");
        userId = intent.getStringExtra("userId");
        Log.d(TAG, "받은 데이터 - 출발지: " + startAddr + ", 도착지: " + endAddr);
    }

    // 경로 안내 관련 (ETA 스타일 유지)
    private void calculateRoute() {
        if (TextUtils.isEmpty(startAddr) || !startAddr.contains(",") ||
                TextUtils.isEmpty(endAddr) || !endAddr.contains(",")) {
            Log.e(TAG, "잘못된 주소 형식: " + startAddr + " / " + endAddr);
            Toast.makeText(this, "주소 형식이 잘못되었습니다", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String[] startCoords = startAddr.split(",");
            double startLat = Double.parseDouble(startCoords[0].trim());
            double startLon = Double.parseDouble(startCoords[1].trim());
            String[] endCoords = endAddr.split(",");
            double endLat = Double.parseDouble(endCoords[0].trim());
            double endLon = Double.parseDouble(endCoords[1].trim());
            Log.d(TAG, "출발지 좌표: " + startLat + "," + startLon);
            Log.d(TAG, "도착지 좌표: " + endLat + "," + endLon);
            startPoint = new TMapPoint(startLat, startLon);
            endPoint = new TMapPoint(endLat, endLon);
            requestRoute(startLon, startLat, endLon, endLat);
        } catch (NumberFormatException e) {
            Log.e(TAG, "좌표 파싱 실패", e);
            Toast.makeText(this, "좌표 형식이 잘못되었습니다", Toast.LENGTH_SHORT).show();
        }
    }

    // 경로 안내 관련 (ETA 스타일 유지)
    private void requestRoute(double startLon, double startLat, double endLon, double endLat) {
        JsonObject body = new JsonObject();
        body.addProperty("startX", String.valueOf(startLon));
        body.addProperty("startY", String.valueOf(startLat));
        body.addProperty("endX", String.valueOf(endLon));
        body.addProperty("endY", String.valueOf(endLat));
        body.addProperty("count", 1);
        body.addProperty("lang", 0); // ETA 기존 스타일 (0)
        body.addProperty("format", "json");

        routeService.getRoute(Keyholder.getAppKey(), body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    drawOnMap(startPoint, endPoint, response.body()); // ETA 기존 drawOnMap 호출
                } else {
                    Log.e(TAG, "Route API 실패: " + response.code() + " " + response.message());
                    Toast.makeText(MapRouteActivity.this, "경로 계산에 실패했습니다", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Route API 오류: " + t.getMessage());
                Toast.makeText(MapRouteActivity.this, "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 경로 안내 관련 (ETA 스타일 유지)
    // 단, 출발/도착지 마커 생성은 기존 createMarker 사용, 현위치 마커는 onLocationSuccess에서 별도 관리
    private void drawOnMap(TMapPoint start, TMapPoint end, JsonObject response) {
        // 출발지, 도착지 마커 추가 (ETA 기존 방식)
        tMapView.addMarkerItem("start", createMarker(start, "출발지")); // 기존 createMarker 사용
        tMapView.addMarkerItem("end", createMarker(end, "도착지"));   // 기존 createMarker 사용
        tMapView.setCenterPoint(start.getLongitude(), start.getLatitude());

        // 폴리라인 그리기 (ETA 기존 방식)
        TMapPolyLine poly = new TMapPolyLine();
        poly.setLineColor(getResources().getColor(R.color.button_primary));
        poly.setLineWidth(8);

        try {
            JsonObject itinerary = response.getAsJsonObject("metaData")
                    .getAsJsonObject("plan")
                    .getAsJsonArray("itineraries")
                    .get(0)
                    .getAsJsonObject();
            int totalTime = itinerary.get("totalTime").getAsInt();
            JsonArray legs = itinerary.getAsJsonArray("legs");

            for (JsonElement legElement : legs) {
                JsonObject leg = legElement.getAsJsonObject();
                String lineString = null;
                if (leg.has("passShape")) {
                    lineString = leg.getAsJsonObject("passShape").get("linestring").getAsString();
                } else if (leg.has("steps")) {
                    JsonArray steps = leg.getAsJsonArray("steps");
                    for (JsonElement stepElement : steps) {
                        JsonObject step = stepElement.getAsJsonObject();
                        if (step.has("linestring")) {
                            lineString = step.get("linestring").getAsString();
                            break;
                        }
                    }
                }
                if (lineString != null) {
                    for (String coordinate : lineString.split(" ")) {
                        String[] coords = coordinate.split(",");
                        if (coords.length >= 2) {
                            double lon = Double.parseDouble(coords[0].trim());
                            double lat = Double.parseDouble(coords[1].trim());
                            poly.addLinePoint(new TMapPoint(lat, lon));
                        }
                    }
                }
            }
            tMapView.addTMapPolyLine("route", poly);

            // 시간 포맷팅 (ETA 기존 방식)
            String timeFormatted = String.format("%02d:%02d:%02d",
                    totalTime / 3600, (totalTime % 3600) / 60, totalTime % 60);

            // 경로 안내 텍스트 생성 (ETA 기존 방식)
            List<String> instructions = generateTextInstructions(response); // 기존 generateTextInstructions 호출
            instructions.add(0, "총 소요 시간: " + timeFormatted);
            textRouteInfo.setText(TextUtils.join("\n", instructions));

        } catch (Exception e) {
            Log.e(TAG, "지도 그리기 실패", e);
            Toast.makeText(this, "경로 표시에 실패했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    // 경로 안내 관련 - 출발/도착지 마커 생성 (ETA 스타일 유지)
    private TMapMarkerItem createMarker(TMapPoint point, String title) {
        TMapMarkerItem marker = new TMapMarkerItem();
        // ETA 기존 방식: 모든 마커에 동일한 아이콘 사용
        Drawable drawable = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation);
        if (drawable != null) {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            marker.setIcon(bitmap);
        }
        marker.setTMapPoint(point);
        marker.setName(title);
        // 필요시 setPosition, setCanShowCallout 등 추가 가능
        return marker;
    }

    // 경로 안내 관련 - Segment 클래스 (ETA 스타일 유지)
    private static class Segment {
        String mode, start, end, route;
        int distance, time;
        Segment(String mode, String start, String end, int distance, int time, String route) {
            this.mode = mode;
            this.start = start;
            this.end = end;
            this.distance = distance;
            this.time = time;
            this.route = route;
        }
    }

    // 경로 안내 관련 - 텍스트 생성 (ETA 스타일 유지)
    private List<String> generateTextInstructions(JsonObject response) {
        try {
            JsonObject plan = response.getAsJsonObject("metaData")
                    .getAsJsonObject("plan")
                    .getAsJsonArray("itineraries")
                    .get(0)
                    .getAsJsonObject();
            JsonArray legs = plan.getAsJsonArray("legs");
            JsonObject first = legs.get(0).getAsJsonObject();
            Segment current = new Segment(
                    first.get("mode").getAsString(),
                    first.getAsJsonObject("start").get("name").getAsString(),
                    first.getAsJsonObject("end").get("name").getAsString(),
                    first.get("distance").getAsInt(),
                    first.get("sectionTime").getAsInt(),
                    first.has("route") ? first.get("route").getAsString() : null
            );
            List<Segment> segments = new ArrayList<>();
            for (int i = 1; i < legs.size(); i++) {
                JsonObject leg = legs.get(i).getAsJsonObject();
                String mode = leg.get("mode").getAsString();
                if (mode.equals(current.mode)) {
                    current.distance += leg.get("distance").getAsInt();
                    current.time += leg.get("sectionTime").getAsInt();
                    current.end = leg.getAsJsonObject("end").get("name").getAsString();
                } else {
                    segments.add(current);
                    current = new Segment(
                            mode,
                            leg.getAsJsonObject("start").get("name").getAsString(),
                            leg.getAsJsonObject("end").get("name").getAsString(),
                            leg.get("distance").getAsInt(),
                            leg.get("sectionTime").getAsInt(),
                            leg.has("route") ? leg.get("route").getAsString() : null
                    );
                }
            }
            segments.add(current);
            List<String> instructions = new ArrayList<>();
            for (Segment seg : segments) {
                String transport;
                switch (seg.mode) {
                    case "WALK": transport = "도보"; break;
                    case "BUS": transport = "버스(" + seg.route + ")"; break;
                    case "SUBWAY": transport = "지하철(" + seg.route + ")"; break;
                    default: transport = seg.mode;
                }
                String distance = formatDistance(seg.distance); // ETA 기존 formatDistance 호출
                String time = formatDuration(seg.time);       // ETA 기존 formatDuration 호출
                instructions.add(transport + ": " + seg.start + " → " + seg.end +
                        ", 거리 " + distance + ", 소요시간 " + time);
            }
            return instructions;
        } catch (Exception e) {
            Log.e(TAG, "경로 안내 생성 실패", e);
            List<String> errorList = new ArrayList<>();
            errorList.add("경로 안내 정보를 생성할 수 없습니다.");
            return errorList;
        }
    }

    // 경로 안내 관련 - 거리 포맷팅 (ETA 스타일 유지)
    private String formatDistance(int meters) {
        if (meters >= 1000) {
            return String.format("%.1fkm", meters / 1000f);
        } else {
            return meters + "m";
        }
    }

    // 경로 안내 관련 - 시간 포맷팅 (ETA 스타일 유지)
    private String formatDuration(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("시간 ");
        if (m > 0) sb.append(m).append("분 ");
        if (s > 0) sb.append(s).append("초");
        return sb.toString().trim();
    }

    // --- 위치 표시 관련 (protoMap 스타일 적용) ---
    @Override
    public void onLocationSuccess(Location location) {
        if (location == null || tMapView == null) return;

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        Log.d(TAG, "현위치 업데이트: " + latitude + ", " + longitude);

        TMapPoint gps = new TMapPoint(latitude, longitude);
        markergps.setTMapPoint(gps); // 마커 위치 업데이트

        // 마커가 지도에 없으면 추가, 있으면 위치만 업데이트 (protoMap 스타일)
        if (tMapView.getMarkerItemFromID("markergps") == null) {
            tMapView.addMarkerItem("markergps", markergps);
        }
        // 현위치로 지도 중심 이동 (선택 사항)
        // tMapView.setCenterPoint(longitude, latitude, true); // 부드럽게 이동
    }

    @Override
    public void onLocationFailure(String errorMessage) { // protoMap 스타일
        Toast.makeText(this, "위치 오류: " + errorMessage, Toast.LENGTH_LONG).show();
        // if (locationHelper != null && locationHelper.isTracking()) { stopLocationUpdates(); } // 필요시
    }

    @Override
    public void onPermissionNeeded() { // protoMap 스타일
        Toast.makeText(this, "현위치 표시를 위해 위치 권한이 필요합니다.", Toast.LENGTH_LONG).show();
        if (locationHelper != null) {
            locationHelper.requestLocationPermissions(this, APP_LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    // 위치 업데이트 관리 (protoMap 스타일)
    private void startLocationUpdates() {
        if (locationHelper == null) return;
        if (!locationHelper.hasLocationPermissions()) {
            Log.d(TAG, "위치 권한 없음. LocationHelper 통해 권한 요청 시도.");
            locationHelper.startLocationTracking(this); // 내부에서 onPermissionNeeded 호출 유도
        } else {
            Log.d(TAG, "위치 권한 있음. 위치 추적 시작.");
            locationHelper.startLocationTracking(this);
            Toast.makeText(this, "현위치 추적을 시작합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationUpdates() { // protoMap 스타일
        if (locationHelper != null && locationHelper.isTracking()) {
            locationHelper.stopLocationTracking();
            Toast.makeText(this, "현위치 추적을 중지합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { // protoMap 스타일
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == APP_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "위치 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
                startLocationUpdates(); // 권한 허용 후 위치 업데이트 시작
            } else {
                Toast.makeText(this, "위치 권한이 거부되었습니다. 현위치 기능이 제한됩니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Activity 생명주기 (protoMap 스타일 위치 관리)
    @Override
    protected void onResume() {
        super.onResume();
        if (locationHelper != null && !locationHelper.isTracking()) {
            Log.d(TAG, "onResume: 위치 추적 시작 호출");
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: 위치 추적 중지 호출");
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: 위치 추적 중지 호출");
        if (locationHelper != null) {
            locationHelper.stopLocationTracking();
        }
        // tMapView 리소스 정리 (필요시)
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}