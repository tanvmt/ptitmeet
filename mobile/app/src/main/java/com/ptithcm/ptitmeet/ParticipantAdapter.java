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

    public interface ParticipantVideoBinder {
        void bindVideo(ParticipantData participant, FrameLayout videoContainer);
    }

    private List<ParticipantData> participantList;
    private ParticipantVideoBinder videoBinder;

    public ParticipantAdapter(List<ParticipantData> participantList) {
        this.participantList = participantList;
    }

    public void setParticipantList(List<ParticipantData> participantList) {
        this.participantList = participantList;
        notifyDataSetChanged();
    }

    public void setVideoBinder(ParticipantVideoBinder videoBinder) {
        this.videoBinder = videoBinder;
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

        // Dynamically adjust item height based on participant count
        int totalItems = getItemCount();
        ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
        if (lp != null) {
            holder.itemView.post(() -> {
                ViewGroup.LayoutParams currentLp = holder.itemView.getLayoutParams();
                if (currentLp != null) {
                    View parent = (View) holder.itemView.getParent();
                    int parentHeight = 0;
                    if (parent != null) {
                        parentHeight = parent.getHeight();
                    }
                    if (parentHeight <= 0) {
                        parentHeight = holder.itemView.getContext().getResources().getDisplayMetrics().heightPixels - 250;
                    }
                    
                    int targetHeight;
                    if (totalItems == 1) {
                        targetHeight = parentHeight;
                    } else if (totalItems == 2) {
                        targetHeight = parentHeight / 2;
                    } else if (totalItems == 3 || totalItems == 4) {
                        targetHeight = parentHeight / 2;
                    } else {
                        targetHeight = parentHeight / 3;
                    }
                    
                    if (currentLp.height != targetHeight) {
                        currentLp.height = targetHeight;
                        holder.itemView.setLayoutParams(currentLp);
                    }
                }
            });
        }

        // Hiển thị tên
        holder.tvParticipantName.setText(p.getName());

        // Xử lý bật/tắt Video
        if (p.hasVideo()) {
            holder.videoContainer.setVisibility(View.VISIBLE);
            holder.avatarContainer.setVisibility(View.GONE);
            holder.videoContainer.removeAllViews();
            if (videoBinder != null) {
                videoBinder.bindVideo(p, holder.videoContainer);
            }
        } else {
            holder.videoContainer.setVisibility(View.GONE);
            holder.videoContainer.removeAllViews();
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

        // Xử lý Giơ tay
        if (p.isHandRaised() && !p.isScreenSharing()) {
            holder.ivHandRaised.setVisibility(View.VISIBLE);
        } else {
            holder.ivHandRaised.setVisibility(View.GONE);
        }

        String displayName;
        if (p.isScreenSharing()) {
            displayName = p.isLocal() ? "You are presenting" : p.getName();
        } else {
            displayName = p.isLocal() ? p.getName() + " (You)" : p.getName();
        }
        holder.tvParticipantName.setText(displayName);
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
        ImageView ivHandRaised;
        View viewActiveSpeakerBorder;

        public ViewHolder(View itemView) {
            super(itemView);
            // Ánh xạ id từ item_participant_video.xml
            videoContainer = itemView.findViewById(R.id.videoContainer);
            avatarContainer = itemView.findViewById(R.id.avatarContainer);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
            tvParticipantName = itemView.findViewById(R.id.tvParticipantName);
            ivMicOff = itemView.findViewById(R.id.ivMicOff);
            ivHandRaised = itemView.findViewById(R.id.ivHandRaised);
            viewActiveSpeakerBorder = itemView.findViewById(R.id.viewActiveSpeakerBorder);
        }
    }
}
