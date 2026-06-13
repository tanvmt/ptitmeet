package com.ptithcm.ptitmeet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingHistoryResponse;

import java.util.ArrayList;
import java.util.List;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private final List<MeetingHistoryResponse> items = new ArrayList<>();

    public void submitList(List<MeetingHistoryResponse> data) {
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
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MeetingHistoryResponse item = items.get(position);
        holder.title.setText(item.getTitle());
        holder.time.setText(item.getDisplayTime());
        holder.status.setText(item.getStatus());
        holder.status.setTextColor(item.getStatusColor(holder.status));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView time;
        private final TextView status;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvRecentTitle);
            time = itemView.findViewById(R.id.tvRecentTime);
            status = itemView.findViewById(R.id.tvRecentStatus);
        }
    }
}
