package com.example.eta.activity;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.eta.R;
import com.example.eta.model.UserData;
import com.example.eta.util.DatabaseConstants;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private EditText etNickname;
    private Button btnStart;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase 초기화
        databaseRef = FirebaseDatabase.getInstance().getReference();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etNickname = findViewById(R.id.et_nickname);
        btnStart = findViewById(R.id.btn_start);

        // 다크 테마 색상 적용
        etNickname.setTextColor(getResources().getColor(R.color.text_primary));
        etNickname.setHintTextColor(getResources().getColor(R.color.text_secondary));
        etNickname.setBackgroundColor(getResources().getColor(R.color.surface_color));

        btnStart.setTextColor(getResources().getColor(R.color.text_primary));
        btnStart.setBackgroundColor(getResources().getColor(R.color.button_primary));
    }

    private void setupClickListeners() {
        btnStart.setOnClickListener(v -> {
            String nickname = etNickname.getText().toString().trim();

            if (nickname.isEmpty()) {
                showToast("닉네임을 입력해주세요");
                return;
            }

            if (!isValidNickname(nickname)) {
                showToast("닉네임은 2~10자 영문/숫자/한글로 입력해주세요");
                return;
            }

            checkNicknameAvailability(nickname);
        });
    }

    // 닉네임 유효성 검사
    private boolean isValidNickname(String nickname) {
        return nickname.matches("^[a-zA-Z0-9가-힣]{2,10}$");
    }

    // 닉네임 중복 확인 (인터넷 연결 확인 포함)
    private void checkNicknameAvailability(String nickname) {
        // 인터넷 연결 확인 코드 추가
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnected()) {
            showToast("인터넷 연결을 확인해주세요");
            return;
        }

        // Firebase에서 닉네임 고유성 확인
        databaseRef.child(DatabaseConstants.NODE_USERNAMES)
                .child(nickname)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // 기존 사용자 로그인 - 같은 닉네임 = 같은 유저
                            String existingUserId = snapshot.getValue(String.class);
                            loginExistingUser(nickname, existingUserId);
                        } else {
                            // 새 사용자 생성
                            createNewUser(nickname);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showToast("서버 연결 실패: " + error.getMessage());
                    }
                });
    }

    // 기존 사용자 로그인 처리 (다른 기기에서도 같은 닉네임 = 같은 유저)
    private void loginExistingUser(String nickname, String userId) {
        // 마지막 활동 시간 업데이트
        databaseRef.child(DatabaseConstants.NODE_USERS)
                .child(userId)
                .child(DatabaseConstants.FIELD_LAST_ACTIVE_AT)
                .setValue(System.currentTimeMillis())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast(nickname + "님 환영합니다!");
                        moveToAppointmentList(nickname, userId);
                    } else {
                        showToast("로그인 실패");
                    }
                });
    }

    // 새 사용자 생성
    private void createNewUser(String nickname) {
        String newUserId = databaseRef.push().getKey();

        if (newUserId == null) {
            showToast("사용자 생성 오류");
            return;
        }

        // 닉네임 매핑 저장 (닉네임 -> 사용자ID)
        databaseRef.child(DatabaseConstants.NODE_USERNAMES)
                .child(nickname)
                .setValue(newUserId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveUserData(newUserId, nickname);
                    } else {
                        showToast("닉네임 등록 실패");
                    }
                });
    }

    // 사용자 데이터 저장
    private void saveUserData(String userId, String nickname) {
        UserData newUser = new UserData(nickname, System.currentTimeMillis());

        databaseRef.child(DatabaseConstants.NODE_USERS)
                .child(userId)
                .setValue(newUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("새 사용자로 등록되었습니다!");
                        moveToAppointmentList(nickname, userId);
                    } else {
                        showToast("사용자 생성 실패");
                    }
                });
    }

    // 채팅방 목록으로 이동
    private void moveToAppointmentList(String nickname, String userId) {
        Intent intent = new Intent(this, AppointmentListActivity.class);
        intent.putExtra("nickname", nickname);
        intent.putExtra("userId", userId);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
