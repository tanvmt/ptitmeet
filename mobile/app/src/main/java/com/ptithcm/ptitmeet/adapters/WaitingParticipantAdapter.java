package com.ptithcm.ptitmeet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.dto.meeting.ParticipantResponse;

import java.util.ArrayList;
import java.util.List;

public class WaitingParticipantAdapter extends RecyclerView.Adapter<WaitingParticipantAdapter.ViewHolder> {

    public interface OnWaitingActionClickListener {
        void onApprove(ParticipantResponse participant);
        void onReject(ParticipantResponse participant);
    }

    private final List<ParticipantResponse> items = new ArrayList<>();
    private final OnWaitingActionClickListener listener;

    public WaitingParticipantAdapter(OnWaitingActionClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ParticipantResponse> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waiting_participant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ParticipantResponse item = items.get(position);
        holder.tvWaitingName.setText(item.getDisplayName());
        
        String email = item.getEmail();
        if (email == null || email.trim().isEmpty()) {
            holder.tvWaitingEmail.setVisibility(View.GONE);
        } else {
            holder.tvWaitingEmail.setVisibility(View.VISIBLE);
            holder.tvWaitingEmail.setText(email);
        }

        String initial = item.getDisplayName() != null && !item.getDisplayName().isEmpty()
                ? item.getDisplayName().substring(0, 1).toUpperCase()
                : "U";
        holder.tvWaitingAvatarInitial.setText(initial);

        holder.btnWaitingAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApprove(item);
            }
        });

        holder.btnWaitingDecline.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvWaitingAvatarInitial;
        private final TextView tvWaitingName;
        private final TextView tvWaitingEmail;
        private final MaterialButton btnWaitingDecline;
        private final MaterialButton btnWaitingAccept;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWaitingAvatarInitial = itemView.findViewById(R.id.tvWaitingAvatarInitial);
            tvWaitingName = itemView.findViewById(R.id.tvWaitingName);
            tvWaitingEmail = itemView.findViewById(R.id.tvWaitingEmail);
            btnWaitingDecline = itemView.findViewById(R.id.btnWaitingDecline);
            btnWaitingAccept = itemView.findViewById(R.id.btnWaitingAccept);
        }
    }
}
