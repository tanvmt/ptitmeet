package com.ptithcm.ptitmeet;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ViewHolder> {

    private List<ParticipantData> participantList;

    public ParticipantAdapter(List<ParticipantData> participantList) {
        this.participantList = participantList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Nạp layout item_participant_video.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_participant_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParticipantData p = participantList.get(position);

        // Hiển thị tên
        holder.tvParticipantName.setText(p.getName());

        // Xử lý bật/tắt Video
        if (p.hasVideo()) {
            holder.videoContainer.setVisibility(View.VISIBLE);
            holder.avatarContainer.setVisibility(View.GONE);
            // TODO: Gắn LiveKit VideoTrack vào holder.videoContainer ở đây
        } else {
            holder.videoContainer.setVisibility(View.GONE);
            holder.avatarContainer.setVisibility(View.VISIBLE);
            // Lấy chữ cái đầu làm Avatar
            if (p.getName() != null && !p.getName().isEmpty()) {
                holder.tvAvatarInitial.setText(String.valueOf(p.getName().charAt(0)).toUpperCase());
            } else {
                holder.tvAvatarInitial.setText("U");
            }
        }

        // Xử lý bật/tắt Mic
        if (p.isMicOn()) {
            holder.ivMicOff.setVisibility(View.GONE); // Mic đang bật thì ẩn icon mic_off
        } else {
            holder.ivMicOff.setVisibility(View.VISIBLE); // Mic tắt thì hiện icon mic_off
        }

        // Xử lý Active Speaker (Viền xanh)
        if (p.isSpeaking()) {
            holder.viewActiveSpeakerBorder.setVisibility(View.VISIBLE);
        } else {
            holder.viewActiveSpeakerBorder.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return participantList == null ? 0 : participantList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout videoContainer;
        FrameLayout avatarContainer;
        TextView tvAvatarInitial;
        TextView tvParticipantName;
        ImageView ivMicOff;
        View viewActiveSpeakerBorder;

        public ViewHolder(View itemView) {
            super(itemView);
            // Ánh xạ id từ item_participant_video.xml
            videoContainer = itemView.findViewById(R.id.videoContainer);
            avatarContainer = itemView.findViewById(R.id.avatarContainer);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
            tvParticipantName = itemView.findViewById(R.id.tvParticipantName);
            ivMicOff = itemView.findViewById(R.id.ivMicOff);
            viewActiveSpeakerBorder = itemView.findViewById(R.id.viewActiveSpeakerBorder);
        }
    }
}