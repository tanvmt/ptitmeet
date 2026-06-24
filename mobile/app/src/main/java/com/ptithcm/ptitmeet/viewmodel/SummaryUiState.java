package com.ptithcm.ptitmeet.viewmodel;

public class SummaryUiState {
    private String subtitle;
    private String participantsText = "--";
    private String messagesText = "--";
    private boolean ratingSubmitted;
    private int selectedRating;

    public SummaryUiState() {
    }

    public SummaryUiState(SummaryUiState other) {
        this.subtitle = other.subtitle;
        this.participantsText = other.participantsText;
        this.messagesText = other.messagesText;
        this.ratingSubmitted = other.ratingSubmitted;
        this.selectedRating = other.selectedRating;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getParticipantsText() {
        return participantsText;
    }

    public void setParticipantsText(String participantsText) {
        this.participantsText = participantsText;
    }

    public String getMessagesText() {
        return messagesText;
    }

    public void setMessagesText(String messagesText) {
        this.messagesText = messagesText;
    }

    public boolean isRatingSubmitted() {
        return ratingSubmitted;
    }

    public void setRatingSubmitted(boolean ratingSubmitted) {
        this.ratingSubmitted = ratingSubmitted;
    }

    public int getSelectedRating() {
        return selectedRating;
    }

    public void setSelectedRating(int selectedRating) {
        this.selectedRating = selectedRating;
    }
}
