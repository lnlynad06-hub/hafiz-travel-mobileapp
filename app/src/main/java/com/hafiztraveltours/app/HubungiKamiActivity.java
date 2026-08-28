package com.hafiztraveltours.app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HubungiKamiActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applySavedLocale(newBase));
    }

    private static final String PHONE_NUMBER = "+6019-785 9867";
    private static final String PHONE_DIAL_URI = "tel:+60197859867";
    private static final String EMAIL_ADDRESS = "sales.httsb@gmail.com.my";
    private static final String ADDRESS = "No. 31, Jalan Dataran Larkin,\n80350 Johor Bahru, Johor.";
    private static final String WHATSAPP_URL = "https://wa.me/60197859867";

    private static final String FACEBOOK_URL = "https://www.facebook.com/hafiztravelntours";
    private static final String INSTAGRAM_URL = "https://www.instagram.com/hafiztravelofficial/";
    private static final String TIKTOK_URL = "https://www.tiktok.com/@hafiztravelofficial";
    private static final String YOUTUBE_URL = "https://www.youtube.com/@hafiztravelandtours";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hubungi_kami);

        findViewById(R.id.hubungiBackButton).setOnClickListener(v -> finish());

        findViewById(R.id.addressRow).setOnClickListener(v -> {
            try {
                Uri mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(ADDRESS));
                startActivity(new Intent(Intent.ACTION_VIEW, mapUri));
            } catch (Exception e) {
                Toast.makeText(this, "Tidak dapat membuka peta", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.phoneRow).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(PHONE_DIAL_URI)));
            } catch (Exception e) {
                Toast.makeText(this, "Tidak dapat membuat panggilan", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.emailRow).setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + EMAIL_ADDRESS));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Tidak dapat membuka emel", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.whatsappButton).setOnClickListener(v -> openLink(WHATSAPP_URL));

        findViewById(R.id.socialFacebook).setOnClickListener(v -> openLink(FACEBOOK_URL));
        findViewById(R.id.socialInstagram).setOnClickListener(v -> openLink(INSTAGRAM_URL));
        findViewById(R.id.socialTiktok).setOnClickListener(v -> openLink(TIKTOK_URL));
        findViewById(R.id.socialYoutube).setOnClickListener(v -> openLink(YOUTUBE_URL));
    }

    private void openLink(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Tidak dapat membuka pautan", Toast.LENGTH_SHORT).show();
        }
    }
}
