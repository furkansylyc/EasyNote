package com.furkansoyleyici.easynote.Activity;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.furkansoyleyici.easynote.BroadcastReceiver.ReminderReceiver;
import com.furkansoyleyici.easynote.Firebase.FirestoreManager;
import com.furkansoyleyici.easynote.Model.Notes;
import com.furkansoyleyici.easynote.R;
import com.furkansoyleyici.easynote.databinding.ActivityUpdateNotesBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class updateNotesActivity extends BaseActivity {

    ActivityUpdateNotesBinding binding;
    String priority = "3";
    String uptitle, upsubtitle, upnotes, uppriority;
    int upid;

    FloatingActionButton menuButton, alarmButton, locationButton, updateNotesButton;
    LinearLayout fabMenu;
    android.widget.ImageButton removeLocationButton, removeAllAlarmsButton;
    android.widget.TextView locationNameText, locationAddressText;
    androidx.cardview.widget.CardView locationCard, alarmCard;
    android.widget.LinearLayout alarmListContainer;
    double selectedLatitude = 0.0;
    double selectedLongitude = 0.0;
    String selectedLocationName = "";
    boolean hasLocationReminder = false;
    java.util.List<Long> selectedAlarmTimes = new java.util.ArrayList<>();
    boolean hasAlarm = false;
    boolean isMenuOpen = false;
    private static final int LOCATION_PICKER_REQUEST_CODE = 1001;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdateNotesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        upid = getIntent().getIntExtra("id", 0);
        uptitle = getIntent().getStringExtra("title");
        upsubtitle = getIntent().getStringExtra("subtitle");
        upnotes = getIntent().getStringExtra("notes");
        uppriority = getIntent().getStringExtra("priority");
        selectedLatitude = getIntent().getDoubleExtra("latitude", 0.0);
        selectedLongitude = getIntent().getDoubleExtra("longitude", 0.0);
        selectedLocationName = getIntent().getStringExtra("locationName");
        hasLocationReminder = getIntent().getBooleanExtra("hasLocationReminder", false);
        String alarmTimesJson = getIntent().getStringExtra("alarmTimes");
        if (alarmTimesJson != null && !alarmTimesJson.isEmpty()) {
            try {
                org.json.JSONArray jsonArray = new org.json.JSONArray(alarmTimesJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    selectedAlarmTimes.add(jsonArray.getLong(i));
                }
                hasAlarm = !selectedAlarmTimes.isEmpty();
            } catch (Exception e) {
                selectedAlarmTimes.clear();
                hasAlarm = false;
            }
        }

        menuButton = findViewById(R.id.menuButton);
        alarmButton = findViewById(R.id.alarmButton);
        updateNotesButton = findViewById(R.id.updateNotesButton);
        locationButton = findViewById(R.id.locationButton);
        fabMenu = findViewById(R.id.fabMenu);
        
        locationCard = findViewById(R.id.locationCard);
        locationNameText = findViewById(R.id.locationNameText);
        locationAddressText = findViewById(R.id.locationAddressText);
        removeLocationButton = findViewById(R.id.removeLocationButton);
        
        alarmCard = findViewById(R.id.alarmCard);
        alarmListContainer = findViewById(R.id.alarmListContainer);
        removeAllAlarmsButton = findViewById(R.id.removeAllAlarmsButton);

        alarmButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (!alarmManager.canScheduleExactAlarms()) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intent);
                }
            }
            showDateTimePicker();
        });

        locationButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationPickerActivity.class);
            startActivityForResult(intent, LOCATION_PICKER_REQUEST_CODE);
        });

        removeLocationButton.setOnClickListener(v -> {
            selectedLatitude = 0.0;
            selectedLongitude = 0.0;
            selectedLocationName = "";
            hasLocationReminder = false;
            locationCard.setVisibility(View.GONE);
            
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));
            TextView toastText = layout.findViewById(R.id.custom_toast_message);
            toastText.setText("Konum hatırlatıcısı kaldırıldı");

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();
        });

        removeAllAlarmsButton.setOnClickListener(v -> {
            selectedAlarmTimes.clear();
            hasAlarm = false;
            alarmCard.setVisibility(View.GONE);
            updateAlarmList();
            
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));
            TextView toastText = layout.findViewById(R.id.custom_toast_message);
            toastText.setText("Tüm alarmlar kaldırıldı");

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();
        });

        binding.updateTitle.setText(uptitle);
        binding.updateSubtitle.setText(upsubtitle);
        binding.updateNotes.setText(upnotes);

        if (hasLocationReminder && !selectedLocationName.isEmpty()) {
            locationCard.setVisibility(View.VISIBLE);
            locationNameText.setText("Konum Seçildi");
            locationAddressText.setText(selectedLocationName);
        }

        if (hasAlarm && !selectedAlarmTimes.isEmpty()) {
            alarmCard.setVisibility(View.VISIBLE);
            updateAlarmList();
        }

        switch (uppriority) {
            case "1":
                binding.greenTag.setImageResource(R.drawable.done);
                binding.yellowTag.setImageResource(0);
                binding.redTag.setImageResource(0);
                priority = "1";
                break;
            case "2":
                binding.greenTag.setImageResource(0);
                binding.yellowTag.setImageResource(R.drawable.done);
                binding.redTag.setImageResource(0);
                priority = "2";
                break;
            case "3":
                binding.greenTag.setImageResource(0);
                binding.yellowTag.setImageResource(0);
                binding.redTag.setImageResource(R.drawable.done);
                priority = "3";
                break;
        }

        firestoreManager = new FirestoreManager();

        binding.greenTag.setOnClickListener(v -> updatePriority("1"));
        binding.yellowTag.setOnClickListener(v -> updatePriority("2"));
        binding.redTag.setOnClickListener(v -> updatePriority("3"));

        updateNotesButton.setOnClickListener(v -> {
            String title = binding.updateTitle.getText().toString();
            String subtitle = binding.updateSubtitle.getText().toString();
            String notes = binding.updateNotes.getText().toString();
            UpdateNotes(title, subtitle, notes);
        });

        menuButton.setOnClickListener(v -> toggleFabMenu());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOCATION_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedLatitude = data.getDoubleExtra("latitude", 0.0);
            selectedLongitude = data.getDoubleExtra("longitude", 0.0);
            selectedLocationName = data.getStringExtra("locationName");
            hasLocationReminder = true;

            locationCard.setVisibility(View.VISIBLE);
            locationNameText.setText("Konum Seçildi");
            locationAddressText.setText(selectedLocationName);

            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));
            TextView toastText = layout.findViewById(R.id.custom_toast_message);
            toastText.setText(getString(R.string.location_selected, selectedLocationName));

            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();


            autoUpdateNote();
        }
    }

    private void showDateTimePicker() {
        final Calendar currentDate = Calendar.getInstance();
        final Calendar date = Calendar.getInstance();

        new DatePickerDialog(updateNotesActivity.this, (view, year, monthOfYear, dayOfMonth) -> {
            date.set(year, monthOfYear, dayOfMonth);

            new TimePickerDialog(updateNotesActivity.this, (view1, hourOfDay, minute) -> {
                date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                date.set(Calendar.MINUTE, minute);
                date.set(Calendar.SECOND, 0);

                selectedAlarmTimes.add(date.getTimeInMillis());
                hasAlarm = true;

                alarmCard.setVisibility(View.VISIBLE);
                updateAlarmList();

                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));
                TextView toastText = layout.findViewById(R.id.custom_toast_message);
                toastText.setText(getString(R.string.alarm_set));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();




                autoUpdateNote();

            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show();

        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
    }


    private void autoUpdateNote() {
        String title = binding.updateTitle.getText().toString();
        String subtitle = binding.updateSubtitle.getText().toString();
        String notes = binding.updateNotes.getText().toString();


        if (title.isEmpty()) {
            title = getString(R.string.default_reminder_title);
            binding.updateTitle.setText(title);
        }
        if (notes.isEmpty()) {
            notes = getString(R.string.default_alarm_note);
            binding.updateNotes.setText(notes);
        }


        UpdateNotesWithAlarms(title, subtitle, notes);
        

        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));
        TextView toastText = layout.findViewById(R.id.custom_toast_message);
        toastText.setText(getString(R.string.auto_save_success));

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }


    private void UpdateNotesWithAlarms(String title, String subtitle, String notes) {
        Date date = new Date();
        CharSequence sequence = DateFormat.format("dd/MM/yyyy", date.getTime());

        Notes updateNotes = new Notes();
        updateNotes.id = upid;
        updateNotes.notesTitle = title;
        updateNotes.notesSubtitle = subtitle;
        updateNotes.notes = notes;
        updateNotes.notesPriority = priority;
        updateNotes.notesDate = sequence.toString();

        updateNotes.latitude = selectedLatitude;
        updateNotes.longitude = selectedLongitude;
        updateNotes.locationName = selectedLocationName;
        updateNotes.hasLocationReminder = hasLocationReminder;
        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray();
            for (Long alarmTime : selectedAlarmTimes) {
                jsonArray.put(alarmTime);
            }
            updateNotes.alarmTimes = jsonArray.toString();
        } catch (Exception e) {
            updateNotes.alarmTimes = "[]";
        }
        updateNotes.hasAlarm = hasAlarm;

        SharedPreferences sharedPreferences = getSharedPreferences("EasyNotePrefs", MODE_PRIVATE);
        Set<String> favoriteIds = new HashSet<>(sharedPreferences.getStringSet("favorite_notes", new HashSet<>()));

        if (favoriteIds.contains(String.valueOf(upid))) {
            favoriteIds.remove(String.valueOf(upid));
        } else {
            favoriteIds.add(String.valueOf(upid));
        }

        sharedPreferences.edit().putStringSet("favorite_notes", favoriteIds).apply();
        firestoreManager.updateNote(getIntent().getStringExtra("firestoreId"), updateNotes, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                runOnUiThread(() -> {
                    Toast.makeText(updateNotesActivity.this, "Not güncellendi!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(updateNotesActivity.this, "Güncelleme hatası: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
        

        setupAlarms(updateNotes.notesTitle, updateNotes.notes, updateNotes.id);
    }


    private void setupAlarms(String title, String message, int noteId) {
        for (Long alarmTime : selectedAlarmTimes) {
            Intent intent = new Intent(getApplicationContext(), ReminderReceiver.class);
            intent.putExtra("title", title);
            intent.putExtra("message", message);
            intent.putExtra("noteId", noteId);

            int requestCode = (int) System.currentTimeMillis();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    getApplicationContext(),
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                );
            }
        }
    }

    private void updatePriority(String priorityValue) {
        priority = priorityValue;
        binding.greenTag.setImageResource(priority.equals("1") ? R.drawable.done : 0);
        binding.yellowTag.setImageResource(priority.equals("2") ? R.drawable.done : 0);
        binding.redTag.setImageResource(priority.equals("3") ? R.drawable.done : 0);
    }

    private void toggleFabMenu() {
        if (isMenuOpen) {
            fabMenu.animate().alpha(0f).translationY(100).setDuration(300).withEndAction(() -> fabMenu.setVisibility(View.GONE)).start();
            isMenuOpen = false;
        } else {
            fabMenu.setVisibility(View.VISIBLE);
            fabMenu.setAlpha(0f);
            fabMenu.setTranslationY(100);
            fabMenu.animate().alpha(1f).translationY(0).setDuration(300).start();
            isMenuOpen = true;
        }
    }

    private void UpdateNotes(String title, String subtitle, String notes) {
        Date date = new Date();
        CharSequence sequence = DateFormat.format("dd/MM/yyyy", date.getTime());

        Notes updateNotes = new Notes();
        updateNotes.id = upid;
        updateNotes.notesTitle = title;
        updateNotes.notesSubtitle = subtitle;
        updateNotes.notes = notes;
        updateNotes.notesPriority = priority;
        updateNotes.notesDate = sequence.toString();

        updateNotes.latitude = selectedLatitude;
        updateNotes.longitude = selectedLongitude;
        updateNotes.locationName = selectedLocationName;
        updateNotes.hasLocationReminder = hasLocationReminder;
        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray();
            for (Long alarmTime : selectedAlarmTimes) {
                jsonArray.put(alarmTime);
            }
            updateNotes.alarmTimes = jsonArray.toString();
        } catch (Exception e) {
            updateNotes.alarmTimes = "[]";
        }
        updateNotes.hasAlarm = hasAlarm;

        String firestoreId = getIntent().getStringExtra("firestoreId");
        if (firestoreId != null && !firestoreId.isEmpty()) {
            firestoreManager.updateNote(firestoreId, updateNotes, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(Object result) {
                    runOnUiThread(() -> {
                        Toast.makeText(updateNotesActivity.this, "Not güncellendi!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(updateNotesActivity.this, "Güncelleme hatası: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        } else {
            Toast.makeText(this, "Not güncellenemedi (firestoreId yok)", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.delete_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.done) {

            String title = binding.updateTitle.getText().toString();
            String subtitle = binding.updateSubtitle.getText().toString();
            String notes = binding.updateNotes.getText().toString();
            UpdateNotes(title, subtitle, notes);
        } else if (item.getItemId() == R.id.ic_delete) {
            BottomSheetDialog sheetDialog = new BottomSheetDialog(updateNotesActivity.this, R.style.BottomSheetStyle);

            View view = LayoutInflater.from(updateNotesActivity.this)
                    .inflate(R.layout.delete_screen, (LinearLayout) findViewById(R.id.delete_panel));

            sheetDialog.setContentView(view);

            View deletePanel = view.findViewById(R.id.delete_panel);
            deletePanel.setAlpha(0f);
            deletePanel.setTranslationY(300);

            deletePanel.animate()
                    .alpha(1f)
                    .translationY(0)
                    .setDuration(650)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .start();

            sheetDialog.show();

            TextView yes, no;
            yes = view.findViewById(R.id.delete_yes);
            no = view.findViewById(R.id.delete_no);

            yes.setOnClickListener(v -> {
                Notes noteToDelete = new Notes();
                noteToDelete.id = upid;
                noteToDelete.notesTitle = uptitle;
                noteToDelete.notesSubtitle = upsubtitle;
                noteToDelete.notes = upnotes;
                noteToDelete.notesPriority = uppriority;
                noteToDelete.latitude = selectedLatitude;
                noteToDelete.longitude = selectedLongitude;
                noteToDelete.locationName = selectedLocationName;
                noteToDelete.hasLocationReminder = hasLocationReminder;

                noteToDelete.firestoreId = getIntent().getStringExtra("firestoreId");
                firestoreManager.deleteNote(noteToDelete.firestoreId, new FirestoreManager.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        runOnUiThread(() -> {
                            Toast.makeText(updateNotesActivity.this, "Not silindi!", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(updateNotesActivity.this, "Silme hatası: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
                sheetDialog.dismiss();
                finish();
            });

            no.setOnClickListener(v -> {
                deletePanel.animate()
                        .alpha(0f)
                        .translationY(300)
                        .setDuration(400)
                        .setInterpolator(new android.view.animation.AccelerateInterpolator())
                        .withEndAction(sheetDialog::dismiss)
                        .start();
            });
        }
        return true;
    }

    private void updateAlarmList() {
        alarmListContainer.removeAllViews();
        
        for (int i = 0; i < selectedAlarmTimes.size(); i++) {
            final int index = i;
            final long alarmTime = selectedAlarmTimes.get(i);
            
            LinearLayout alarmItemLayout = new LinearLayout(this);
            alarmItemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            alarmItemLayout.setOrientation(LinearLayout.HORIZONTAL);
            alarmItemLayout.setPadding(0, 8, 0, 8);
            alarmItemLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView alarmTimeText = new TextView(this);
            alarmTimeText.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
            String alarmDateTime = dateFormat.format(new java.util.Date(alarmTime));
            alarmTimeText.setText(alarmDateTime);
            alarmTimeText.setTextSize(14);
            alarmTimeText.setTextColor(getResources().getColor(android.R.color.black));

            ImageButton removeButton = new ImageButton(this);
            removeButton.setLayoutParams(new LinearLayout.LayoutParams(
                    48, 48));
            removeButton.setImageResource(R.drawable.delete);
            removeButton.setBackgroundResource(android.R.drawable.btn_default_small);
            removeButton.setOnClickListener(v -> {
                selectedAlarmTimes.remove(index);
                if (selectedAlarmTimes.isEmpty()) {
                    hasAlarm = false;
                    alarmCard.setVisibility(View.GONE);
                }
                updateAlarmList();
            });

            alarmItemLayout.addView(alarmTimeText);
            alarmItemLayout.addView(removeButton);
            alarmListContainer.addView(alarmItemLayout);
        }
    }
}
