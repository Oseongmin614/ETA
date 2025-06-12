package com.example.eta.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eta.R;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {

    private final Context context;
    private final List<String> participantList;
    private final String userId;
    private final String userNickname;

    public ParticipantAdapter(Context context, List<String> participantList, String userId, String userNickname) {
        this.context = context;
        this.participantList = participantList;
        this.userId = userId;
        this.userNickname = userNickname;
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_participant, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        String participantName = participantList.get(position);
        holder.nameTextView.setText(participantName);

        holder.alarmButton.setVisibility(View.VISIBLE);

        if (participantName.equals(userNickname)) {
            // 자기 자신: 클릭 비활성화 + 흐리게 표시
            holder.alarmButton.setEnabled(false);
            holder.alarmButton.setAlpha(0.3f);
        } else {
            // 다른 사람: 클릭 가능
            holder.alarmButton.setEnabled(true);
            holder.alarmButton.setAlpha(1f);
            holder.alarmButton.setOnClickListener(v -> {
                sendAlarmToUser(participantName);
            });
        }
    }

    @Override
    public int getItemCount() {
        return participantList.size();
    }

    private void sendAlarmToUser(String targetNickname) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("alarmRequests")
                .child(targetNickname);

        ref.push().setValue("지각입니다 서두르세요")
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context, targetNickname + "님에게 알람 전송 완료", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(context, "알람 전송 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    public static class ParticipantViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        ImageButton alarmButton;

        public ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.participantNameTextView);
            alarmButton = itemView.findViewById(R.id.alarmIconButton);
        }
    }
}