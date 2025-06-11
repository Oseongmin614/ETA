package com.example.eta.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.eta.R;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity {

    private TextView textAlarmTime, textAlarmUser;
    private Button btnEndAlarm, btnSnoozeAlarm, btnMoveToManage, btnShowParticipants;

    private String nickname;
    private String currentUserId;
    private String chatRoomId;
    private String roomName;

    private DatabaseReference alarmRequestRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        // 🔹 인텐트에서 값 받기
        nickname = getIntent().getStringExtra("nickname");
        currentUserId = getIntent().getStringExtra("userId");
        chatRoomId = getIntent().getStringExtra("chatRoomId");
        roomName = getIntent().getStringExtra("roomName");

        // 🔹 뷰 초기화
        textAlarmTime = findViewById(R.id.text_alarm_time);
        textAlarmUser = findViewById(R.id.text_alarm_user);
        btnEndAlarm = findViewById(R.id.btn_end_alarm);
        btnSnoozeAlarm = findViewById(R.id.btn_snooze_alarm);
        btnMoveToManage = findViewById(R.id.btn_move_to_manage);
        btnShowParticipants = findViewById(R.id.btn_show_participants);

        // 🔹 현재 시간 표시 (24시간 형식)
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        textAlarmTime.setText(currentTime);

        // 🔹 닉네임 표시
        if (nickname != null && !nickname.isEmpty()) {
            textAlarmUser.setText("보낸 사람: " + nickname);
            observeAlarmRequests(); // 닉네임 있을 때만 Firebase 감시
        }

        // 🔹 버튼 클릭 이벤트 설정
        btnEndAlarm.setOnClickListener(v -> finish());

        btnSnoozeAlarm.setOnClickListener(v -> finish());

        btnMoveToManage.setOnClickListener(v -> {
            Intent intent = new Intent(this, AlarmSetActivity.class);
            startActivity(intent);
        });

        btnShowParticipants.setOnClickListener(v -> {
            Intent intent = new Intent(this, AlarmParticipantListActivity.class);
            intent.putExtra("roomId", chatRoomId);
            intent.putExtra("userId", currentUserId);
            intent.putExtra("nickname", nickname);
            startActivity(intent);
        });
    }

    private void observeAlarmRequests() {
        alarmRequestRef = FirebaseDatabase.getInstance()
                .getReference("alarmRequests")
                .child(nickname);

        alarmRequestRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                String message = snapshot.getValue(String.class);
                if (message != null) {
                    textAlarmUser.setText(message);
                    Toast.makeText(AlarmActivity.this, message, Toast.LENGTH_LONG).show();
                    snapshot.getRef().removeValue();
                }

            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}