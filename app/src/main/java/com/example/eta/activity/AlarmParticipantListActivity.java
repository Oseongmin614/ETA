package com.example.eta.activity;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eta.R;
import com.example.eta.adapter.ParticipantAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AlarmParticipantListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ParticipantAdapter adapter;
    private List<String> participantList = new ArrayList<>();
    private DatabaseReference participantsRef;

    private String roomId;
    private String userId;
    private String userNickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_participant_list);

        // 🔹 인텐트 데이터 수신
        roomId = getIntent().getStringExtra("roomId");
        userId = getIntent().getStringExtra("userId");
        userNickname = getIntent().getStringExtra("nickname");  // 필수

        if (roomId == null || userId == null || userNickname == null) {
            finish(); // 필수 데이터 없으면 종료
            return;
        }

        // 🔹 Firebase Realtime Database 경로 설정
        participantsRef = FirebaseDatabase.getInstance()
                .getReference("chats")
                .child(roomId)
                .child("participants");

        // 🔹 뒤로가기 버튼
        ImageView backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> finish());

        // 🔹 RecyclerView 구성
        recyclerView = findViewById(R.id.participant_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ParticipantAdapter(this, participantList, userId, userNickname);
        recyclerView.setAdapter(adapter);

        // 🔹 참여자 실시간 감시
        observeParticipants();
    }

    private void observeParticipants() {
        participantsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                participantList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String nickname = child.getValue(String.class);
                    if (nickname != null) {
                        participantList.add(nickname);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // 에러 로깅 가능
            }
        });
    }
}