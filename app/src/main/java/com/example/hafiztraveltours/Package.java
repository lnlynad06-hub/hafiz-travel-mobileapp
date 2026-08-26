package com.example.hafiztraveltours;

public class Package {
    public String title;
    public String subtitle;
    public String url;
    public int imageResId; // <--- Medan baru untuk gambar local drawable

    public Package(String title, String subtitle, String url, int imageResId) {
        this.title = title;
        this.subtitle = subtitle;
        this.url = url;
        this.imageResId = imageResId;
    }
}