package com.example.eta.activity;

import static android.graphics.Color.RED;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.eta.R;
import com.example.eta.model.JsonReaderR;
import com.example.eta.util.Keyholder;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import java.util.ArrayList;
import java.util.List;

public class MapFriendsActivity extends AppCompatActivity {
    private FrameLayout mapContainer;
    private TextView textRouteInfo;
    private TMapView tMapView;
    private static final String TAG = "RouteMapFriendsActivity";
    private DatabaseReference mDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_route_map_friends);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            mDatabase = FirebaseDatabase.getInstance().getReference();
            JsonReaderR jr = new JsonReaderR();
            String jsonStr= jr.loadJSONFromAsset(this, "route2.json");
            JsonObject response = JsonParser.parseString(jsonStr).getAsJsonObject();
            initViews();
            initTMap();

            String jsonStr2= jr.loadJSONFromAsset(this, "reute3.json");
            JsonObject response2 = JsonParser.parseString(jsonStr2).getAsJsonObject();
            TMapPoint gps = new TMapPoint(37.50821164293811, 127.1030613629431);
            drawOnLine(response2, gps, "a");


            drawOnMap(response);
            return insets;
        });


    }
    private void initTMap() {
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(Keyholder.getAppKey());
        mapContainer.addView(tMapView);
    }
    private void initViews() {
        mapContainer = findViewById(R.id.tmapcon);
        textRouteInfo = findViewById(R.id.text_route_info);

        // 다크 테마 적용
        textRouteInfo.setTextColor(getResources().getColor(R.color.text_primary));
        textRouteInfo.setBackgroundColor(getResources().getColor(R.color.surface_color));
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

    private void drawOnLine(JsonObject response, TMapPoint gps, String name) {
        TMapMarkerItem markergps = new TMapMarkerItem();
        markergps.setTMapPoint(gps);
        tMapView.removeMarkerItem("markergps" + name);
        tMapView.addMarkerItem("markergps" + name, markergps);
        // 폴리라인 그리기
        TMapPolyLine poly = new TMapPolyLine();
        poly.setLineColor(RED);
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

            tMapView.addTMapPolyLine("reute"+name, poly);

        } catch (Exception e) {
            Log.e(TAG, "라인 추가 실패", e);
            Toast.makeText(this, "라인 표시에 실패했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void drawOnMap(JsonObject response) {
        double friendPointLat = 37.51163599542048;
        double friendPointLon = 127.08545246258772;
        TMapPoint gps = new TMapPoint(friendPointLat, friendPointLon);
        TMapMarkerItem markergps = new TMapMarkerItem();
        markergps.setTMapPoint(gps);
        tMapView.removeMarkerItem("markergps");
        tMapView.addMarkerItem("markergps", markergps);
        tMapView.setCenterPoint(friendPointLon, friendPointLat);
        // 폴리라인 그리기
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

            // 시간 포맷팅
            String timeFormatted = String.format("%02d:%02d:%02d",
                    totalTime / 3600, (totalTime % 3600) / 60, totalTime % 60);

            // 경로 안내 텍스트 생성
            List<String> instructions = generateTextInstructions(response);
            instructions.add(0, "총 소요 시간: " + timeFormatted);

            textRouteInfo.setText(TextUtils.join("\n", instructions));

        } catch (Exception e) {
            Log.e(TAG, "지도 그리기 실패", e);
            Toast.makeText(this, "경로 표시에 실패했습니다", Toast.LENGTH_SHORT).show();
        }
    }

    private TMapMarkerItem createMarker(TMapPoint point, String title) {
        TMapMarkerItem marker = new TMapMarkerItem();
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
        return marker;
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