package com.furkansoyleyici.easynote.Activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import androidx.appcompat.app.AppCompatDelegate;

import com.furkansoyleyici.easynote.MainActivity;
import com.furkansoyleyici.easynote.R;
import com.furkansoyleyici.easynote.Auth.FirebaseAuthManager;

public class SplashScreen extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_splash_screen);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        ImageView logo = findViewById(R.id.splashLogo);
        TextView appName = findViewById(R.id.appName);
        TextView slogan = findViewById(R.id.slogan);
        CircularProgressIndicator progress = findViewById(R.id.splashProgress);

        // Logo pulse animasyonu (sonsuz döngü)
        ObjectAnimator pulseX = ObjectAnimator.ofFloat(logo, "scaleX", 1f, 1.08f, 1f);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(logo, "scaleY", 1f, 1.08f, 1f);
        pulseX.setDuration(1200);
        pulseY.setDuration(1200);
        pulseX.setRepeatCount(ObjectAnimator.INFINITE);
        pulseY.setRepeatCount(ObjectAnimator.INFINITE);
        pulseX.start();
        pulseY.start();

        // App name typing efekti
        String appNameFull = "EasyNote";
        appName.setText("");
        Handler typingHandler = new Handler();
        for (int i = 0; i <= appNameFull.length(); i++) {
            final int idx = i;
            typingHandler.postDelayed(() -> {
                appName.setText(appNameFull.substring(0, idx));
            }, 400 + i * 80);
        }

        // Slogan ve progress animasyonları
        ObjectAnimator sloganFade = ObjectAnimator.ofFloat(slogan, "alpha", 0f, 1f);
        ObjectAnimator sloganTrans = ObjectAnimator.ofFloat(slogan, "translationY", 40f, 0f);
        AnimatorSet sloganAnim = new AnimatorSet();
        sloganAnim.playTogether(sloganFade, sloganTrans);
        sloganAnim.setDuration(600);
        sloganAnim.setStartDelay(700);

        ObjectAnimator progressFade = ObjectAnimator.ofFloat(progress, "alpha", 0f, 1f);
        ObjectAnimator progressScale = ObjectAnimator.ofFloat(progress, "scaleX", 0.7f, 1.1f, 1f);
        AnimatorSet progressAnim = new AnimatorSet();
        progressAnim.playTogether(progressFade, progressScale);
        progressAnim.setDuration(700);
        progressAnim.setStartDelay(1000);

        AnimatorSet all = new AnimatorSet();
        all.playTogether(sloganAnim, progressAnim);
        all.start();

        new Handler().postDelayed(() -> {
            FirebaseAuthManager authManager = new FirebaseAuthManager(this, new FirebaseAuthManager.AuthCallback() {
                @Override
                public void onAuthSuccess(com.google.firebase.auth.FirebaseUser user) {
                    startActivity(new Intent(SplashScreen.this, MainActivity.class));
                    finish();
                }
                @Override
                public void onAuthFailure(String error) {
                    startActivity(new Intent(SplashScreen.this, LoginActivity.class));
                    finish();
                }
                @Override
                public void onSignOut() {
                    startActivity(new Intent(SplashScreen.this, LoginActivity.class));
                    finish();
                }
            });
            if (authManager.isUserSignedIn()) {
                startActivity(new Intent(SplashScreen.this, MainActivity.class));
            } else {
                startActivity(new Intent(SplashScreen.this, LoginActivity.class));
            }
            finish();
        }, 2000);
    }
}
