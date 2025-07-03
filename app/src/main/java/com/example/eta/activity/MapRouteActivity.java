package com.example.eta.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eta.R;
import com.example.eta.service.MapDBInsertService;
import com.example.eta.util.Keyholder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class MapRouteActivity extends AppCompatActivity {

    private static final String TAG = "RouteMapActivity";
    private static final String ROUTE_API_BASE = "https://apis.openapi.sk.com";

    // UI 컴포넌트
    private FrameLayout mapContainer;
    private TMapView tMapView;
    // REFACTORED: 경로 선택 UI 추가
    private RecyclerView routeRecyclerView;
    private Button confirmRouteButton;
    private LinearLayout routeSelectionLayout;
    private RouteAdapter routeAdapter;

    // 데이터
    private String startAddr;
    private String endAddr;
    private String nickname;
    private String userId;
    private String chatRoomId;
    private TMapPoint startPoint;
    private TMapPoint endPoint;

    // REFACTORED: 다중 경로 데이터 관리
    private JsonArray allItineraries;
    private int selectedItineraryIndex = -1;
    private JsonObject fullRouteResponse;

    // 네트워크
    private RouteService routeService;
    private MapDBInsertService mapDBInsertService;

    // Retrofit 인터페이스
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
        mapDBInsertService = new MapDBInsertService();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("경로 선택");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        setupRecyclerView();
        initTMapAndRetrofit();
        getIntentData();

        if (startAddr != null && endAddr != null) {
            calculateRoute();
        }
    }

    private void initViews() {
        mapContainer = findViewById(R.id.tmapcon);
        // REFACTORED: 새로운 UI 컴포넌트 초기화
        routeSelectionLayout = findViewById(R.id.route_selection_layout); // 이 ID로 레이아웃을 감싸야 함
        routeRecyclerView = findViewById(R.id.recycler_view_routes); // XML에 추가된 RecyclerView ID
        confirmRouteButton = findViewById(R.id.btn_confirm_route); // XML에 추가된 Button ID

        confirmRouteButton.setText("이 경로로 결정");
        confirmRouteButton.setVisibility(View.GONE); // 초기에는 숨김
        confirmRouteButton.setOnClickListener(v -> onConfirmRoute());
    }

    private void setupRecyclerView() {
        routeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        routeAdapter = new RouteAdapter(new ArrayList<>(), this::onRouteSelected);
        routeRecyclerView.setAdapter(routeAdapter);
    }

    private void initTMapAndRetrofit() {
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(Keyholder.getAppKey());
        tMapView.setZoomLevel(15);
        mapContainer.addView(tMapView);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ROUTE_API_BASE)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        routeService = retrofit.create(RouteService.class);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        endAddr = intent.getStringExtra("end");
        startAddr = intent.getStringExtra("start");
        nickname = intent.getStringExtra("nickname");
        userId = intent.getStringExtra("userId");
        chatRoomId = intent.getStringExtra("roomId");
    }

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
            startPoint = new TMapPoint(startLat, startLon);
            endPoint = new TMapPoint(endLat, endLon);
            requestRoute(startLon, startLat, endLon, endLat);
        } catch (NumberFormatException e) {
            Log.e(TAG, "좌표 파싱 실패", e);
            Toast.makeText(this, "좌표 형식이 잘못되었습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestRoute(double startLon, double startLat, double endLon, double endLat) {
        JsonObject body = new JsonObject();
        body.addProperty("startX", String.valueOf(startLon));
        body.addProperty("startY", String.valueOf(startLat));
        body.addProperty("endX", String.valueOf(endLon));
        body.addProperty("endY", String.valueOf(endLat));
        body.addProperty("count", 5); // 여러 경로를 받기 위해 count 증가 (최대 5)
        body.addProperty("searchOption", "4"); // 추천 + 버스 + 지하철 경로 모두 탐색
        body.addProperty("lang", 0);
        body.addProperty("format", "json");

        routeService.getRoute(Keyholder.getAppKey(), body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fullRouteResponse = response.body(); // 전체 응답 저장
                    // REFACTORED: 첫번째 경로를 바로 그리지 않고, 모든 경로를 목록으로 표시
                    displayRouteOptions(fullRouteResponse);
                } else {
                    Log.e(TAG, "Route API 실패: " + response.code() + " " + response.message());
                    Toast.makeText(MapRouteActivity.this, "경로 계산에 실패했습니다", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "Route API 오류: " + t.getMessage());
                Toast.makeText(MapRouteActivity.this, "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // REFACTORED: 경로 옵션들을 RecyclerView에 표시하는 메소드
    private void displayRouteOptions(JsonObject response) {
        try {
            allItineraries = response.getAsJsonObject("metaData")
                    .getAsJsonObject("plan")
                    .getAsJsonArray("itineraries");

            if (allItineraries == null || allItineraries.size() == 0) {
                Toast.makeText(this, "이용 가능한 경로가 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 출발, 도착 마커는 초기에 한 번만 그림
            tMapView.addMarkerItem("start", createMarker(startPoint, "출발지", android.R.drawable.ic_menu_mylocation)); // 커스텀 아이콘 사용 권장
            tMapView.addMarkerItem("end", createMarker(endPoint, "도착지", android.R.drawable.ic_menu_mylocation));
            tMapView.setCenterPoint(startPoint.getLongitude(), startPoint.getLatitude());

            List<JsonObject> routeList = new ArrayList<>();
            for (JsonElement element : allItineraries) {
                routeList.add(element.getAsJsonObject());
            }
            routeAdapter.updateRoutes(routeList);
            routeSelectionLayout.setVisibility(View.VISIBLE);

            // 첫 번째 경로를 기본으로 선택하여 지도에 표시
            onRouteSelected(0);

        } catch (Exception e) {
            Log.e(TAG, "경로 옵션 표시 실패", e);
            Toast.makeText(this, "경로 정보를 처리할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // REFACTORED: 사용자가 경로를 선택했을 때 호출되는 콜백
    private void onRouteSelected(int position) {
        this.selectedItineraryIndex = position;
        routeAdapter.setSelectedPosition(position);
        drawOnMap(position); // 선택된 경로를 지도에 그림
        confirmRouteButton.setVisibility(View.VISIBLE); // 경로 선택 시 버튼 표시
    }

    // REFACTORED: 최종 경로 선택 시 DB 저장 로직
    private void onConfirmRoute() {
        if (selectedItineraryIndex == -1 || fullRouteResponse == null) {
            Toast.makeText(this, "경로를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 선택된 경로 정보만 String으로 변환하여 저장
        JsonObject selectedItinerary = allItineraries.get(selectedItineraryIndex).getAsJsonObject();
        mapDBInsertService.insert(chatRoomId, userId, selectedItinerary.toString());

        int totalTime = selectedItinerary.get("totalTime").getAsInt();
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                totalTime / 3600, (totalTime % 3600) / 60, totalTime % 60);
        mapDBInsertService.insertTime(chatRoomId, userId, timeFormatted);

        Toast.makeText(this, (selectedItineraryIndex + 1) + "번 경로가 최종 선택되었습니다.", Toast.LENGTH_SHORT).show();
        // 필요 시, 이전 액티비티로 돌아가거나 추가 작업 수행
        finish();
    }


    // REFACTORED: 특정 인덱스의 경로를 지도에 그리는 메소드. 이동 수단별 색상 구분 적용.
    private void drawOnMap(int itineraryIndex) {
        tMapView.removeAllTMapPolyLine(); // 기존에 그려진 폴리라인 모두 제거

        if (allItineraries == null || allItineraries.size() <= itineraryIndex) return;

        try {
            JsonObject itinerary = allItineraries.get(itineraryIndex).getAsJsonObject();
            JsonArray legs = itinerary.getAsJsonArray("legs");

            int legIndex = 0;
            for (JsonElement legElement : legs) {
                JsonObject leg = legElement.getAsJsonObject();
                TMapPolyLine polyLine = new TMapPolyLine();
                polyLine.setLineWidth(10); // 선 굵기 증가

                // 이동 수단(mode)에 따라 색상 설정
                switch (leg.get("mode").getAsString()) {
                    case "WALK":
                        polyLine.setLineColor(ContextCompat.getColor(this, R.color.walk_line_color));
                        break;
                    case "BUS":
                        polyLine.setLineColor(ContextCompat.getColor(this, R.color.bus_line_color));
                        break;
                    case "SUBWAY":
                        polyLine.setLineColor(ContextCompat.getColor(this, R.color.subway_line_color));
                        break;
                    default:
                        polyLine.setLineColor(Color.GRAY);
                        break;
                }

                String lineString = null;
                // 경로 좌표 데이터 파싱 (passShape, steps 등 다양한 형태 고려)
                if (leg.has("passShape")) {
                    lineString = leg.getAsJsonObject("passShape").get("linestring").getAsString();
                } else if (leg.has("steps")) {
                    // 도보의 경우 steps 안에 linestring이 있을 수 있음
                    JsonArray steps = leg.getAsJsonArray("steps");
                    StringBuilder combinedLineString = new StringBuilder();
                    for(JsonElement stepElement : steps){
                        JsonObject step = stepElement.getAsJsonObject();
                        if(step.has("linestring")){
                            combinedLineString.append(step.get("linestring").getAsString()).append(" ");
                        }
                    }
                    lineString = combinedLineString.toString().trim();
                }

                if (lineString != null && !lineString.isEmpty()) {
                    for (String coordinate : lineString.split(" ")) {
                        String[] coords = coordinate.split(",");
                        if (coords.length >= 2) {
                            double lon = Double.parseDouble(coords[0].trim());
                            double lat = Double.parseDouble(coords[1].trim());
                            polyLine.addLinePoint(new TMapPoint(lat, lon));
                        }
                    }
                }
                // 각 leg를 별도의 PolyLine으로 지도에 추가 (고유 ID 부여)
                tMapView.addTMapPolyLine("route_leg_" + itineraryIndex + "_" + legIndex++, polyLine);
            }

            // 지도 중심 및 줌 레벨 조정
            tMapView.setCenterPoint(startPoint.getLongitude(), startPoint.getLatitude());

        } catch (Exception e) {
            Log.e(TAG, "지도 그리기 실패", e);
            Toast.makeText(this, "경로 표시에 실패했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private TMapMarkerItem createMarker(TMapPoint point, String title, int iconResId) {
        TMapMarkerItem marker = new TMapMarkerItem();
        Drawable drawable = ContextCompat.getDrawable(this, iconResId);
        if (drawable != null) {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            marker.setIcon(bitmap);
        }
        marker.setTMapPoint(point);
        marker.setName(title);
        marker.setCanShowCallout(true);
        marker.setCalloutTitle(title);
        marker.setPosition(0.5f, 1.0f);
        return marker;
    }

    // 경로 안내 텍스트 생성 (ETA 스타일 유지) - RouteAdapter에서 활용
    private String generateRouteSummary(JsonObject itinerary) {
        try {
            JsonArray legs = itinerary.getAsJsonArray("legs");
            List<String> modes = new ArrayList<>();
            String lastMode = "";

            for (JsonElement legElement : legs) {
                JsonObject leg = legElement.getAsJsonObject();
                String mode = leg.get("mode").getAsString();
                String transport;
                switch (mode) {
                    case "WALK": transport = "도보"; break;
                    case "BUS":
                        transport = leg.has("route") ? leg.get("route").getAsString() + "번 버스" : "버스";
                        break;
                    case "SUBWAY":
                        transport = leg.has("route") ? leg.get("route").getAsString() + "호선" : "지하철";
                        break;
                    default: transport = mode;
                }
                if (!transport.equals(lastMode)) {
                    modes.add(transport);
                    lastMode = transport;
                }
            }
            return TextUtils.join(" → ", modes);
        } catch (Exception e) {
            return "경로 요약 정보 생성 실패";
        }
    }

    private String formatDuration(int seconds) {
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        if (h > 0) {
            return String.format(Locale.getDefault(), "%d시간 %d분", h, m);
        } else {
            return String.format(Locale.getDefault(), "%d분", m);
        }
    }

    // --- RecyclerView Adapter 및 ViewHolder ---
    private class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {
        private List<JsonObject> routes;
        private final OnRouteSelectedListener listener;
        private int selectedPosition = 0; // 기본으로 0번 선택

        RouteAdapter(List<JsonObject> routes, OnRouteSelectedListener listener) {
            this.routes = routes;
            this.listener = listener;
        }

        @NonNull
        @Override
        public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // route_item.xml 레이아웃 파일이 필요합니다.
            // 예: <LinearLayout ...>
            //         <TextView android:id="@+id/text_route_number" />
            //         <TextView android:id="@+id/text_route_duration" />
            //         <TextView android:id="@+id/text_route_summary" />
            //     </LinearLayout>
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.route_item, parent, false);
            return new RouteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
            JsonObject itinerary = routes.get(position);
            holder.bind(itinerary, position);
            holder.itemView.setOnClickListener(v -> listener.onRouteSelected(position));
            // 선택된 아이템 배경색 변경
            holder.itemView.setBackgroundColor(selectedPosition == position ?
                    ContextCompat.getColor(getApplicationContext(), R.color.selected_item_color) :
                    Color.TRANSPARENT);
        }

        @Override
        public int getItemCount() {
            return routes.size();
        }

        void updateRoutes(List<JsonObject> newRoutes) {
            this.routes = newRoutes;
            notifyDataSetChanged();
        }

        void setSelectedPosition(int position) {
            int previousPosition = this.selectedPosition;
            this.selectedPosition = position;
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
        }

        class RouteViewHolder extends RecyclerView.ViewHolder {
            TextView routeNumber, routeDuration, routeSummary;

            RouteViewHolder(@NonNull View itemView) {
                super(itemView);
                routeNumber = itemView.findViewById(R.id.text_route_number);
                routeDuration = itemView.findViewById(R.id.text_route_duration);
                routeSummary = itemView.findViewById(R.id.text_route_summary);
            }

            void bind(JsonObject itinerary, int position) {
                int totalTime = itinerary.get("totalTime").getAsInt();

                routeNumber.setText("경로 " + (position + 1));
                routeDuration.setText("약 " + formatDuration(totalTime));
                routeSummary.setText(generateRouteSummary(itinerary));
            }
        }
    }

    interface OnRouteSelectedListener {
        void onRouteSelected(int position);
    }
}