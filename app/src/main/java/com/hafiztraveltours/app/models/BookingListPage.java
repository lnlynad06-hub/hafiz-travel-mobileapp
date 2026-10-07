package com.hafiztraveltours.app.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class BookingListPage {
    @SerializedName("current_page")
    public int currentPage;

    @SerializedName("data")
    public List<BookingDto> data;

    @SerializedName("total")
    public int total;

    @SerializedName("last_page")
    public int lastPage;
}
