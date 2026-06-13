package com.ptithcm.ptitmeet.api.dto.common;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PageResponse<T> {

    @SerializedName("content")
    private List<T> content;

    @SerializedName("number")
    private int number;

    @SerializedName("size")
    private int size;

    @SerializedName("totalPages")
    private int totalPages;

    @SerializedName("totalElements")
    private long totalElements;

    public List<T> getContent() {
        return content;
    }

    public int getNumber() {
        return number;
    }

    public int getSize() {
        return size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }
}
