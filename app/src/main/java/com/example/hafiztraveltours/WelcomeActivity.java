package com.example.hafiztraveltours;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        ImageView imageLeft = findViewById(R.id.imageLeft);
        ImageView imageCenter = findViewById(R.id.imageCenter);
        ImageView imageRight = findViewById(R.id.imageRight);

        // Floating/bounce loop, each image starts at a slightly different time
        // so they don't move perfectly in sync (feels more "alive")
        startFloatingLoop(imageLeft, 0);
        startFloatingLoop(imageCenter, 150);
        startFloatingLoop(imageRight, 300);

        findViewById(R.id.getStartedButton).setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, MainActivity.class))
        );

        findViewById(R.id.loginLinkText).setOnClickListener(v ->
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class))
        );
    }

    /**
     * Moves the view up and down gently, forever, like it's floating.
     * startDelayMs staggers each image so the group doesn't bounce in perfect unison.
     */
    private void startFloatingLoop(ImageView view, int startDelayMs) {
        ObjectAnimator floatAnim = ObjectAnimator.ofFloat(view, "translationY", 0f, -18f);
        floatAnim.setDuration(900);
        floatAnim.setStartDelay(startDelayMs);
        floatAnim.setRepeatCount(ValueAnimator.INFINITE);
        floatAnim.setRepeatMode(ValueAnimator.REVERSE);
        floatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        floatAnim.start();
    }
}
