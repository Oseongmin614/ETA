package com.example.eta.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eta.R;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";

    // UI 컴포넌트
    private Button buttonSelectDestination;
    private Button buttonSelectStart;
    private Button buttonStartRoute;
    private TextView textDestination;
    private TextView textStartLocation;

    // 데이터
    private String endAddr;
    private String startAddr;
    private String nickname;
    private String userId;

    // ActivityResultLauncher들
    private final ActivityResultLauncher<Intent> destinationLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            endAddr = data.getStringExtra("selectedAddr");
                            String locationName = data.getStringExtra("locationName");
                            textDestination.setText(locationName != null ? locationName : "목적지 선택됨");
                            Log.d(TAG, "목적지 받음: " + endAddr);
                            updateRouteButton();
                        }
                    });

    private final ActivityResultLauncher<Intent> startLocationLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            startAddr = data.getStringExtra("selectedAddr");
                            String locationName = data.getStringExtra("locationName");
                            textStartLocation.setText(locationName != null ? locationName : "출발지 선택됨");
                            Log.d(TAG, "출발지 받음: " + startAddr);
                            updateRouteButton();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // 액션바 설정
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("길찾기");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        getIntentData();
        initViews();
        setupClickListeners();
    }

    private void getIntentData() {
        nickname = getIntent().getStringExtra("nickname");
        userId = getIntent().getStringExtra("userId");
    }

    private void initViews() {
        buttonSelectDestination = findViewById(R.id.button_select_destination);
        buttonSelectStart = findViewById(R.id.button_select_start);
        buttonStartRoute = findViewById(R.id.button_start_route);
        textDestination = findViewById(R.id.text_destination);
        textStartLocation = findViewById(R.id.text_start_location);

        // 다크 테마 적용
        buttonSelectDestination.setBackgroundColor(getResources().getColor(R.color.button_primary));
        buttonSelectDestination.setTextColor(getResources().getColor(R.color.text_primary));

        buttonSelectStart.setBackgroundColor(getResources().getColor(R.color.button_primary));
        buttonSelectStart.setTextColor(getResources().getColor(R.color.text_primary));

        buttonStartRoute.setBackgroundColor(getResources().getColor(R.color.button_secondary));
        buttonStartRoute.setTextColor(getResources().getColor(R.color.text_primary));
        buttonStartRoute.setEnabled(false);

        textDestination.setTextColor(getResources().getColor(R.color.text_secondary));
        textStartLocation.setTextColor(getResources().getColor(R.color.text_secondary));
    }

    private void setupClickListeners() {
        // 목적지 선택 버튼
        buttonSelectDestination.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationSearchActivity.class);
            intent.putExtra("searchType", "약속장소는 어디인가요?");
            destinationLauncher.launch(intent);
        });

        // 출발지 선택 버튼
        buttonSelectStart.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationSearchActivity.class);
            intent.putExtra("searchType", "출발장소는 어디인가요?");
            startLocationLauncher.launch(intent);
        });

        // 길찾기 시작 버튼
        buttonStartRoute.setOnClickListener(v -> {
            if (startAddr != null && endAddr != null) {
                Intent intent = new Intent(this, MapRouteActivity.class);
                intent.putExtra("start", startAddr);
                intent.putExtra("end", endAddr);
                intent.putExtra("nickname", nickname);
                intent.putExtra("userId", userId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "출발지와 목적지를 모두 선택해주세요", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRouteButton() {
        boolean canStartRoute = (startAddr != null && endAddr != null);
        buttonStartRoute.setEnabled(canStartRoute);

        if (canStartRoute) {
            buttonStartRoute.setBackgroundColor(getResources().getColor(R.color.button_primary));
        } else {
            buttonStartRoute.setBackgroundColor(getResources().getColor(R.color.button_secondary));
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
}
