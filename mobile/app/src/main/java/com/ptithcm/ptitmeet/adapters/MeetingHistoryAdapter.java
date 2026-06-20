package com.ptithcm.ptitmeet.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        holder.tvMeetingTitle.setText(item.getTitle() != null ? item.getTitle() : "Cuộc họp không tên");
        holder.tvMeetingCode.setText(item.getMeetingCode());
        holder.tvMeetingTime.setText(item.getDisplayTime());

        // Role Badge Setup
        boolean isOwner = item.isOwner() || item.isHost();
        if (isOwner) {
            holder.tvRoleBadge.setText("CHỦ PHÒNG");
            holder.tvRoleBadge.setTextColor(Color.parseColor("#FB923C")); // Orange-400
            holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_recording_pill);
        } else {
            holder.tvRoleBadge.setText("THAM GIA");
            holder.tvRoleBadge.setTextColor(Color.parseColor("#60A5FA")); // Blue-400
            holder.tvRoleBadge.setBackgroundResource(R.drawable.bg_recording_pill);
        }

        // Status Badge Translation & Color
        String status = item.getStatus().toUpperCase();
        String displayStatus = status;
        int statusColor = Color.parseColor("#94A3B8"); // Slate-400 default

        if ("SCHEDULED".equals(status) || "UPCOMING".equals(status)) {
            displayStatus = "SẮP DIỄN RA";
            statusColor = Color.parseColor("#93C5FD"); // Blue-300
        } else if ("ACTIVE".equals(status) || "LIVE".equals(status)) {
            displayStatus = "ĐANG DIỄN RA";
            statusColor = Color.parseColor("#4ADE80"); // Green-400
        } else if ("ENDED".equals(status) || "COMPLETED".equals(status) || "FINISHED".equals(status)) {
            displayStatus = "ĐÃ KẾT THÚC";
            statusColor = Color.parseColor("#A1A1AA"); // Zinc-400
        } else if ("CANCELLED".equals(status) || "CANCELED".equals(status)) {
            displayStatus = "ĐÃ HỦY";
            statusColor = Color.parseColor("#FCA5A5"); // Red-300
        }

        holder.tvStatusBadge.setText(displayStatus);
        holder.tvStatusBadge.setTextColor(statusColor);

        // Action Buttons Logic
        boolean isUpcomingOrActive = "SCHEDULED".equals(status) || "UPCOMING".equals(status) || "ACTIVE".equals(status);

        // 1. Join Button
        if (isUpcomingOrActive) {
            holder.btnJoinMeeting.setVisibility(View.VISIBLE);
            holder.btnJoinMeeting.setOnClickListener(v -> listener.onJoin(item));
        } else {
            holder.btnJoinMeeting.setVisibility(View.GONE);
        }

        // 2. Cancel Button (Only if Host/Owner and Scheduled/Upcoming)
        boolean canCancel = isOwner && ("SCHEDULED".equals(status) || "UPCOMING".equals(status) || "ACTIVE".equals(status));
        if (canCancel) {
            holder.btnCancelMeeting.setVisibility(View.VISIBLE);
            holder.btnCancelMeeting.setOnClickListener(v -> listener.onCancel(item));
        } else {
            holder.btnCancelMeeting.setVisibility(View.GONE);
        }

        // 3. View Chat Button (If canViewChatHistory is true)
        if (item.isCanViewChatHistory()) {
            holder.btnViewChat.setVisibility(View.VISIBLE);
            holder.btnViewChat.setOnClickListener(v -> listener.onViewChat(item));
        } else {
            holder.btnViewChat.setVisibility(View.GONE);
        }
    }

    // getItemCount() KHÔNG cần override — ListAdapter đã xử lý

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRoleBadge;
        private final TextView tvStatusBadge;
        private final TextView tvMeetingTitle;
        private final TextView tvMeetingTime;
        private final TextView tvMeetingCode;
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
            btnCancelMeeting = itemView.findViewById(R.id.btnCancelMeeting);
            btnViewChat = itemView.findViewById(R.id.btnViewChat);
            btnJoinMeeting = itemView.findViewById(R.id.btnJoinMeeting);
        }
    }
}
