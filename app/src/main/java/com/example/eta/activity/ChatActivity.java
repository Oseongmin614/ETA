package com.example.eta.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eta.R;
import com.example.eta.adapter.ChatAdapter;
import com.example.eta.model.ChatMessage;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText messageInput;
    private Button sendButton;
    private Button btnQuickMenu;
    private LinearLayout layoutQuickMenu;
    private TextView textCurrentTime;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;

    // Firebase 관련 변수
    private DatabaseReference mDatabase;
    private String currentUserId;
    private String chatRoomId;
    private String nickname;
    private String roomName;

    // 퀵메뉴 표시 상태
    private boolean isQuickMenuVisible = false;

    // 시계 업데이트용 핸들러
    private Handler timeHandler = new Handler();
    private Runnable timeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Intent에서 데이터 받기
        getIntentData();

        // Firebase 초기화
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // UI 초기화
        initializeChatUI();

        // 메시지 로드
        loadMessages();

        // 입장 메시지 전송
        sendJoinMessage();

        // 실시간 시계 시작
        startClock();
    }

    private void getIntentData() {
        nickname = getIntent().getStringExtra("nickname");
        currentUserId = getIntent().getStringExtra("userId");
        chatRoomId = getIntent().getStringExtra("roomId");
        roomName = getIntent().getStringExtra("roomName");

        if (nickname == null || currentUserId == null || chatRoomId == null) {
            Toast.makeText(this, "채팅방 정보가 없습니다", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeChatUI() {
        // 액션바 설정
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(roomName != null ? roomName : "채팅방");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // 뷰 초기화
        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        btnQuickMenu = findViewById(R.id.btn_quick_menu);
        layoutQuickMenu = findViewById(R.id.layout_quick_menu);
        textCurrentTime = findViewById(R.id.text_current_time);

        // 다크 테마 UI 설정
        messageInput.setTextColor(getResources().getColor(R.color.text_primary));
        messageInput.setHintTextColor(getResources().getColor(R.color.text_secondary));
        sendButton.setBackgroundColor(getResources().getColor(R.color.button_primary));
        sendButton.setTextColor(getResources().getColor(R.color.text_primary));

        // RecyclerView 설정
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messageList, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);
        recyclerView.setBackgroundColor(getResources().getColor(R.color.background_color));

        // 클릭 리스너 설정
        setupClickListeners();
    }

    private void setupClickListeners() {
        // 전송 버튼 클릭 리스너
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageInput.setText("");
            }
        });

        // 퀵메뉴 토글 버튼
        btnQuickMenu.setOnClickListener(v -> toggleQuickMenu());

        // 메시지 입력창 포커스 리스너
        messageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && isQuickMenuVisible) {
                hideQuickMenu();
            }
        });

        // 퀵메뉴 버튼들
        setupQuickMenuButtons();
    }

    private void setupQuickMenuButtons() {
        // 알람 버튼 (AlarmActivity 실행)
        LinearLayout menuAlarm = findViewById(R.id.menu_alarm);
        menuAlarm.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, AlarmActivity.class);
                intent.putExtra("nickname", nickname);
                intent.putExtra("userId", currentUserId);
                intent.putExtra("chatRoomId", chatRoomId);
                intent.putExtra("roomName", roomName);
                startActivity(intent);
                hideQuickMenu();
                Toast.makeText(this, "알람을 실행합니다", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "알람 실행 중 오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // 지도 버튼 (MapActivity 실행)
        LinearLayout menuMap = findViewById(R.id.menu_map);
        menuMap.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(this, MapActivity.class);
                intent.putExtra("nickname", nickname);
                intent.putExtra("userId", currentUserId);
                intent.putExtra("roomId", chatRoomId);
                startActivity(intent);
                hideQuickMenu();
                Toast.makeText(this, "지도를 실행합니다", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "지도 실행 중 오류가 발생했습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // 공유 버튼 (아이콘만)
        LinearLayout menuShare = findViewById(R.id.menu_share);
        menuShare.setOnClickListener(v -> {
            Toast.makeText(this, "공유 기능은 개발 중입니다", Toast.LENGTH_SHORT).show();
            hideQuickMenu();
        });

        // 출발 버튼 (아이콘만)
        LinearLayout menuDeparture = findViewById(R.id.menu_departure);
        menuDeparture.setOnClickListener(v -> {
            Toast.makeText(this, "출발 기능은 개발 중입니다", Toast.LENGTH_SHORT).show();
            hideQuickMenu();
        });

        // 친구위치 버튼 (아이콘만)
        LinearLayout menuMapFriends = findViewById(R.id.menu_mapFriends);
        menuMapFriends.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapFriendsActivity.class);
            startActivity(intent);
            hideQuickMenu();
        });
    }

    private void toggleQuickMenu() {
        if (isQuickMenuVisible) {
            hideQuickMenu();
        } else {
            showQuickMenu();
        }
    }

    private void showQuickMenu() {
        // 키보드 숨기기
        hideKeyboard();

        // 퀵메뉴 보이기
        layoutQuickMenu.setVisibility(View.VISIBLE);
        isQuickMenuVisible = true;

        // + 버튼을 X로 변경
        btnQuickMenu.setText("×");
    }

    private void hideQuickMenu() {
        // 퀵메뉴 숨기기
        layoutQuickMenu.setVisibility(View.GONE);
        isQuickMenuVisible = false;

        // + 버튼 원래대로
        btnQuickMenu.setText("+");
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void startClock() {
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                String currentTime = sdf.format(new Date());
                textCurrentTime.setText(currentTime);

                // 1초마다 업데이트
                timeHandler.postDelayed(this, 1000);
            }
        };
        timeHandler.post(timeRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 핸들러 정리
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
    }

    private void loadMessages() {
        mDatabase.child("chats").child(chatRoomId).child("messages")
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        ChatMessage message = snapshot.getValue(ChatMessage.class);
                        if (message != null) {
                            messageList.add(message);
                            chatAdapter.notifyItemInserted(messageList.size() - 1);
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChatActivity.this, "메시지 로드 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendMessage(String messageText) {
        ChatMessage chatMessage = new ChatMessage(
                currentUserId,
                nickname,
                messageText,
                System.currentTimeMillis()
        );

        String messageId = mDatabase.child("chats").child(chatRoomId).child("messages").push().getKey();
        if (messageId != null) {
            mDatabase.child("chats").child(chatRoomId).child("messages").child(messageId).setValue(chatMessage)
                    .addOnFailureListener(e ->
                            Toast.makeText(ChatActivity.this, "메시지 전송 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void sendJoinMessage() {
        ChatMessage joinMessage = new ChatMessage(
                "system",
                "",
                nickname + "님이 채팅에 참여했습니다.",
                System.currentTimeMillis(),
                ChatMessage.TYPE_SYSTEM
        );

        String messageId = mDatabase.child("chats").child(chatRoomId).child("messages").push().getKey();
        if (messageId != null) {
            mDatabase.child("chats").child(chatRoomId).child("messages").child(messageId).setValue(joinMessage);
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
    public void onBackPressed() {
        if (isQuickMenuVisible) {
            hideQuickMenu();
        } else {
            super.onBackPressed();
        }
    }
}