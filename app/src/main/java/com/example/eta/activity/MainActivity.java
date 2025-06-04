package com.example.eta.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eta.R;

public class MainActivity extends AppCompatActivity {

    private EditText etNickname;
    private Button btnStart;
    private Button btnAlarm; // 🔥 알람 버튼 추가

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 이미 저장된 닉네임이 있는지 확인
        checkSavedNickname();

        initViews();
        setupClickListeners();
    }

    private void checkSavedNickname() {
        SharedPreferences pref = getSharedPreferences("profile", MODE_PRIVATE);
        String savedNickname = pref.getString("nickName", "");

        if (!savedNickname.isEmpty()) {
            // 이미 닉네임이 저장되어 있으면 바로 채팅방 목록으로 이동
            Intent intent = new Intent(this, AppointmentListActivity.class);
            intent.putExtra("nickname", savedNickname);
            intent.putExtra("userId", pref.getString("userId", ""));
            startActivity(intent);
            finish();
        }
    }

    private void initViews() {
        etNickname = findViewById(R.id.et_nickname);
        btnStart = findViewById(R.id.btn_start);
        btnAlarm = findViewById(R.id.btn_alarm); // 🔥 알람 버튼 연결
    }

    private void setupClickListeners() {
        btnStart.setOnClickListener(v -> {
            String nickname = etNickname.getText().toString().trim();

            if (nickname.isEmpty()) {
                Toast.makeText(this, "닉네임을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            // 닉네임 저장
            saveNickname(nickname);

            // 채팅방 목록으로 이동 (AppointmentListActivity를 채팅방 목록으로 사용)
            Intent intent = new Intent(this, AppointmentListActivity.class);
            intent.putExtra("nickname", nickname);
            intent.putExtra("userId", "user_" + System.currentTimeMillis());
            startActivity(intent);
            finish();
        });

        // 🔥 알람 버튼 클릭 리스너
        btnAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AlarmManageActivity.class);
            startActivity(intent);
        });
    }

    private void saveNickname(String nickname) {
        SharedPreferences pref = getSharedPreferences("profile", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("nickName", nickname);
        editor.putString("userId", "user_" + System.currentTimeMillis());
        editor.apply();

        Toast.makeText(this, "프로필이 저장되었습니다", Toast.LENGTH_SHORT).show();
    }
}