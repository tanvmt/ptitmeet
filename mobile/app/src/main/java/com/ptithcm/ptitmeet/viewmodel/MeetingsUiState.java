package com.ptithcm.ptitmeet.viewmodel;

import com.ptithcm.ptitmeet.api.dto.chat.ChatMessageResponse;
import com.ptithcm.ptitmeet.api.dto.common.PageResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingHistoryResponse;

import java.util.ArrayList;
import java.util.List;

public class MeetingsUiState {
    private boolean loading;
    private String summaryText = "";
    private int currentPage = 1;
    private int totalPages = 1;
    private List<MeetingHistoryResponse> meetings = new ArrayList<>();
    private String joinDisplayName = "User";
    private String chatDialogTitle;
    private List<ChatMessageResponse> chatHistory;

    public MeetingsUiState() {
    }

    public MeetingsUiState(MeetingsUiState other) {
        this.loading = other.loading;
        this.summaryText = other.summaryText;
        this.currentPage = other.currentPage;
        this.totalPages = other.totalPages;
        this.meetings = new ArrayList<>(other.meetings);
        this.joinDisplayName = other.joinDisplayName;
        this.chatDialogTitle = other.chatDialogTitle;
        this.chatHistory = other.chatHistory == null ? null : new ArrayList<>(other.chatHistory);
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<MeetingHistoryResponse> getMeetings() {
        return meetings;
    }

    public void setMeetings(List<MeetingHistoryResponse> meetings) {
        this.meetings = new ArrayList<>(meetings);
    }

    public String getJoinDisplayName() {
        return joinDisplayName;
    }

    public void setJoinDisplayName(String joinDisplayName) {
        this.joinDisplayName = joinDisplayName;
    }

    public String getChatDialogTitle() {
        return chatDialogTitle;
    }

    public void setChatDialogTitle(String chatDialogTitle) {
        this.chatDialogTitle = chatDialogTitle;
    }

    public List<ChatMessageResponse> getChatHistory() {
        return chatHistory;
    }

    public void setChatHistory(List<ChatMessageResponse> chatHistory) {
        this.chatHistory = chatHistory == null ? null : new ArrayList<>(chatHistory);
    }
}
