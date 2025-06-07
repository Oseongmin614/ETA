package com.example.eta.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // 알람 시간이 되었을 때 간단한 토스트 알림을 띄움
        Toast.makeText(context, "⏰ 알람 시간입니다!", Toast.LENGTH_LONG).show();
    }
}