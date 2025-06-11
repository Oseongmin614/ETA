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

    private DatabaseReference mDatabase;
    private String currentUserId;
    private String chatRoomId;
    private String nickname;
    private String roomName;

    private boolean isQuickMenuVisible = false;
    private Handler timeHandler = new Handler();
    private Runnable timeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        getIntentData();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        initializeChatUI();
        loadMessages();
        sendJoinMessage();
        registerParticipant(); // ✅ 입장 시 참여자 등록
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(roomName != null ? roomName : "채팅방");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        btnQuickMenu = findViewById(R.id.btn_quick_menu);
        layoutQuickMenu = findViewById(R.id.layout_quick_menu);
        textCurrentTime = findViewById(R.id.text_current_time);

        messageInput.setTextColor(getResources().getColor(R.color.text_primary));
        messageInput.setHintTextColor(getResources().getColor(R.color.text_secondary));
        sendButton.setBackgroundColor(getResources().getColor(R.color.button_primary));
        sendButton.setTextColor(getResources().getColor(R.color.text_primary));

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messageList, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);
        recyclerView.setBackgroundColor(getResources().getColor(R.color.background_color));

        setupClickListeners();
    }

    private void setupClickListeners() {
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageInput.setText("");
            }
        });

        btnQuickMenu.setOnClickListener(v -> toggleQuickMenu());

        messageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && isQuickMenuVisible) {
                hideQuickMenu();
            }
        });

        setupQuickMenuButtons();
    }

    private void setupQuickMenuButtons() {
        LinearLayout menuAlarm = findViewById(R.id.menu_alarm);
        menuAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(this, AlarmActivity.class);
            intent.putExtra("nickname", nickname);
            intent.putExtra("userId", currentUserId);
            intent.putExtra("chatRoomId", chatRoomId);
            intent.putExtra("roomName", roomName);
            startActivity(intent);
            hideQuickMenu();
        });

        LinearLayout menuMap = findViewById(R.id.menu_map);
        menuMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapActivity.class);
            intent.putExtra("nickname", nickname);
            intent.putExtra("userId", currentUserId);
            intent.putExtra("roomId", chatRoomId);
            startActivity(intent);
            hideQuickMenu();
        });

        LinearLayout menuShare = findViewById(R.id.menu_share);
        menuShare.setOnClickListener(v -> {
            Toast.makeText(this, "공유 기능은 개발 중입니다", Toast.LENGTH_SHORT).show();
            hideQuickMenu();
        });

        LinearLayout menuDeparture = findViewById(R.id.menu_departure);
        menuDeparture.setOnClickListener(v -> {
            Toast.makeText(this, "출발 기능은 개발 중입니다", Toast.LENGTH_SHORT).show();
            hideQuickMenu();
        });

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
        hideKeyboard();
        layoutQuickMenu.setVisibility(View.VISIBLE);
        isQuickMenuVisible = true;
        btnQuickMenu.setText("×");
    }

    private void hideQuickMenu() {
        layoutQuickMenu.setVisibility(View.GONE);
        isQuickMenuVisible = false;
        btnQuickMenu.setText("+");
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void startClock() {
        timeRunnable = () -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String currentTime = sdf.format(new Date());
            textCurrentTime.setText(currentTime);
            timeHandler.postDelayed(timeRunnable, 1000);
        };
        timeHandler.post(timeRunnable);
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

                    @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                    @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                    @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                    @Override public void onCancelled(@NonNull DatabaseError error) {
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
                            Toast.makeText(ChatActivity.this, "메시지 전송 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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

    private void sendLeaveMessage() {
        ChatMessage leaveMessage = new ChatMessage(
                "system",
                "",
                nickname + "님이 채팅에서 퇴장하였습니다.",
                System.currentTimeMillis(),
                ChatMessage.TYPE_SYSTEM
        );

        String messageId = mDatabase.child("chats").child(chatRoomId).child("messages").push().getKey();
        if (messageId != null) {
            mDatabase.child("chats").child(chatRoomId).child("messages").child(messageId).setValue(leaveMessage);
        }
    }

    private void registerParticipant() {
        mDatabase.child("chats").child(chatRoomId).child("participants")
                .child(currentUserId).setValue(nickname);
    }

    private void unregisterParticipant() {
        mDatabase.child("chats").child(chatRoomId).child("participants")
                .child(currentUserId).removeValue();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timeHandler != null && timeRunnable != null) {
            timeHandler.removeCallbacks(timeRunnable);
        }
        sendLeaveMessage();      // ✅ 퇴장 메시지
        unregisterParticipant(); // ✅ 실시간 참여자 제거
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