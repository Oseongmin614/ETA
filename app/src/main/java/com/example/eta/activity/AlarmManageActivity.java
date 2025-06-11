package com.example.eta.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eta.adapter.AlarmAdapter;
import com.example.eta.model.AlarmItem;

import java.util.ArrayList;

public class AlarmManageActivity extends Activity {

    private RecyclerView alarmRecyclerView;
    private AlarmAdapter adapter;
    private ArrayList<AlarmItem> alarmList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔧 루트 레이아웃 생성
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(32, 32, 32, 32);
        rootLayout.setBackgroundColor(0xFF121212); // 다크 배경
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 🔧 RecyclerView 생성
        alarmRecyclerView = new RecyclerView(this);
        alarmRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        alarmRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ));

        // 🔧 뒤로가기 버튼 생성
        Button btnBack = new Button(this);
        btnBack.setText("뒤로 가기");
        btnBack.setTextColor(0xFFFFFFFF);
        btnBack.setBackgroundColor(0xFFA020F0);
        btnBack.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 🔧 버튼 클릭 시 액티비티 종료
        btnBack.setOnClickListener(v -> finish());

        // 🔧 리스트 초기화 및 어댑터 연결
        alarmList = new ArrayList<>();
        adapter = new AlarmAdapter(this, alarmList);
        alarmRecyclerView.setAdapter(adapter);

        // 🔧 전달받은 알람이 있다면 추가
        AlarmItem newAlarm = (AlarmItem) getIntent().getSerializableExtra("alarm");
        if (newAlarm != null) {
            alarmList.add(newAlarm);
            adapter.notifyDataSetChanged();
        }

        // 🔧 레이아웃에 뷰 추가
        rootLayout.addView(alarmRecyclerView);
        rootLayout.addView(btnBack);

        // 🔧 화면에 표시
        setContentView(rootLayout);
    }
}