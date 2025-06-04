package com.example.eta.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eta.R;
import com.example.eta.adapter.AlarmAdapter;
import com.example.eta.model.AlarmItem;
import com.example.eta.receiver.AlarmReceiver;

import java.util.ArrayList;
import java.util.Calendar;

public class AlarmManageActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ADD_ALARM = 100;

    private RecyclerView recyclerView;
    private AlarmAdapter alarmAdapter;
    private ArrayList<AlarmItem> alarmList;
    private Button btnSetAlarm;
    private TextView textCurrentTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_manage);

        initViews();
        setupClickListeners();

        alarmList = new ArrayList<>();
        alarmAdapter = new AlarmAdapter(this, alarmList);
        recyclerView.setAdapter(alarmAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_alarm_list);
        btnSetAlarm = findViewById(R.id.btn_set_alarm);
        textCurrentTime = findViewById(R.id.text_current_time);
    }

    private void setupClickListeners() {
        // 알람 설정 버튼 클릭 시 AlarmSetActivity 실행
        btnSetAlarm.setOnClickListener(v -> {
            Intent intent = new Intent(this, AlarmSetActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_ALARM);
        });
    }

    private void addAlarm(int hour, int minute) {
        String timeText = String.format("%02d:%02d", hour, minute);
        int requestCode = hour * 100 + minute;
        long timeInMillis = getMillisFromTime(hour, minute);

        AlarmItem alarmItem = new AlarmItem(timeText, requestCode, true, timeInMillis);
        alarmList.add(alarmItem);
        alarmAdapter.notifyItemInserted(alarmList.size() - 1);

        Log.d("AlarmManageActivity", "알람 추가됨: " + timeText + ", 총 개수: " + alarmList.size());

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("hour", hour);
        intent.putExtra("minute", minute);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private long getMillisFromTime(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADD_ALARM && resultCode == RESULT_OK && data != null) {
            String timeText = data.getStringExtra("timeText");
            long timeInMillis = data.getLongExtra("timeInMillis", 0);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeInMillis);
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            addAlarm(hour, minute);
        }
    }
}