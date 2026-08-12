package com.example.hafiztraveltours;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupAvailablePackages();
        setupPopularPackages();
        setupFeaturesRow();
        setupBottomNav();
        setupTopBarAndChat();
    }

    private void setupAvailablePackages() {
        List<Package> items = new ArrayList<>();
        // TODO: replace this dummy data with real packages (from Firebase/Laravel API later)
        items.add(new Package("Korea Tour", "6 Hari 4 Malam"));
        items.add(new Package("Pakej Umrah Ekonomi", "12 Hari"));
        items.add(new Package("China Tour", "5 Hari 4 Malam"));

        RecyclerView recyclerView = findViewById(R.id.availablePackagesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(new PackageLargeAdapter(items));
    }

    private void setupPopularPackages() {
        List<Package> items = new ArrayList<>();
        // TODO: replace this dummy data with real packages (from Firebase/Laravel API later)
        items.add(new Package("Pakej Haji Plus", "RM45,000"));
        items.add(new Package("Jepun Tour", "RM6,800"));
        items.add(new Package("Umrah Ramadhan", "RM9,500"));

        RecyclerView recyclerView = findViewById(R.id.popularPackagesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(new PackagePopularAdapter(items));
    }

    /**
     * Builds the "Ciri-ciri" shortcut row (like Muslim Pro's feature icons).
     * Each item is just an emoji + label for now - swap the emoji for a real
     * icon drawable once you have one, and wire the click to the real screen.
     */
    private void setupFeaturesRow() {
        LinearLayout featuresRow = findViewById(R.id.featuresRow);

        String[][] features = {
                {"\uD83D\uDD4C", "Waktu Solat"},
                {"\uD83E\uDDED", "Kiblat"},
                {"\uD83D\uDD4B", "Pakej Umrah"},
                {"\u2708\uFE0F", "Pakej Haji"},
                {"\uD83C\uDF0F", "Tour Luar Negara"},
                {"\uD83C\uDFE0", "Tour Dalam Negara"},
                {"\uD83E\uDDF3", "Perjalanan Saya"},
                {"\uD83D\uDCCD", "Cawangan"}
        };

        for (String[] feature : features) {
            featuresRow.addView(buildFeatureItem(feature[0], feature[1]));
        }
    }

    private View buildFeatureItem(String emoji, String label) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, dp(16), 0);
        item.setLayoutParams(params);
        item.setClickable(true);
        item.setFocusable(true);

        TextView icon = new TextView(this);
        icon.setText(emoji);
        icon.setTextSize(20);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundResource(R.drawable.circle_bg_light);
        icon.setLayoutParams(new LinearLayout.LayoutParams(dp(52), dp(52)));

        TextView text = new TextView(this);
        text.setText(label);
        text.setTextSize(11);
        text.setGravity(Gravity.CENTER);
        text.setTextColor(getResources().getColor(R.color.text_gray));
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(dp(68), LinearLayout.LayoutParams.WRAP_CONTENT);
        textParams.topMargin = dp(6);
        text.setLayoutParams(textParams);

        item.addView(icon);
        item.addView(text);

        // TODO: replace this with real navigation per feature (Waktu Solat, Kiblat, etc.)
        item.setOnClickListener(v -> Toast.makeText(this, label + " - akan datang", Toast.LENGTH_SHORT).show());

        return item;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void setupBottomNav() {
        // Utama (Home) - this is the current screen, so it does nothing.
        // TODO: wire these to real screens once they're built (Search, My Trip, Profile).
        findViewById(R.id.navSearch).setOnClickListener(v ->
                Toast.makeText(this, "Carian - akan datang", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navMyTrip).setOnClickListener(v ->
                Toast.makeText(this, "Perjalanan Saya - akan datang", Toast.LENGTH_SHORT).show());
        findViewById(R.id.navProfile).setOnClickListener(v ->
                Toast.makeText(this, "Profil - akan datang", Toast.LENGTH_SHORT).show());
    }

    private void setupTopBarAndChat() {
        // TODO: wire this to a real language picker
        findViewById(R.id.languageButton).setOnClickListener(v ->
                Toast.makeText(this, "Tukar bahasa - akan datang", Toast.LENGTH_SHORT).show());

        // TODO: wire this to your AI chat screen
        findViewById(R.id.aiChatButton).setOnClickListener(v ->
                Toast.makeText(this, "AI Chat - akan datang", Toast.LENGTH_SHORT).show());
    }
}
