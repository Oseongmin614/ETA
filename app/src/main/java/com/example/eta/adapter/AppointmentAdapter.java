package com.example.eta.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eta.model.AppointmentRoom;
import com.example.eta.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private Context context;
    private List<AppointmentRoom> appointmentRoomList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(AppointmentRoom appointmentRoom);
    }

    public AppointmentAdapter(Context context, List<AppointmentRoom> appointmentRoomList,
                              OnItemClickListener onItemClickListener) {
        this.context = context;
        this.appointmentRoomList = appointmentRoomList;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_appointment_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppointmentRoom room = appointmentRoomList.get(position);

        holder.textViewAppointmentName.setText(room.getAppointmentName());
        holder.textViewCreator.setText("생성자: " + room.getCreatorNickname());
        holder.textViewCreatedTime.setText(formatTime(room.getCreatedAt()));

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(room);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointmentRoomList.size();
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewAppointmentName;
        TextView textViewCreator;
        TextView textViewCreatedTime;

        ViewHolder(View itemView) {
            super(itemView);
            textViewAppointmentName = itemView.findViewById(R.id.text_view_appointment_name);
            textViewCreator = itemView.findViewById(R.id.text_view_creator);
            textViewCreatedTime = itemView.findViewById(R.id.text_view_created_time);
        }
    }
}
