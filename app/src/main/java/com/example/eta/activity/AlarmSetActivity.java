package com.example.eta.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TimePicker;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eta.R;
import com.example.eta.adapter.AlarmAdapter;
import com.example.eta.model.AlarmItem;
import com.example.eta.receiver.AlarmReceiver;

import java.util.ArrayList;
import java.util.Calendar;

public class AlarmSetActivity extends AppCompatActivity {

    private TimePicker timePicker;
    private Button btnSaveAlarm;
    private RecyclerView recyclerView;
    private AlarmAdapter alarmAdapter;
    private ArrayList<AlarmItem> alarmList;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_set);

        initViews();
        setupRecyclerView();
        setupClickListeners();
    }

    private void initViews() {
        timePicker = findViewById(R.id.time_picker);
        btnSaveAlarm = findViewById(R.id.btn_save_alarm);
        recyclerView = findViewById(R.id.recycler_alarm_list);
    }

    private void setupRecyclerView() {
        alarmList = new ArrayList<>();
        alarmAdapter = new AlarmAdapter(this, alarmList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(alarmAdapter);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void setupClickListeners() {
        btnSaveAlarm.setOnClickListener(v -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();
            addAlarm(hour, minute);
        });
    }

    private void addAlarm(int hour, int minute) {
        String timeText = String.format("%02d:%02d", hour, minute);
        int requestCode = hour * 100 + minute;
        long timeInMillis = getMillisFromTime(hour, minute);

        // 알람 객체 추가
        AlarmItem alarmItem = new AlarmItem(timeText, requestCode, true, timeInMillis);
        alarmList.add(alarmItem);
        alarmAdapter.notifyItemInserted(alarmList.size() - 1);

        // 시스템 알람 설정
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
}