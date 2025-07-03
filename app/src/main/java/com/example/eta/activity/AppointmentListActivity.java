package com.example.eta.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("채팅방 목록");
        }

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
                PopupMenu popup = new PopupMenu(AppointmentListActivity.this, fabCreateAppointment, 0, 0, R.style.PopupMenuTextStyle);
                popup.getMenuInflater().inflate(R.menu.chat_room_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_create_chat_room) {
                        showCreateChatRoomDialog();
                        return true;
                    } else if (item.getItemId() == R.id.menu_join_chat_room) {
                        showJoinChatRoomDialog();
                        return true;
                    }
                    return false;
                });
                popup.show();
            }
        });
    }

    // 채팅방 생성 다이얼로그
    private void showCreateChatRoomDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_create_chat_room, null);

        EditText editRoomName = dialogView.findViewById(R.id.editRoomName);
        EditText editRoomCode = dialogView.findViewById(R.id.editRoomCode);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("새 채팅방 만들기")
                .setView(dialogView)
                .setPositiveButton("만들기", (dialogInterface, which) -> {
                    String chatRoomName = editRoomName.getText().toString().trim();
                    String chatRoomCode = editRoomCode.getText().toString().trim();
                    if (!chatRoomName.isEmpty() && !chatRoomCode.isEmpty()) {
                        checkCodeAndCreateRoom(chatRoomName, chatRoomCode);
                    } else {
                        Toast.makeText(this, "이름과 코드를 모두 입력하세요.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", null)
                .create();

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    // 채팅방 참여 다이얼로그
    private void showJoinChatRoomDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_join_chat_room, null);

        EditText editChatRoomCode = dialogView.findViewById(R.id.editChatRoomCode);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("채팅방 참여하기")
                .setView(dialogView)
                .setPositiveButton("참여", (dialogInterface, which) -> {
                    String code = editChatRoomCode.getText().toString().trim();
                    if (!code.isEmpty()) {
                        searchAndJoinChatRoom(code);
                    } else {
                        Toast.makeText(this, "채팅방 코드를 입력하세요.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", null)
                .create();

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    // 중복 코드 확인 후 채팅방 생성
    private void checkCodeAndCreateRoom(String chatRoomName, String chatRoomCode) {
        databaseReference.child("chatRooms")
                .orderByChild("chatRoomCode")
                .equalTo(chatRoomCode)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(AppointmentListActivity.this,
                                    "이미 사용 중인 코드입니다. 다른 코드를 입력하세요.", Toast.LENGTH_SHORT).show();
                        } else {
                            createChatRoom(chatRoomName, chatRoomCode);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AppointmentListActivity.this,
                                "코드 확인 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 채팅방 생성
    private void createChatRoom(String chatRoomName, String chatRoomCode) {
        String roomId = databaseReference.child("chatRooms").push().getKey();
        AppointmentRoom newRoom = new AppointmentRoom(
                roomId,
                chatRoomName,
                nickname,
                System.currentTimeMillis(),
                1,
                chatRoomCode // 반드시 6번째 인자!
        );

        databaseReference.child("chatRooms")
                .child(roomId)
                .setValue(newRoom)
                .addOnSuccessListener(aVoid -> {
                    addUserToChatRoom(roomId);
                    Toast.makeText(this, "채팅방이 생성되었습니다", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "채팅방 생성 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // 코드로 채팅방 검색 후 참여
    private void searchAndJoinChatRoom(String chatRoomCode) {
        databaseReference.child("chatRooms")
                .orderByChild("chatRoomCode")
                .equalTo(chatRoomCode)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                                AppointmentRoom room = roomSnapshot.getValue(AppointmentRoom.class);
                                if (room != null) {
                                    checkAndJoinRoom(room);
                                    break;
                                }
                            }
                        } else {
                            Toast.makeText(AppointmentListActivity.this,
                                    "해당 코드의 채팅방을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AppointmentListActivity.this,
                                "채팅방 검색 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 이미 참여한 방인지 확인 후 참여 처리
    private void checkAndJoinRoom(AppointmentRoom room) {
        databaseReference.child("chatRooms")
                .child(room.getRoomId())
                .child("participants")
                .child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(AppointmentListActivity.this,
                                    "이미 참여한 채팅방입니다.", Toast.LENGTH_SHORT).show();
                            enterChatRoom(room);
                        } else {
                            addUserToChatRoom(room.getRoomId());
                            updateParticipantCount(room.getRoomId(), room.getParticipantCount() + 1);
                            Toast.makeText(AppointmentListActivity.this,
                                    "채팅방에 참여했습니다!", Toast.LENGTH_SHORT).show();
                            enterChatRoom(room);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AppointmentListActivity.this,
                                "참여 확인 실패: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // participants에 사용자 추가
    private void addUserToChatRoom(String roomId) {
        databaseReference.child("chatRooms")
                .child(roomId)
                .child("participants")
                .child(userId)
                .setValue(nickname);
    }

    // 참여자 수 업데이트
    private void updateParticipantCount(String roomId, int newCount) {
        databaseReference.child("chatRooms")
                .child(roomId)
                .child("participantCount")
                .setValue(newCount);
    }

    // 채팅방 입장
    private void enterChatRoom(AppointmentRoom room) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("nickname", nickname);
        intent.putExtra("userId", userId);
        intent.putExtra("roomId", room.getRoomId());
        intent.putExtra("roomName", room.getAppointmentName());
        startActivity(intent);
    }

    // participants에 내가 포함된 채팅방만 목록에 표시
    private void loadAppointmentRooms() {
        databaseReference.child("chatRooms")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        appointmentRoomList.clear();
                        for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
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

    // 목록에서 채팅방 클릭 시 입장
    private void joinChatRoom(AppointmentRoom appointmentRoom) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("nickname", nickname);
        intent.putExtra("userId", userId);
        intent.putExtra("roomId", appointmentRoom.getRoomId());
        intent.putExtra("roomName", appointmentRoom.getAppointmentName());
        startActivity(intent);
    }
}
