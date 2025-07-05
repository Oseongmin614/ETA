package com.example.eta.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.eta.R;

public class CreateRoomInfoActivity extends AppCompatActivity {

    private EditText editLocation;
    private Spinner spinnerMonth, spinnerDay, spinnerHour, spinnerMinute;
    private Button btnCreateRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room_info);

        editLocation = findViewById(R.id.edit_location);
        spinnerMonth = findViewById(R.id.spinner_month);
        spinnerDay = findViewById(R.id.spinner_day);
        spinnerHour = findViewById(R.id.spinner_hour);
        spinnerMinute = findViewById(R.id.spinner_minute);
        btnCreateRoom = findViewById(R.id.btn_create_room);

        setupSpinners();

        btnCreateRoom.setOnClickListener(v -> {
            String location = editLocation.getText().toString();
            String month = spinnerMonth.getSelectedItem().toString();
            String day = spinnerDay.getSelectedItem().toString();
            String hour = spinnerHour.getSelectedItem().toString();
            String minute = spinnerMinute.getSelectedItem().toString();

            String timeFormatted = hour + ":" + minute;

            Intent intent = new Intent();
            intent.putExtra("location", location);
            intent.putExtra("time", timeFormatted);
            setResult(RESULT_OK, intent);
            finish();
        });
    }

    private void setupSpinners() {
        spinnerMonth.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"4월", "5월", "6월"}));
        spinnerDay.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"19일", "20일", "21일"}));
        spinnerHour.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"09", "10", "11", "12"}));
        spinnerMinute.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"00", "15", "30", "45"}));
    }
}
