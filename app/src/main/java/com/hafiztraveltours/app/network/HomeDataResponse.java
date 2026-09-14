package com.hafiztraveltours.app.network;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.R;


import com.google.gson.annotations.SerializedName;
import com.hafiztraveltours.app.models.UmrahPackage;

import java.util.ArrayList;
import java.util.List;

public class HomeDataResponse {
    @SerializedName("featured")
    public List<UmrahPackage> featured = new ArrayList<>();

    @SerializedName("popular_umrah")
    public List<UmrahPackage> popularUmrah = new ArrayList<>();

    @SerializedName("popular_tour")
    public List<UmrahPackage> popularTour = new ArrayList<>();
}
