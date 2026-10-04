package com.hafiztraveltours.app.ui;

import android.content.Intent;
import android.os.Bundle;

public class TentangKamiActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, AboutActivity.class));
        finish();
    }
}
