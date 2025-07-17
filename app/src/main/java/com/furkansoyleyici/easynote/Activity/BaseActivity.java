package com.furkansoyleyici.easynote.Activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.furkansoyleyici.easynote.Activity.LanguageSelectionActivity;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyLanguage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentLanguage = LanguageSelectionActivity.getCurrentLanguage(this);
        if (!currentLanguage.equals(getCurrentLocaleLanguage())) {
            recreate();
        }
    }

    private void applyLanguage() {
        String language = LanguageSelectionActivity.getCurrentLanguage(this);
        setLocale(language);
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private String getCurrentLocaleLanguage() {
        Locale current = getResources().getConfiguration().getLocales().get(0);
        return current.getLanguage();
    }
} 