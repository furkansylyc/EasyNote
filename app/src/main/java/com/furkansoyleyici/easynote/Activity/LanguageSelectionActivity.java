package com.furkansoyleyici.easynote.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.furkansoyleyici.easynote.MainActivity;
import com.furkansoyleyici.easynote.R;

import java.util.Locale;

public class LanguageSelectionActivity extends BaseActivity {

    private Button btnTurkish, btnEnglish, btnGerman;
    private TextView tvTitle, tvMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);

        initViews();
        setListeners();
    }

    private void initViews() {
        btnTurkish = findViewById(R.id.btnTurkish);
        btnEnglish = findViewById(R.id.btnEnglish);
        btnGerman = findViewById(R.id.btnDeutsch);
        tvTitle = findViewById(R.id.tvTitle);
        tvMessage = findViewById(R.id.tvMessage);

        tvTitle.setText(R.string.language_selection_title);
        tvMessage.setText(R.string.language_selection_message);
        btnTurkish.setText(R.string.language_turkish);
        btnEnglish.setText(R.string.language_english);
        btnGerman.setText(R.string.language_deutsch);
    }

    private void setListeners() {
        btnTurkish.setOnClickListener(v -> setLanguage("tr"));
        btnEnglish.setOnClickListener(v -> setLanguage("en"));
        btnGerman.setOnClickListener(v -> setLanguage("de"));
    }

    private void setLanguage(String languageCode) {
        SharedPreferences prefs = getSharedPreferences("EasyNotePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("language", languageCode);
        editor.apply();

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public static void setLanguage(Context context, String languageCode) {
        SharedPreferences prefs = context.getSharedPreferences("EasyNotePrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("language", languageCode);
        editor.apply();

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    public static String getCurrentLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("EasyNotePrefs", Context.MODE_PRIVATE);
        return prefs.getString("language", "");
    }
}
