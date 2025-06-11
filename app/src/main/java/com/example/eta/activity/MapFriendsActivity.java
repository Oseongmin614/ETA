package com.example.eta.activity;

import static android.graphics.Color.RED;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.eta.R;
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

public class MapFriendsActivity extends AppCompatActivity {
    private static final String TAG = "MapFriendsActivity";

    // UI Components
    private FrameLayout mapContainer;
    private TextView textRouteInfo;
    private TMapView tMapView;

    // Data
    private String userId;
    private String chatRoomId;

    // Firebase
    private DatabaseReference mDatabase;
    private DatabaseReference chatRoomRef;

    // Map & Color Management
    private final List<Integer> friendColors = new ArrayList<>();
    private final Map<String, Integer> userColorMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_map_friends);

        initViews();
        initTMap();
        initFriendColors();
        getIntentData();
        initFirebase();

        loadAllRoutesAndInitialGps();
        attachGpsListener();
    }

    private void initTMap() {
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(Keyholder.getAppKey());
        mapContainer.addView(tMapView);
        tMapView.setZoomLevel(15);
    }

    private void initViews () {
            mapContainer = findViewById(R.id.tmapcon);
            textRouteInfo = findViewById(R.id.text_route_info);
            textRouteInfo.setTextColor(getResources().getColor(R.color.text_primary));
            textRouteInfo.setBackgroundColor(getResources().getColor(R.color.surface_color));
    }

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

        if (TextUtils.isEmpty(userId) || TextUtils.isEmpty(chatRoomId)) {
            Toast.makeText(this, "사용자 또는 채팅방 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    private void initFirebase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        chatRoomRef = mDatabase.child("chatRooms").child(chatRoomId);
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
            JsonObject itinerary = coordinatesJson.getAsJsonObject("metaData")
                    .getAsJsonObject("plan")
                    .getAsJsonArray("itineraries")
                    .get(0)
                    .getAsJsonObject();

            JsonArray legs = itinerary.getAsJsonArray("legs");

            for (JsonElement legElement : legs) {
                JsonObject leg = legElement.getAsJsonObject();
                if (leg.has("passShape")) {
                    String lineString = leg.getAsJsonObject("passShape").get("linestring").getAsString();
                    for (String coordinate : lineString.split(" ")) {
                        String[] coords = coordinate.split(",");
                        if (coords.length >= 2) {
                            poly.addLinePoint(new TMapPoint(Double.parseDouble(coords[1].trim()), Double.parseDouble(coords[0].trim())));
                        }
                    }
                }
            }
            tMapView.addTMapPolyLine("route" + otherUserId, poly);
        } catch (Exception e) {
            Log.e(TAG, "라인 추가 실패 (User: " + otherUserId + ")", e);
        }
    }

    private void drawOnMap(JsonObject response) {
        TMapPolyLine poly = new TMapPolyLine();
        poly.setLineColor(getResources().getColor(R.color.button_primary));
        poly.setLineWidth(8);

        // ... 기존 폴리라인 파싱 및 추가, 정보 텍스트 표시 로직 (동일)
        try {
            JsonObject itinerary = response.getAsJsonObject("metaData")
                    .getAsJsonObject("plan")
                    .getAsJsonArray("itineraries")
                    .get(0)
                    .getAsJsonObject();

            int totalTime = itinerary.get("totalTime").getAsInt();
            JsonArray legs = itinerary.getAsJsonArray("legs");
            TMapPoint lastPoint = null;

            for (JsonElement legElement : legs) {
                JsonObject leg = legElement.getAsJsonObject();
                if (leg.has("passShape")) {
                    String lineString = leg.getAsJsonObject("passShape").get("linestring").getAsString();
                    for (String coordinate : lineString.split(" ")) {
                        String[] coords = coordinate.split(",");
                        if (coords.length >= 2) {
                            double lon = Double.parseDouble(coords[0].trim());
                            double lat = Double.parseDouble(coords[1].trim());
                            TMapPoint point = new TMapPoint(lat, lon);
                            poly.addLinePoint(point);
                            lastPoint = point;
                        }
                    }
                }
            }
            tMapView.addTMapPolyLine("route" + this.userId, poly);

            // 내 마커 추가 및 지도 중심 이동
            if (lastPoint != null) {
                updateMarker(this.userId, lastPoint); // 내 마커 위치 업데이트
                tMapView.setCenterPoint(lastPoint.getLongitude(), lastPoint.getLatitude());
            }

            String timeFormatted = String.format("%02d:%02d:%02d", totalTime / 3600, (totalTime % 3600) / 60, totalTime % 60);
            List<String> instructions = generateTextInstructions(response);
            instructions.add(0, "총 소요 시간: " + timeFormatted);
            textRouteInfo.setText(TextUtils.join("\n", instructions));

        } catch (Exception e) {
            Log.e(TAG, "지도 그리기 실패", e);
            Toast.makeText(this, "경로 표시에 실패했습니다", Toast.LENGTH_SHORT).show();
        }
    }

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
                    case "WALK":
                        transport = "도보";
                        break;
                    case "BUS":
                        transport = "버스(" + seg.route + ")";
                        break;
                    case "SUBWAY":
                        transport = "지하철(" + seg.route + ")";
                        break;
                    default:
                        transport = seg.mode;
                }

                String distance = formatDistance(seg.distance);
                String time = formatDuration(seg.time);
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
        int s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("시간 ");
        if (m > 0) sb.append(m).append("분 ");
        if (s > 0) sb.append(s).append("초");
        return sb.toString().trim();
    }

}