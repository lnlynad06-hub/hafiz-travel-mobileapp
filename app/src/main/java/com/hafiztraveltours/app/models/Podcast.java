package com.hafiztraveltours.app.models;

import com.hafiztraveltours.app.R;
import com.hafiztraveltours.app.models.*;
import com.hafiztraveltours.app.adapters.*;
import com.hafiztraveltours.app.network.*;
import com.hafiztraveltours.app.services.*;
import com.hafiztraveltours.app.utils.*;
import com.hafiztraveltours.app.views.*;
import com.hafiztraveltours.app.ui.*;


public class Podcast {
    public String title;
    public String videoId;

    public Podcast(String title, String videoId) {
        this.title = title;
        this.videoId = videoId;
    }
}