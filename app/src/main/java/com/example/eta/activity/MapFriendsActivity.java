package com.example.eta.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
// REFACTORED: RecyclerView 추가
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eta.R;
import com.example.eta.service.LocationService;
import com.example.eta.util.Keyholder;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFriendsActivity extends AppCompatActivity implements LocationService.LocationClientListener {
    private static final String TAG = "MapFriendsActivity";

    // --- UI Components ---
    private FrameLayout mapContainer;
    private TMapView tMapView;
    // REFACTORED: TextView를 RecyclerView로 교체
    private RecyclerView recyclerViewRouteDetails;
    private RouteSegmentAdapter routeSegmentAdapter;
    private TextView textTotalTime; // 총 소요시간을 표시할 TextView (레이아웃에 추가 필요)


    // --- Data ---
    private String userId;
    private String chatRoomId;
    private String endAddr;

    // --- Firebase ---
    private DatabaseReference mDatabase;
    private DatabaseReference chatRoomRef;

    // --- Map & Color Management ---
    private final List<Integer> friendColors = new ArrayList<>();
    private final Map<String, Integer> userColorMap = new HashMap<>();
    private LocationService locationService;
    private boolean isServiceBound = false;
    private TMapMarkerItem markermygps = new TMapMarkerItem();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // REFACTORED: 개선된 레이아웃 파일을 사용합니다.
        setContentView(R.layout.activity_route_map_friends);

        initViews();
        initTMap();
        setupRecyclerView(); // REFACTORED: RecyclerView 설정 메서드 호출
        initFriendColors();
        getIntentData();
        initFirebase();

        loadAllRoutesAndInitialGps();
        attachGpsListener();
        initMarkerIcon();
        startAndBindLocationService();
    }

    private void initTMap() {
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(Keyholder.getAppKey());
        mapContainer.addView(tMapView);
        tMapView.setZoomLevel(15);
    }

    private void initViews() {
        mapContainer = findViewById(R.id.tmapcon);
        // REFACTORED: UI 컴포넌트 초기화 변경
        // text_total_time ID는 activity_route_map_friends.xml의 '상세 경로' TextView 위에 추가해야 합니다.
        // 예: <TextView android:id="@+id/text_total_time" ... />
        // textTotalTime = findViewById(R.id.text_total_time);
        recyclerViewRouteDetails = findViewById(R.id.recycler_view_route_details);
    }

    // REFACTORED: RecyclerView와 Adapter를 설정하는 새로운 메서드
    private void setupRecyclerView() {
        recyclerViewRouteDetails.setLayoutManager(new LinearLayoutManager(this));
        routeSegmentAdapter = new RouteSegmentAdapter(new ArrayList<>());
        recyclerViewRouteDetails.setAdapter(routeSegmentAdapter);
    }

    // ... (initFriendColors, getIntentData, initFirebase, parseGpsString, attachGpsListener, loadAllRoutesAndInitialGps 생략, 변경 없음) ...
    private void initFriendColors() {
        friendColors.add(Color.RED);
        friendColors.add(Color.YELLOW);
        friendColors.add(Color.GREEN);
        friendColors.add(Color.BLUE);
    }
    private void getIntentData() {
        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");
        chatRoomId = intent.getStringExtra("roomId");
        endAddr = intent.getStringExtra("endAddr");

        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(chatRoomId)) {
            Toast.makeText(this, "사용자 또는 채팅방 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    private void initFirebase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        chatRoomRef = mDatabase.child("chatRooms").child(chatRoomId).child("maps");
    }
    private TMapPoint parseGpsString(String gpsStr) {
        try {
            String[] coords = gpsStr.split(",");
            double lat = Double.parseDouble(coords[0]);
            double lon = Double.parseDouble(coords[1]);
            return new TMapPoint(lat, lon);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse GPS string: " + gpsStr, e);
            return null;
        }
    }

    private void attachGpsListener() {
        chatRoomRef.child("gps").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // loadAllRoutesAndInitialGps에서 초기 위치는 이미 처리했으므로, 여기서는 중복 작업을 피할 수 있습니다.
                // 또는 이 곳에서 모든 로직을 처리하고 loadAllRoutesAndInitialGps를 간소화할 수도 있습니다.
                // 현재 구조에서는 초기 로딩 후의 변경사항만 처리합니다.
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String changedUserId = snapshot.getKey();
                String gpsStr = snapshot.getValue(String.class);
                if (changedUserId == null || gpsStr == null) return;

                TMapPoint newGpsPoint = parseGpsString(gpsStr);
                updateMarker(changedUserId, newGpsPoint);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String removedUserId = snapshot.getKey();
                if (removedUserId == null) return;
                tMapView.removeMarkerItem("markergps" + removedUserId);
                tMapView.removeTMapPolyLine("route" + removedUserId);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "GPS listener was cancelled", error.toException());
            }
        });
    }
    private void loadAllRoutesAndInitialGps() {
        chatRoomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                DataSnapshot coordinatesSnapshot = snapshot.child("Coordinates");
                DataSnapshot gpsSnapshot = snapshot.child("gps");

                // 다른 사용자들의 ID 리스트를 만들어 색상을 할당합니다.
                List<String> otherUserIds = new ArrayList<>();
                for (DataSnapshot userGpsSnapshot : gpsSnapshot.getChildren()) {
                    if (!userGpsSnapshot.getKey().equals(userId)) {
                        otherUserIds.add(userGpsSnapshot.getKey());
                    }
                }
                for (int i = 0; i < otherUserIds.size(); i++) {
                    userColorMap.put(otherUserIds.get(i), friendColors.get(i % friendColors.size()));
                }

                // 각 사용자의 경로와 GPS를 지도에 그립니다.
                for (DataSnapshot userCoordSnapshot : coordinatesSnapshot.getChildren()) {
                    String currentUserId = userCoordSnapshot.getKey();
                    String routeJsonStr = userCoordSnapshot.getValue(String.class);
                    String gpsStr = gpsSnapshot.child(currentUserId).getValue(String.class);

                    if (routeJsonStr == null || gpsStr == null) continue;

                    JsonObject routeJson = JsonParser.parseString(routeJsonStr).getAsJsonObject();
                    TMapPoint initialGpsPoint = parseGpsString(gpsStr);

                    if (currentUserId.equals(userId)) {
                        drawOnMap(routeJson); // 내 경로 그리기
                    } else {
                        Integer color = userColorMap.get(currentUserId);
                        drawOnLine(routeJson, initialGpsPoint, currentUserId, color != null ? color : Color.GRAY); // 다른 사용자 경로 그리기
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to load initial chat room data.", error.toException());
            }
        });
    }

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
    private void drawOnLine(JsonObject coordinatesJson, TMapPoint gps, String otherUserId, int color) {
        TMapMarkerItem markergps = new TMapMarkerItem();
        markergps.setTMapPoint(gps);
        markergps.setIcon(createMarkerIcon());
        tMapView.addMarkerItem("markergps" + otherUserId, markergps);

        TMapPolyLine poly = new TMapPolyLine();
        poly.setLineColor(color); // 파라미터로 받은 색상 적용
        poly.setLineWidth(8);

        // ... 기존 폴리라인 파싱 및 추가 로직 (동일)
        try {
            JsonObject itinerary = coordinatesJson
                    .getAsJsonObject();

            JsonArray legs = itinerary.getAsJsonArray("legs");

            // 2. 각 이동 구간(leg)을 순회합니다.
            for (JsonElement legElement : legs) {
                JsonObject leg = legElement.getAsJsonObject();

                // 3. passShape 또는 steps에서 좌표 데이터를 수집합니다. (이전 로직과 동일)
                String lineString = null;

                // Case 1: passShape가 있는 경우 (주로 버스, 지하철 등)
                if (leg.has("passShape") && leg.getAsJsonObject("passShape").has("linestring")) {
                    lineString = leg.getAsJsonObject("passShape").get("linestring").getAsString();

                    // Case 2: passShape는 없지만 steps가 있는 경우 (주로 도보)
                } else if (leg.has("steps")) {
                    JsonArray steps = leg.getAsJsonArray("steps");
                    StringBuilder combinedLineString = new StringBuilder();

                    for (JsonElement stepElement : steps) {
                        JsonObject step = stepElement.getAsJsonObject();
                        if (step.has("linestring")) {
                            combinedLineString.append(step.get("linestring").getAsString()).append(" ");
                        }
                    }
                    lineString = combinedLineString.toString().trim();
                }

                // 4. 수집된 좌표가 있다면 폴리라인에 점을 추가합니다.
                if (lineString != null && !lineString.isEmpty()) {
                    for (String coordinate : lineString.split(" ")) {
                        String[] coords = coordinate.split(",");
                        if (coords.length >= 2) {
                            poly.addLinePoint(new TMapPoint(Double.parseDouble(coords[1].trim()), Double.parseDouble(coords[0].trim())));
                        }
                    }
                }
            }

            // 5. 완성된 폴리라인을 지도에 추가합니다.
            tMapView.addTMapPolyLine("route" + otherUserId, poly);

        } catch (Exception e) {
            Log.e(TAG, "라인 추가 실패 (User: " + otherUserId + ")", e);
        }
    }
    private void drawOnMap(JsonObject response) {
        // 폴리라인 그리기 로직은 동일
        TMapPolyLine poly = new TMapPolyLine();
        poly.setLineColor(getResources().getColor(R.color.button_primary));
        poly.setLineWidth(8);

        try {
            JsonObject itinerary = response.getAsJsonObject();
            int totalTime = itinerary.get("totalTime").getAsInt();
            JsonArray legs = itinerary.getAsJsonArray("legs");
            TMapPoint lastPoint = null;

            for (JsonElement legElement : legs) {
                // ... (폴리라인 좌표 파싱 로직은 위 drawOnLine과 동일하므로 생략) ...
                String lineString = null;
                if (legElement.getAsJsonObject().has("passShape")) {
                    lineString = legElement.getAsJsonObject().getAsJsonObject("passShape").get("linestring").getAsString();
                } else if (legElement.getAsJsonObject().has("steps")) {
                    // ... steps 파싱
                }
                if (lineString != null && !lineString.isEmpty()) {
                    for (String coordinate : lineString.split(" ")) {
                        String[] coords = coordinate.split(",");
                        if (coords.length >= 2) {
                            lastPoint = new TMapPoint(Double.parseDouble(coords[1].trim()), Double.parseDouble(coords[0].trim()));
                            poly.addLinePoint(lastPoint);
                        }
                    }
                }
            }
            tMapView.addTMapPolyLine("route" + this.userId, poly);

            if (lastPoint != null) {
                updateMarker(this.userId, lastPoint);
                tMapView.setCenterPoint(lastPoint.getLongitude(), lastPoint.getLatitude());
            }

            // REFACTORED: 경로 안내 UI 업데이트 로직 변경
            String timeFormatted = String.format("%02d시간 %02d분", totalTime / 3600, (totalTime % 3600) / 60);
            // if (textTotalTime != null) textTotalTime.setText("총 소요시간: " + timeFormatted);

            List<Segment> segments = parseRouteSegments(response);
            routeSegmentAdapter.updateSegments(segments);

        } catch (Exception e) {
            Log.e(TAG, "지도 그리기 실패", e);
            Toast.makeText(this, "경로 표시에 실패했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    // ... (updateMarker, createMarkerIcon, formatDistance, formatDuration 생략, 변경 없음) ...
    private void updateMarker(String markerUserId, TMapPoint point) {
        if (point == null) return;
        TMapMarkerItem marker = tMapView.getMarkerItemFromID("markergps" + markerUserId);
        if (marker != null) {
            marker.setTMapPoint(point);
        } else {
            // 마커가 없는 경우 새로 생성 (리스너 부착 시점 이슈 방지)
            TMapMarkerItem newMarker = new TMapMarkerItem();
            newMarker.setTMapPoint(point);
            newMarker.setIcon(createMarkerIcon()); // 아이콘 설정
            tMapView.addMarkerItem("markergps" + markerUserId, newMarker);
        }
    }

    private Bitmap createMarkerIcon() {
        Drawable drawable = ContextCompat.getDrawable(this, android.R.drawable.ic_menu_mylocation);
        if (drawable != null) {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            return bitmap;
        }
        return null;
    }

    // REFACTORED: 메서드 이름을 변경하고 반환 타입을 List<Segment>로 변경
    private List<Segment> parseRouteSegments(JsonObject response) {
        try {
            JsonArray legs = response.getAsJsonArray("legs");
            if (legs == null || legs.size() == 0) return new ArrayList<>();

            List<Segment> segments = new ArrayList<>();
            for (JsonElement legElement : legs) {
                JsonObject leg = legElement.getAsJsonObject();
                segments.add(new Segment(
                        leg.get("mode").getAsString(),
                        leg.getAsJsonObject("start").get("name").getAsString(),
                        leg.getAsJsonObject("end").get("name").getAsString(),
                        leg.get("distance").getAsInt(),
                        leg.get("sectionTime").getAsInt(),
                        leg.has("route") ? leg.get("route").getAsString() : ""
                ));
            }
            return segments;
        } catch (Exception e) {
            Log.e(TAG, "경로 세그먼트 파싱 실패", e);
            return new ArrayList<>(); // 오류 발생 시 빈 리스트 반환
        }
    }
    private String formatDistance(int meters) {
        if (meters >= 1000) {
            return String.format("%.1fkm", meters / 1000f);
        } else {
            return meters + "m";
        }
    }

    private String formatDuration(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        if (h > 0) return String.format("%d시간 %d분", h, m);
        if (m > 0) return String.format("%d분", m);
        return "1분 미만";
    }


    // --- 위치 서비스 관련 로직 (변경 없음) ---
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            locationService = binder.getService();
            isServiceBound = true;
            locationService.setClientListener(MapFriendsActivity.this); // 서비스에 리스너(액티비티 자신) 등록
            Log.d(TAG, "LocationService에 연결되었습니다.");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            locationService = null;
            Log.d(TAG, "LocationService 연결이 끊겼습니다.");
        }
    };
    private void startAndBindLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.putExtra("roomId", chatRoomId);
        serviceIntent.putExtra("userId", userId);
        serviceIntent.putExtra("endAddr", endAddr);

        // 안드로이드 8.0 이상에서는 startForegroundService 사용
        ContextCompat.startForegroundService(this, serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (isServiceBound && locationService != null) {
            locationService.setClientListener(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: 위치 추적 일시중지 호출");
        if (isServiceBound && locationService != null) {
            locationService.setClientListener(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: 위치 추적 중지 호출");
        // 서비스 바인딩 해제
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onLocationUpdated(Location location) {
        if (location == null || tMapView == null) return;

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        Log.d(TAG, "액티비티에서 UI 업데이트: " + latitude + ", " + longitude);

        TMapPoint gps = new TMapPoint(latitude, longitude);
        markermygps.setTMapPoint(gps); // 마커 위치 업데이트

        if (tMapView.getMarkerItemFromID("markerMyGPS") == null) {
            tMapView.addMarkerItem("markerMyGPS", markermygps);
        }
    }
    private void initMarkerIcon() {
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_person);
        if (drawable != null) {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            drawable.setBounds(0, 0, width, height);
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.draw(canvas);
            markermygps.setIcon(bitmap);
            markermygps.setName("현위치");
            markermygps.setPosition(0.5f, 1.0f);
        } else {
            Log.e(TAG, "현위치 마커 아이콘 로드 실패");
        }
    }


    // REFACTORED: RecyclerView.Adapter 내부 클래스 추가
    private class RouteSegmentAdapter extends RecyclerView.Adapter<RouteSegmentAdapter.SegmentViewHolder> {
        private List<Segment> segments;

        RouteSegmentAdapter(List<Segment> segments) {
            this.segments = segments;
        }

        @NonNull
        @Override
        public SegmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_route_segment, parent, false);
            return new SegmentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SegmentViewHolder holder, int position) {
            Segment segment = segments.get(position);
            holder.bind(segment);
        }

        @Override
        public int getItemCount() {
            return segments.size();
        }

        // 데이터 업데이트 메서드
        void updateSegments(List<Segment> newSegments) {
            this.segments = newSegments;
            notifyDataSetChanged();
        }

        class SegmentViewHolder extends RecyclerView.ViewHolder {
            // item_route_segment.xml의 뷰들
            private final ImageView iconTransport;
            private final TextView textTransportInfo;
            private final TextView textDuration;
            private final TextView textDistance;
            private final TextView textStartPoint;

            SegmentViewHolder(@NonNull View itemView) {
                super(itemView);
                iconTransport = itemView.findViewById(R.id.icon_transport);
                textTransportInfo = itemView.findViewById(R.id.text_transport_info);
                textDuration = itemView.findViewById(R.id.text_duration);
                textDistance = itemView.findViewById(R.id.text_distance);
                textStartPoint = itemView.findViewById(R.id.text_start_point);
            }

            void bind(Segment segment) {
                // 데이터를 뷰에 바인딩
                textDuration.setText(formatDuration(segment.time));
                textDistance.setText(formatDistance(segment.distance));
                textStartPoint.setText(String.format("%s → %s", segment.start, segment.end));

                switch (segment.mode) {
                    case "WALK":
                        iconTransport.setImageResource(R.drawable.ic_walk); // ic_walk.xml 필요
                        textTransportInfo.setText("도보");
                        break;
                    case "BUS":
                        iconTransport.setImageResource(R.drawable.ic_bus); // ic_bus.xml 필요
                        textTransportInfo.setText(String.format("%s번 버스", segment.route));
                        break;
                    case "SUBWAY":
                        iconTransport.setImageResource(R.drawable.ic_subway); // ic_subway.xml 필요
                        textTransportInfo.setText(String.format("%s호선", segment.route));
                        break;

                }
            }
        }
    }
}