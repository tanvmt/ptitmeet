package com.ptithcm.ptitmeet.api.dto.meeting;

public class FeedbackRequest {

    private final int rating;

    public FeedbackRequest(int rating) {
        this.rating = rating;
    }

    public int getRating() {
        return rating;
    }
}
