package com.ptithcm.ptitmeet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingHistoryResponse;

import java.util.Objects;

public class MeetingHistoryAdapter extends ListAdapter<MeetingHistoryResponse, MeetingHistoryAdapter.ViewHolder> {

    // ──────────────────────────────────────────────────
    // DiffUtil: so sánh theo meetingCode (unique key) và
    // nội dung thay đổi (status + title)
    // ──────────────────────────────────────────────────
    private static final DiffUtil.ItemCallback<MeetingHistoryResponse> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MeetingHistoryResponse>() {
                @Override
                public boolean areItemsTheSame(@NonNull MeetingHistoryResponse oldItem,
                                               @NonNull MeetingHistoryResponse newItem) {
                    return Objects.equals(oldItem.getMeetingCode(), newItem.getMeetingCode());
                }

                @Override
                public boolean areContentsTheSame(@NonNull MeetingHistoryResponse oldItem,
                                                  @NonNull MeetingHistoryResponse newItem) {
                    return Objects.equals(oldItem.getStatus(), newItem.getStatus())
                            && Objects.equals(oldItem.getTitle(), newItem.getTitle())
                            && oldItem.isOwner() == newItem.isOwner()
                            && oldItem.isHost() == newItem.isHost();
                }
            };

    public interface OnMeetingActionListener {
        void onJoin(MeetingHistoryResponse meeting);
        void onCancel(MeetingHistoryResponse meeting);
        void onViewChat(MeetingHistoryResponse meeting);
    }

    private final OnMeetingActionListener listener;

    public MeetingHistoryAdapter(OnMeetingActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_meeting_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MeetingHistoryResponse item = getItem(position); // ListAdapter cung cấp getItem()

        holder.tvMeetingTitle.setText(item.getTitle() != null ? item.getTitle() : "Untitled meeting");
        holder.tvMeetingCode.setText(item.getMeetingCode());
        holder.tvMeetingTime.setText(item.getDisplayTime());

        boolean isOwner = item.isOwner() || item.isHost();
        if (isOwner) {
            holder.tvRoleBadge.setText("Host");
            holder.tvRoleBadge.setTextColor(android.graphics.Color.parseColor("#FBBF24"));
        } else {
            holder.tvRoleBadge.setText("Guest");
            holder.tvRoleBadge.setTextColor(android.graphics.Color.parseColor("#93C5FD"));
        }

        String status = item.getStatus().toUpperCase();
        holder.tvStatusBadge.setText(item.getDisplayStatusLabel());
        holder.tvStatusBadge.setTextColor(item.getStatusColor());

        boolean isUpcomingOrActive = "SCHEDULED".equals(status) || "UPCOMING".equals(status) || "ACTIVE".equals(status);
        if (isUpcomingOrActive) {
            holder.btnJoinMeeting.setVisibility(View.VISIBLE);
            holder.btnJoinMeeting.setOnClickListener(v -> listener.onJoin(item));
        } else {
            holder.btnJoinMeeting.setVisibility(View.GONE);
        }

        boolean canCancel = isOwner && ("SCHEDULED".equals(status) || "UPCOMING".equals(status) || "ACTIVE".equals(status));
        if (canCancel) {
            holder.btnCancelMeeting.setVisibility(View.VISIBLE);
            holder.btnCancelMeeting.setOnClickListener(v -> listener.onCancel(item));
        } else {
            holder.btnCancelMeeting.setVisibility(View.GONE);
        }

        if (item.isCanViewChatHistory()) {
            holder.btnViewChat.setVisibility(View.VISIBLE);
            holder.btnViewChat.setOnClickListener(v -> listener.onViewChat(item));
        } else {
            holder.btnViewChat.setVisibility(View.GONE);
        }

        holder.layoutCardActions.setVisibility(
                holder.btnJoinMeeting.getVisibility() == View.VISIBLE
                        || holder.btnCancelMeeting.getVisibility() == View.VISIBLE
                        || holder.btnViewChat.getVisibility() == View.VISIBLE
                        ? View.VISIBLE : View.GONE);
    }

    // getItemCount() KHÔNG cần override — ListAdapter đã xử lý

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRoleBadge;
        private final TextView tvStatusBadge;
        private final TextView tvMeetingTitle;
        private final TextView tvMeetingTime;
        private final TextView tvMeetingCode;
        private final LinearLayout layoutCardActions;
        private final AppCompatButton btnCancelMeeting;
        private final AppCompatButton btnViewChat;
        private final AppCompatButton btnJoinMeeting;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoleBadge = itemView.findViewById(R.id.tvRoleBadge);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvMeetingTitle = itemView.findViewById(R.id.tvMeetingTitle);
            tvMeetingTime = itemView.findViewById(R.id.tvMeetingTime);
            tvMeetingCode = itemView.findViewById(R.id.tvMeetingCode);
            layoutCardActions = itemView.findViewById(R.id.layoutCardActions);
            btnCancelMeeting = itemView.findViewById(R.id.btnCancelMeeting);
            btnViewChat = itemView.findViewById(R.id.btnViewChat);
            btnJoinMeeting = itemView.findViewById(R.id.btnJoinMeeting);
        }
    }
}
