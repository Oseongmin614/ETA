package com.example.eta.adapter;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eta.R;
import com.example.eta.model.AlarmItem;
import com.example.eta.receiver.AlarmReceiver;

import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {

    private final Context context;
    private final List<AlarmItem> alarmList;

    public AlarmAdapter(Context context, List<AlarmItem> alarmList) {
        this.context = context;
        this.alarmList = alarmList;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        AlarmItem alarm = alarmList.get(position);
        holder.textViewAlarmTime.setText(alarm.getTimeText());

        holder.btnCancel.setOnClickListener(v -> {
            // 1. 시스템 알람 취소
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarm.getRequestCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }

            // 2. 리스트에서 제거
            int removedPosition = holder.getAdapterPosition();
            alarmList.remove(removedPosition);
            notifyItemRemoved(removedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    static class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView textViewAlarmTime;
        Button btnCancel;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewAlarmTime = itemView.findViewById(R.id.text_alarm_time);
            btnCancel = itemView.findViewById(R.id.btn_cancel_alarm);
        }
    }
}