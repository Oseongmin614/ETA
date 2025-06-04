package com.example.eta.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eta.R;
import com.example.eta.util.Keyholder;
import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.poi_item.TMapPOIItem;

import java.util.ArrayList;

public class LocationSearchActivity extends AppCompatActivity {

    private static final String TAG = "LocationSearchActivity";

    // UI 컴포넌트
    private ImageButton buttonSearch;
    private EditText editTextLocation;
    private Button buttonConfirm;
    private TMapView tMapView;
    private LinearLayout layoutResults;
    private ScrollView scrollResults;
    private FrameLayout mapContainer;

    // 데이터
    private TMapPoint selectedPoint;
    private String selectedLocationName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_search);

        // 액션바 설정
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("위치 검색");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        initTMap();
        setupClickListeners();
        setupHintText();
    }

    private void initViews() {
        buttonSearch = findViewById(R.id.button_search);
        editTextLocation = findViewById(R.id.edit_text_location);
        buttonConfirm = findViewById(R.id.button_confirm);
        mapContainer = findViewById(R.id.mapContainer2);
        scrollResults = findViewById(R.id.scroll_results);
        layoutResults = findViewById(R.id.layout_results);

        // 다크 테마 적용
        editTextLocation.setTextColor(getResources().getColor(R.color.text_primary));
        editTextLocation.setHintTextColor(getResources().getColor(R.color.text_secondary));
        editTextLocation.setBackgroundColor(getResources().getColor(R.color.surface_color));

        buttonConfirm.setBackgroundColor(getResources().getColor(R.color.button_secondary));
        buttonConfirm.setTextColor(getResources().getColor(R.color.text_primary));
        buttonConfirm.setEnabled(false);

        layoutResults.setBackgroundColor(getResources().getColor(R.color.background_color));
    }

    private void initTMap() {
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(Keyholder.getAppKey());
        mapContainer.addView(tMapView);

        // 서울시청을 기본 중심점으로 설정
        tMapView.setCenterPoint(126.9780, 37.5665);
        tMapView.setZoomLevel(15);
    }

    private void setupClickListeners() {
        buttonSearch.setOnClickListener(v -> {
            String searchText = editTextLocation.getText().toString().trim();
            if (!searchText.isEmpty()) {
                searchPOI(searchText);
            } else {
                Toast.makeText(this, "검색어를 입력해주세요", Toast.LENGTH_SHORT).show();
            }
        });

        buttonConfirm.setOnClickListener(v -> {
            if (selectedPoint != null && selectedLocationName != null) {
                Intent resultIntent = new Intent();
                String coordinates = selectedPoint.getLatitude() + "," + selectedPoint.getLongitude();
                resultIntent.putExtra("selectedAddr", coordinates);
                resultIntent.putExtra("locationName", selectedLocationName);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "위치를 선택해주세요", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupHintText() {
        Intent intent = getIntent();
        String searchType = intent.getStringExtra("searchType");
        if (searchType != null) {
            editTextLocation.setHint(searchType);
        }
    }

    private void searchPOI(String searchText) {
        layoutResults.removeAllViews();

        TMapData tMapData = new TMapData();
        tMapData.findAllPOI(searchText, new TMapData.FindAllPOIListenerCallback() {
            @Override
            public void onFindAllPOI(ArrayList<TMapPOIItem> poiItems) {
                runOnUiThread(() -> {
                    if (poiItems != null && !poiItems.isEmpty()) {
                        for (TMapPOIItem item : poiItems) {
                            Log.d(TAG, "POI: " + item.getPOIName() + ", 주소: " +
                                    item.getPOIAddress().replace("null", "") +
                                    ", 좌표: " + item.getPOIPoint().toString());
                            addPOIResult(item);
                        }
                    } else {
                        showNoResults();
                    }
                });
            }
        });
    }

    private void addPOIResult(TMapPOIItem poi) {
        // POI 이름 텍스트뷰
        TextView nameTextView = new TextView(this);
        nameTextView.setText(poi.getPOIName());
        nameTextView.setTextSize(16);
        nameTextView.setTextColor(getResources().getColor(R.color.text_primary));
        nameTextView.setPadding(16, 12, 16, 4);
        nameTextView.setTag(poi);
        nameTextView.setBackgroundColor(getResources().getColor(R.color.surface_color));
        nameTextView.setOnClickListener(v -> selectPOI((TMapPOIItem) v.getTag()));
        layoutResults.addView(nameTextView);

        // 주소 텍스트뷰
        TextView addressTextView = new TextView(this);
        String address = poi.getPOIAddress().replace("null", "");
        addressTextView.setText(address);
        addressTextView.setTextSize(12);
        addressTextView.setTextColor(getResources().getColor(R.color.text_secondary));
        addressTextView.setPadding(16, 4, 16, 12);
        addressTextView.setBackgroundColor(getResources().getColor(R.color.surface_color));
        layoutResults.addView(addressTextView);

        // 구분선
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2));
        divider.setBackgroundColor(getResources().getColor(R.color.divider_color));
        layoutResults.addView(divider);
    }

    private void selectPOI(TMapPOIItem poi) {
        // 지도에서 기존 마커 제거
        tMapView.removeAllMarkerItem();

        // 선택된 POI 정보 저장
        selectedPoint = poi.getPOIPoint();
        selectedLocationName = poi.getPOIName();

        // 지도에 마커 표시
        double lat = selectedPoint.getLatitude();
        double lon = selectedPoint.getLongitude();

        TMapMarkerItem marker = new TMapMarkerItem();
        marker.setTMapPoint(selectedPoint);
        marker.setName(selectedLocationName);
        marker.setCanShowCallout(true);
        marker.setCalloutTitle("선택된 위치");

        tMapView.addMarkerItem("selected_poi", marker);
        tMapView.setCenterPoint(lon, lat);

        // 확인 버튼 활성화
        buttonConfirm.setEnabled(true);
        buttonConfirm.setBackgroundColor(getResources().getColor(R.color.button_primary));

        // 선택된 항목 하이라이트
        highlightSelectedItem(poi);

        Toast.makeText(this, selectedLocationName + " 선택됨", Toast.LENGTH_SHORT).show();
    }

    private void highlightSelectedItem(TMapPOIItem selectedPOI) {
        for (int i = 0; i < layoutResults.getChildCount(); i++) {
            View child = layoutResults.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                if (textView.getTag() != null && textView.getTag().equals(selectedPOI)) {
                    textView.setBackgroundColor(getResources().getColor(R.color.button_primary));
                } else {
                    textView.setBackgroundColor(getResources().getColor(R.color.surface_color));
                }
            }
        }
    }

    private void showNoResults() {
        TextView noResultTextView = new TextView(this);
        noResultTextView.setText("검색 결과가 없습니다");
        noResultTextView.setTextColor(getResources().getColor(R.color.text_secondary));
        noResultTextView.setPadding(16, 16, 16, 16);
        noResultTextView.setGravity(View.TEXT_ALIGNMENT_CENTER);
        layoutResults.addView(noResultTextView);
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
