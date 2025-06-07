package com.example.eta.activity;

import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eta.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity {

    private TextView textAlarmTime;
    private TextView textUserInfo;
    private Button buttonDismiss;
    private Button buttonSnooze;
    private Button btnGoManage;

    private String nickname;
    private String roomName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        // 잠금화면 위에 표시 + 화면 켜짐
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }

        getIntentData();
        initViews();
        setupClickListeners();

        // 액션바 타이틀 설정
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("알람 - " + (roomName != null ? roomName : "ETA"));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void getIntentData() {
        Intent intent = getIntent();
        nickname = intent.getStringExtra("nickname");
        roomName = intent.getStringExtra("roomName");
    }

    private void initViews() {
        textAlarmTime = findViewById(R.id.text_alarm_time);
        textUserInfo = findViewById(R.id.text_user_info);
        buttonDismiss = findViewById(R.id.button_dismiss);
        buttonSnooze = findViewById(R.id.button_snooze);
        btnGoManage = findViewById(R.id.btn_go_manage);

        // 현재 시간 표시
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        textAlarmTime.setText(currentTime);

        // 유저 이름 표시
        if (nickname != null) {
            textUserInfo.setText(nickname + "님의 알람");
        }
    }

    private void setupClickListeners() {
        buttonDismiss.setOnClickListener(v -> {
            Toast.makeText(this, "알람을 종료합니다", Toast.LENGTH_SHORT).show();
            finish();
        });

        buttonSnooze.setOnClickListener(v -> {
            Toast.makeText(this, "5분 후 다시 울립니다 (기능 미구현)", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnGoManage.setOnClickListener(v -> {
            Intent intent = new Intent(AlarmActivity.this, AlarmSetActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // 뒤로가기
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}