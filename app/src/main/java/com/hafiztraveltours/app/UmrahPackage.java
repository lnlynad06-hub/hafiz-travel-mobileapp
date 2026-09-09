package com.hafiztraveltours.app;

public class UmrahPackage {
    public String id;
    public String name;
    public int durationDays;
    public int nightsCount;
    public String price;
    public String url;
    public String imageUrl;

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