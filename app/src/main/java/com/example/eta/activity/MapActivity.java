package com.example.eta.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eta.R;
import com.example.eta.service.NerPointExtractor;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";

    // UI 컴포넌트
    private Button buttonSelectDestination;
    private Button buttonSelectStart;
    private Button buttonStartRoute;
    private Button buttonFriends;
    private TextView textDestination;
    private TextView textStartLocation;

    // 데이터
    private String endAddr;
    private String endStr;
    private String startAddr;
    private String nickname;
    private String userId;
    private String chatRoomId;
    private DatabaseReference mDatabase;
    NerPointExtractor extractor;


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
                            String messageId = mDatabase.child("chatRooms").child(chatRoomId).child("endPoint").push().getKey();
                            if (messageId != null) {
                                mDatabase.child("chatRooms").child(chatRoomId).child("endPoint").setValue(endAddr)
                                        .addOnFailureListener(e ->
                                                Toast.makeText(MapActivity.this, "메시지 전송 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                        );
                            }
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
        mDatabase = FirebaseDatabase.getInstance().getReference();
        // 액션바 설정
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("길찾기");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        getIntentData();
        initViews();
        setupClickListeners();
        setEndAddr();
    }

    private void setEndAddr() {
        extractor = new NerPointExtractor();
        if (endStr != null) {
            extractor.getFirstPOICoordinates(endStr , new NerPointExtractor.TMapSearchCallback() {
                @Override
                public void onSuccess(String coordinates) {
                    // UI 스레드에서 UI 업데이트
                    runOnUiThread(() -> {
                        endAddr = coordinates;
                        textDestination.setText(endStr);
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    runOnUiThread(() -> {
                        Log.e("MainActivity", "실패: " + errorMessage);
                    });
                }
            });
        }
    }

    private void getIntentData() {
        nickname = getIntent().getStringExtra("nickname");
        userId = getIntent().getStringExtra("userId");
        chatRoomId = getIntent().getStringExtra("roomId");
        endStr = getIntent().getStringExtra("endAdder");
    }

    private void initViews() {
        buttonSelectDestination = findViewById(R.id.button_select_destination);
        buttonSelectStart = findViewById(R.id.button_select_start);
        buttonStartRoute = findViewById(R.id.button_start_route);
        buttonFriends = findViewById(R.id.button_friends);
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


        buttonFriends.setBackgroundColor(getResources().getColor(R.color.button_secondary));
        buttonFriends.setTextColor(getResources().getColor(R.color.text_primary));
        buttonFriends.setEnabled(true);

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
            endPointNaming();

        });

        // 친구 위치 보기 버튼
        buttonFriends.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapFriendsActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("roomId", chatRoomId);
            startActivity(intent);
        });
    }
    private void endPointNaming() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("목적지를 뭐라고 부를까요?");

        final EditText input = new EditText(this);
        input.setHint("약속 장소의 이름을 알려주세요");
        input.setTextColor(getResources().getColor(R.color.text_primary));
        input.setHintTextColor(getResources().getColor(R.color.text_secondary));
        input.setBackgroundColor(getResources().getColor(R.color.surface_color));
        builder.setView(input);

        builder.setPositiveButton("확인", (dialog, which) -> {
            String endName = input.getText().toString().trim();
            if (!endName.isEmpty()) {
                String messageId = mDatabase.child("chatRooms").child(chatRoomId).child("endName").push().getKey();
                if (messageId != null) {
                    mDatabase.child("chatRooms").child(chatRoomId).child("endName").setValue(endName)
                            .addOnFailureListener(e ->
                                    Toast.makeText(MapActivity.this, "메시지 전송 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                }
                if (startAddr != null && endAddr != null) {
                    Intent intent = new Intent(this, MapRouteActivity.class);
                    intent.putExtra("start", startAddr);
                    intent.putExtra("end", endAddr);
                    intent.putExtra("nickname", nickname);
                    intent.putExtra("userId", userId);
                    intent.putExtra("roomId", chatRoomId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "출발지와 목적지를 모두 선택해주세요", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "채팅방 이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(R.color.surface_color);
        alertDialog.show();
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