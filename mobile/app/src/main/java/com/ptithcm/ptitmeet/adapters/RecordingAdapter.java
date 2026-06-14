package com.ptithcm.ptitmeet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.dto.recording.MeetingRecordingResponse;
import com.ptithcm.ptitmeet.utils.MeetingUiFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecordingAdapter extends RecyclerView.Adapter<RecordingAdapter.ViewHolder> {

    public interface Listener {
        void onOpenRecording(MeetingRecordingResponse recording);
    }

    private final List<MeetingRecordingResponse> items = new ArrayList<>();
    private final Listener listener;

    public RecordingAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<MeetingRecordingResponse> data) {
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
                .inflate(R.layout.item_recording, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MeetingRecordingResponse item = items.get(position);
        String status = item.getStatus() == null ? "UNKNOWN" : item.getStatus().toUpperCase(Locale.ROOT);
        String title = item.getRoomName() == null || item.getRoomName().trim().isEmpty()
                ? "Meeting recording"
                : item.getRoomName();

        holder.title.setText(title);
        holder.status.setText(status);
        holder.status.setTextColor(resolveStatusColor(holder, status));
        holder.date.setText("Created: " + MeetingUiFormatter.formatDateTime(item.getCreatedAt()));
        holder.meta.setText(item.getEgressId() == null ? "Egress ID: -" : "Egress ID: " + item.getEgressId());

        boolean hasFile = item.hasFile();
        holder.openButton.setVisibility(hasFile ? View.VISIBLE : View.GONE);
        holder.openButton.setOnClickListener(v -> listener.onOpenRecording(item));
        holder.itemView.setOnClickListener(v -> {
            if (hasFile) {
                listener.onOpenRecording(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private int resolveStatusColor(ViewHolder holder, String status) {
        int colorRes;
        switch (status) {
            case "COMPLETED":
                colorRes = android.R.color.holo_green_light;
                break;
            case "FAILED":
                colorRes = android.R.color.holo_red_light;
                break;
            case "RECORDING":
            case "STARTING":
            case "STOPPING":
                colorRes = android.R.color.holo_orange_light;
                break;
            default:
                colorRes = R.color.text_secondary;
                break;
        }
        return ContextCompat.getColor(holder.itemView.getContext(), colorRes);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView status;
        private final TextView date;
        private final TextView meta;
        private final AppCompatButton openButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvRecordingTitle);
            status = itemView.findViewById(R.id.tvRecordingStatus);
            date = itemView.findViewById(R.id.tvRecordingDate);
            meta = itemView.findViewById(R.id.tvRecordingMeta);
            openButton = itemView.findViewById(R.id.btnOpenRecording);
        }
    }
}
