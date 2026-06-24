package com.ptithcm.ptitmeet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.models.ParticipantData;

import java.util.List;

public class ActiveParticipantAdapter extends RecyclerView.Adapter<ActiveParticipantAdapter.ViewHolder> {

    public interface OnActiveParticipantActionListener {
        void onMute(ParticipantData participant);
        void onStopCam(ParticipantData participant);
        void onKick(ParticipantData participant);
    }

    private List<ParticipantData> participantList;
    private final boolean isHost;
    private final OnActiveParticipantActionListener actionListener;

    public ActiveParticipantAdapter(List<ParticipantData> participantList, boolean isHost, OnActiveParticipantActionListener actionListener) {
        this.participantList = participantList;
        this.isHost = isHost;
        this.actionListener = actionListener;
    }

    public void setParticipantList(List<ParticipantData> participantList) {
        this.participantList = participantList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_active_participant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParticipantData p = participantList.get(position);

        String displayName = p.isLocal() ? p.getName() + " (You)" : p.getName();
        holder.tvName.setText(displayName);
        holder.tvIdentity.setText(p.getIdentity());

        if (p.getName() != null && !p.getName().isEmpty()) {
            holder.tvAvatarInitial.setText(String.valueOf(p.getName().charAt(0)).toUpperCase());
        } else {
            holder.tvAvatarInitial.setText("U");
        }

        if (p.isMicOn()) {
            holder.ivStatusMic.setImageResource(R.drawable.ic_mic);
            holder.ivStatusMic.setColorFilter(null);
            holder.ivStatusMic.setAlpha(1f);
        } else {
            holder.ivStatusMic.setImageResource(R.drawable.ic_mic);
            holder.ivStatusMic.setColorFilter(android.graphics.Color.parseColor("#EF4444"));
            holder.ivStatusMic.setAlpha(0.6f);
        }

        if (p.hasVideo()) {
            holder.ivStatusVideo.setImageResource(R.drawable.ic_videocam);
            holder.ivStatusVideo.setColorFilter(null);
            holder.ivStatusVideo.setAlpha(1f);
        } else {
            holder.ivStatusVideo.setImageResource(R.drawable.ic_videocam);
            holder.ivStatusVideo.setColorFilter(android.graphics.Color.parseColor("#EF4444"));
            holder.ivStatusVideo.setAlpha(0.6f);
        }

        if (isHost && !p.isLocal()) {
            holder.layoutHostActions.setVisibility(View.VISIBLE);
            holder.btnMute.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onMute(p);
                }
            });
            holder.btnStopCam.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onStopCam(p);
                }
            });
            holder.btnKick.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onKick(p);
                }
            });
        } else {
            holder.layoutHostActions.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return participantList == null ? 0 : participantList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatarInitial;
        TextView tvName;
        TextView tvIdentity;
        ImageView ivStatusMic;
        ImageView ivStatusVideo;
        LinearLayout layoutHostActions;
        MaterialButton btnMute;
        MaterialButton btnStopCam;
        MaterialButton btnKick;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
            tvName = itemView.findViewById(R.id.tvName);
            tvIdentity = itemView.findViewById(R.id.tvIdentity);
            ivStatusMic = itemView.findViewById(R.id.ivStatusMic);
            ivStatusVideo = itemView.findViewById(R.id.ivStatusVideo);
            layoutHostActions = itemView.findViewById(R.id.layoutHostActions);
            btnMute = itemView.findViewById(R.id.btnMute);
            btnStopCam = itemView.findViewById(R.id.btnStopCam);
            btnKick = itemView.findViewById(R.id.btnKick);
        }
    }
}
