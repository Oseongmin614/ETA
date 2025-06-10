package com.example.eta.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.eta.R;
import com.example.eta.adapter.AppointmentAdapter;
import com.example.eta.model.AppointmentRoom;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class AppointmentListActivity extends AppCompatActivity {

    private TextView textViewWelcome;
    private RecyclerView recyclerViewAppointments;
    private FloatingActionButton fabCreateAppointment;
    private String nickname;
    private String userId;
    private DatabaseReference databaseReference;
    private AppointmentAdapter appointmentAdapter;
    private List<AppointmentRoom> appointmentRoomList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_list);

        getIntentData();
        initializeFirebase();
        initializeViews();
        setupWelcomeMessage();
        setupRecyclerView();
        setupClickListeners();
        loadAppointmentRooms();
    }

    // Intent에서 사용자 정보 받기
    private void getIntentData() {
        nickname = getIntent().getStringExtra("nickname");
        userId = getIntent().getStringExtra("userId");

        if (nickname == null || userId == null) {
            Toast.makeText(this, "사용자 정보가 없습니다", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeFirebase() {
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }

    private void initializeViews() {
        textViewWelcome = findViewById(R.id.text_view_welcome);
        recyclerViewAppointments = findViewById(R.id.recycler_view_appointments);
        fabCreateAppointment = findViewById(R.id.fab_create_appointment);

        // 액션바 설정
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("채팅방 목록");
        }

        // 다크 테마 설정
        recyclerViewAppointments.setBackgroundColor(getResources().getColor(R.color.background_color));
    }

    private void setupWelcomeMessage() {
        if (nickname != null) {
            textViewWelcome.setText(nickname + "님, 채팅방을 선택하세요!");
            textViewWelcome.setTextColor(getResources().getColor(R.color.text_primary));
        }
    }

    private void setupRecyclerView() {
        appointmentRoomList = new ArrayList<>();
        appointmentAdapter = new AppointmentAdapter(this, appointmentRoomList, new AppointmentAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(AppointmentRoom appointmentRoom) {
                joinChatRoom(appointmentRoom);
            }
        });
        recyclerViewAppointments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewAppointments.setAdapter(appointmentAdapter);
    }

    private void setupClickListeners() {
        fabCreateAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(AppointmentListActivity.this, fabCreateAppointment);
                popup.getMenuInflater().inflate(R.menu.chat_room_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_create_chat_room) {
                        showCreateChatRoomDialog();
                        return true;
                    } else if (item.getItemId() == R.id.menu_join_chat_room) {
                       showJoinChatRoomDialog(); // 새로 구현 필요
                        return true;
                    }
                    return false;
                });
                popup.show();
            }
        });

    }
    private void showJoinChatRoomDialog() {
        // 다이얼로그 레이아웃 inflate
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_join_chat_room, null);

        EditText editChatRoomCode = dialogView.findViewById(R.id.editChatRoomCode);

        new AlertDialog.Builder(this)
                .setTitle("채팅방 참여하기")
                .setView(dialogView)
                .setPositiveButton("참여", (dialog, which) -> {
                    String code = editChatRoomCode.getText().toString().trim();
                    if (!code.isEmpty()) {
                        joinChatRoom(code); // 실제 채팅방 참여 로직 함수
                    } else {
                        Toast.makeText(this, "채팅방 코드를 입력하세요.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // 실제 채팅방 참여 로직 예시
    private void joinChatRoom(String code) {
        // 서버에 코드로 채팅방 참여 요청 등 실제 구현 필요
        Toast.makeText(this, "채팅방 [" + code + "] 참여 시도!", Toast.LENGTH_SHORT).show();
        // TODO: 참여 성공 시 채팅방 화면으로 이동 등 처리
    }
    private void showCreateChatRoomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("새 채팅방 만들기");
        final EditText input = new EditText(this);
        input.setHint("채팅방 이름을 입력하세요");
        input.setTextColor(getResources().getColor(R.color.text_primary));
        input.setHintTextColor(getResources().getColor(R.color.text_secondary));
        input.setBackgroundColor(getResources().getColor(R.color.surface_color));
        builder.setView(input);

        builder.setPositiveButton("만들기", (dialog, which) -> {
            String chatRoomName = input.getText().toString().trim();
            if (!chatRoomName.isEmpty()) {
                createChatRoom(chatRoomName);
            } else {
                Toast.makeText(this, "채팅방 이름을 입력해주세요", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());
        AlertDialog alertDialog = builder.create();
        alertDialog.getWindow().setBackgroundDrawableResource(R.color.surface_color);
        alertDialog.show();
    }

    // 채팅방 생성: participants에 생성자 추가
    private void createChatRoom(String chatRoomName) {
        String roomId = databaseReference.child("chatRooms").push().getKey();
        AppointmentRoom newRoom = new AppointmentRoom(
                roomId,
                chatRoomName,
                nickname, // 생성자
                System.currentTimeMillis(),
                1 // 참여자 수 (생성자 포함)
        );

        // 1. 채팅방 정보 저장
        databaseReference.child("chatRooms")
                .child(roomId)
                .setValue(newRoom)
                .addOnSuccessListener(aVoid -> {
                    // 2. 생성자를 participants에 추가
                    addUserToChatRoom(roomId);
                    Toast.makeText(this, "채팅방이 생성되었습니다", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "채팅방 생성 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // participants에 userId 추가
    private void addUserToChatRoom(String roomId) {
        databaseReference.child("chatRooms")
                .child(roomId)
                .child("participants")
                .child(userId)
                .setValue(nickname);
    }

    // participants에 내가 포함된 채팅방만 목록에 추가
    private void loadAppointmentRooms() {
        databaseReference.child("chatRooms")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        appointmentRoomList.clear();
                        for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                            // participants에 내 userId가 있는지 확인
                            DataSnapshot participantsSnapshot = roomSnapshot.child("participants");
                            if (participantsSnapshot.hasChild(userId)) {
                                AppointmentRoom room = roomSnapshot.getValue(AppointmentRoom.class);
                                if (room != null) {
                                    appointmentRoomList.add(room);
                                }
                            }
                        }
                        appointmentAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AppointmentListActivity.this,
                                "채팅방 목록 로드 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void joinChatRoom(AppointmentRoom appointmentRoom) {
        // 채팅방으로 이동
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("nickname", nickname);
        intent.putExtra("userId", userId);
        intent.putExtra("roomId", appointmentRoom.getRoomId());
        intent.putExtra("roomName", appointmentRoom.getAppointmentName());
        startActivity(intent);
    }
}
