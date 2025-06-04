package com.example.eta.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eta.R;
import com.example.eta.model.ChatMessage;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MY_MESSAGE = 0;
    private static final int VIEW_TYPE_OTHER_MESSAGE = 1;
    private static final int VIEW_TYPE_SYSTEM_MESSAGE = 2;

    private Context context;
    private List<ChatMessage> messageList;
    private String currentUserId;

    public ChatAdapter(Context context, List<ChatMessage> messageList, String currentUserId) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        if (message.isSystemMessage()) {
            return VIEW_TYPE_SYSTEM_MESSAGE;
        } else if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_MY_MESSAGE;
        } else {
            return VIEW_TYPE_OTHER_MESSAGE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_MY_MESSAGE) {
            view = LayoutInflater.from(context).inflate(R.layout.item_chat_me, parent, false);
            return new MessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_OTHER_MESSAGE) {
            view = LayoutInflater.from(context).inflate(R.layout.item_chat_other, parent, false);
            return new MessageViewHolder(view);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.item_chat_system, parent, false);
            return new SystemMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        if (holder instanceof MessageViewHolder) {
            MessageViewHolder messageHolder = (MessageViewHolder) holder;

            // null 체크 추가
            if (messageHolder.messageText != null) {
                messageHolder.messageText.setText(message.getMessage());
                messageHolder.messageText.setTextColor(context.getResources().getColor(R.color.text_primary));
            }

            if (messageHolder.timeText != null) {
                messageHolder.timeText.setText(message.getFormattedTime());
                messageHolder.timeText.setTextColor(context.getResources().getColor(R.color.text_secondary));
            }

            if (getItemViewType(position) == VIEW_TYPE_OTHER_MESSAGE) {
                // 상대방 메시지
                if (messageHolder.nicknameText != null) {
                    messageHolder.nicknameText.setVisibility(View.VISIBLE);
                    messageHolder.nicknameText.setText(message.getSenderNickname());
                    messageHolder.nicknameText.setTextColor(context.getResources().getColor(R.color.text_secondary));
                }

                if (messageHolder.messageText != null) {
                    messageHolder.messageText.setBackgroundColor(context.getResources().getColor(R.color.surface_color));
                }
            } else {
                // 내 메시지
                if (messageHolder.nicknameText != null) {
                    messageHolder.nicknameText.setVisibility(View.GONE);
                }

                if (messageHolder.messageText != null) {
                    messageHolder.messageText.setBackgroundColor(context.getResources().getColor(R.color.button_primary));
                }
            }

        } else if (holder instanceof SystemMessageViewHolder) {
            SystemMessageViewHolder systemHolder = (SystemMessageViewHolder) holder;
            if (systemHolder.systemMessageText != null) {
                systemHolder.systemMessageText.setText(message.getMessage());
                systemHolder.systemMessageText.setTextColor(context.getResources().getColor(R.color.text_secondary));
                systemHolder.systemMessageText.setBackgroundColor(context.getResources().getColor(R.color.gray6));
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView nicknameText;
        TextView messageText;
        TextView timeText;

        MessageViewHolder(View itemView) {
            super(itemView);
            nicknameText = itemView.findViewById(R.id.text_nickname);
            messageText = itemView.findViewById(R.id.text_message);
            timeText = itemView.findViewById(R.id.text_time);

            // 디버깅용 로그 (나중에 제거 가능)
            if (nicknameText == null) {
                Log.d("ChatAdapter", "nicknameText is null - check R.id.text_nickname in layout");
            }
            if (messageText == null) {
                Log.e("ChatAdapter", "messageText is null - check R.id.text_message in layout");
            }
            if (timeText == null) {
                Log.e("ChatAdapter", "timeText is null - check R.id.text_time in layout");
            }
        }
    }

    static class SystemMessageViewHolder extends RecyclerView.ViewHolder {
        TextView systemMessageText;

        SystemMessageViewHolder(View itemView) {
            super(itemView);
            systemMessageText = itemView.findViewById(R.id.text_system_message);

            if (systemMessageText == null) {
                Log.e("ChatAdapter", "systemMessageText is null - check R.id.text_system_message in layout");
            }
        }
    }
}
