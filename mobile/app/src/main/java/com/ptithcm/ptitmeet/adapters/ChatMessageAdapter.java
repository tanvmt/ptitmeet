package com.ptithcm.ptitmeet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.dto.chat.ChatMessageResponse;
import com.ptithcm.ptitmeet.utils.MeetingUiFormatter;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ViewHolder> {

    private final List<ChatMessageResponse> messages = new ArrayList<>();
    private final String currentUserId;

    public ChatMessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void submitList(List<ChatMessageResponse> newMessages) {
        messages.clear();
        if (newMessages != null) {
            messages.addAll(newMessages);
        }
        notifyDataSetChanged();
    }

    public void addMessage(ChatMessageResponse message) {
        if (message == null) {
            return;
        }
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessageResponse item = messages.get(position);
        boolean isMine = currentUserId != null && currentUserId.equals(String.valueOf(item.getSenderId()));

        holder.senderName.setText(isMine ? "You" : item.getSenderName());
        holder.messageContent.setText(item.getContent());
        holder.messageTime.setText(MeetingUiFormatter.formatClock(item.getTimestamp()));
        holder.messageBubble.setBackgroundResource(isMine ? R.drawable.bg_chat_bubble_mine : R.drawable.bg_chat_bubble_other);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.messageContainer.getLayoutParams();
        params.gravity = isMine ? android.view.Gravity.END : android.view.Gravity.START;
        holder.messageContainer.setLayoutParams(params);
        holder.senderName.setTextAlignment(isMine ? View.TEXT_ALIGNMENT_VIEW_END : View.TEXT_ALIGNMENT_VIEW_START);
        holder.messageTime.setTextColor(holder.itemView.getContext().getColor(isMine ? R.color.white : R.color.text_secondary));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView senderName;
        private final TextView messageContent;
        private final TextView messageTime;
        private final View messageBubble;
        private final LinearLayout messageContainer;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.chatMessageContainer);
            senderName = itemView.findViewById(R.id.tvChatSender);
            messageContent = itemView.findViewById(R.id.tvChatContent);
            messageTime = itemView.findViewById(R.id.tvChatTime);
            messageBubble = itemView.findViewById(R.id.chatBubble);
        }
    }
}
