package com.hafiztraveltours.app;

import com.google.gson.annotations.SerializedName;

public class UmrahPackage {
    @SerializedName("id")
    public String id;

    @SerializedName("name")
    public String name;

    @SerializedName("duration_days")
    public int durationDays;

    @SerializedName("nights_count")
    public int nightsCount;

    @SerializedName("price_formatted")
    public String price;

    @SerializedName("starting_price")
    public String startingPrice;

    @SerializedName("url")
    public String url;

    @SerializedName("image_url")
    public String imageUrl;

    @SerializedName("category")
    public String category;

    @SerializedName("destination")
    public String destination;

    @SerializedName("summary")
    public String summary;

    @SerializedName("is_featured")
    public boolean isFeatured;

    /** "umrah" atau "tour" / "umrah_packages" atau "tour_packages" */
    public String collectionName;

    public UmrahPackage() {}

    public UmrahPackage(String id, String name, int durationDays, int nightsCount,
                        String price, String url, String imageUrl) {
        this.id = id;
        this.name = name;
        this.durationDays = durationDays;
        this.nightsCount = nightsCount;
        this.price = price;
        this.url = url;
        this.imageUrl = imageUrl;
    }
}