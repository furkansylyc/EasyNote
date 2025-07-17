package com.furkansoyleyici.easynote.Activity;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.text.format.DateFormat;

import com.furkansoyleyici.easynote.BroadcastReceiver.ReminderReceiver;
import com.furkansoyleyici.easynote.Model.Notes;
import com.furkansoyleyici.easynote.R;
import com.furkansoyleyici.easynote.Firebase.FirestoreManager;
import com.furkansoyleyici.easynote.databinding.ActivityInsertNotesBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class insertNotesActivity extends BaseActivity {

    ActivityInsertNotesBinding binding;
    String title, subtitle, notes;
    String priority = "3";
    FloatingActionButton menuButton, alarmButton, locationButton, doneNotesButton;
    LinearLayout fabMenu;
    android.widget.ImageButton removeLocationButton, removeAllAlarmsButton;
    android.widget.TextView locationNameText, locationAddressText;
    androidx.cardview.widget.CardView locationCard, alarmCard;
    android.widget.LinearLayout alarmListContainer;

    boolean isMenuOpen = false;
    
    double selectedLatitude = 0.0;
    double selectedLongitude = 0.0;
    String selectedLocationName = "";
    boolean hasLocationReminder = false;
    java.util.List<Long> selectedAlarmTimes = new java.util.ArrayList<>();
    boolean hasAlarm = false;

    private static final int LOCATION_PICKER_REQUEST_CODE = 1001;
    private FirestoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInsertNotesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        menuButton = findViewById(R.id.menuButton);
        alarmButton = findViewById(R.id.alarmButton);
        locationButton = findViewById(R.id.locationButton);
        doneNotesButton = findViewById(R.id.doneNotesButton);
        fabMenu = findViewById(R.id.fabMenu);
        
        locationCard = findViewById(R.id.locationCard);
        locationNameText = findViewById(R.id.locationNameText);
        locationAddressText = findViewById(R.id.locationAddressText);
        removeLocationButton = findViewById(R.id.removeLocationButton);
        
        alarmCard = findViewById(R.id.alarmCard);
        alarmListContainer = findViewById(R.id.alarmListContainer);
        removeAllAlarmsButton = findViewById(R.id.removeAllAlarmsButton);

        menuButton.setOnClickListener(v -> {
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
        });

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

        firestoreManager = new FirestoreManager();

        binding.greenTag.setOnClickListener(v -> {
            binding.greenTag.setImageResource(R.drawable.done);
            binding.yellowTag.setImageResource(0);
            binding.redTag.setImageResource(0);
            priority = "1";
        });

        binding.yellowTag.setOnClickListener(v -> {
            binding.greenTag.setImageResource(0);
            binding.yellowTag.setImageResource(R.drawable.done);
            binding.redTag.setImageResource(0);
            priority = "2";
        });

        binding.redTag.setOnClickListener(v -> {
            binding.greenTag.setImageResource(0);
            binding.yellowTag.setImageResource(0);
            binding.redTag.setImageResource(R.drawable.done);
            priority = "3";
        });


        binding.doneNotesButton.setOnClickListener(view -> {
            title = binding.notesBaslik.getText().toString();
            subtitle = binding.notesAltBaslik.getText().toString();
            notes = binding.notesText.getText().toString();


            if (title.isEmpty() || notes.isEmpty()  ) {
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.toast_warning, findViewById(R.id.toast_layout_root));
                TextView text = layout.findViewById(R.id.toast_text);
                text.setText(getString(R.string.title_notes_required));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
            } else {
                CreateNotes(title, subtitle, notes);
            }

        });
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


            autoSaveNote();
        }
    }

    private void CreateNotes(String title, String subtitle, String notes) {
        Date date = new Date();
        CharSequence sequence = DateFormat.format("dd/MM/yyyy", date.getTime());

        Notes notes1 = new Notes();
        notes1.notesTitle = title;
        notes1.notesSubtitle = subtitle;
        notes1.notes = notes;
        notes1.notesPriority = priority;
        notes1.notesDate = sequence.toString();
        notes1.latitude = selectedLatitude;
        notes1.longitude = selectedLongitude;
        notes1.locationName = selectedLocationName;
        notes1.hasLocationReminder = hasLocationReminder;
        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray();
            for (Long alarmTime : selectedAlarmTimes) {
                jsonArray.put(alarmTime);
            }
            notes1.alarmTimes = jsonArray.toString();
        } catch (Exception e) {
            notes1.alarmTimes = "[]";
        }
        notes1.hasAlarm = hasAlarm;
        notes1.backgroundColorIndex = new Random().nextInt(8);
        firestoreManager.addNote(notes1, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                runOnUiThread(() -> {
                    LayoutInflater inflater = getLayoutInflater();
                    View layout = inflater.inflate(R.layout.toast_custom, (ViewGroup) findViewById(R.id.toast_layout_root));
                    Toast toast = new Toast(getApplicationContext());
                    toast.setDuration(Toast.LENGTH_SHORT);
                    toast.setView(layout);
                    toast.show();
                    finish();
                });
            }
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(insertNotesActivity.this, "Not eklenirken hata: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showDateTimePicker() {
        final Calendar currentDate = Calendar.getInstance();
        final Calendar date = Calendar.getInstance();

        new DatePickerDialog(insertNotesActivity.this, (view, year, monthOfYear, dayOfMonth) -> {
            date.set(year, monthOfYear, dayOfMonth);

            new TimePickerDialog(insertNotesActivity.this, (view1, hourOfDay, minute) -> {
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




                autoSaveNote();

            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show();

        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
    }


    private void autoSaveNote() {
        title = binding.notesBaslik.getText().toString();
        subtitle = binding.notesAltBaslik.getText().toString();
        notes = binding.notesText.getText().toString();


        if (title.isEmpty()) {
            title = getString(R.string.default_reminder_title);
            binding.notesBaslik.setText(title);
        }
        if (notes.isEmpty()) {
            notes = getString(R.string.default_alarm_note);
            binding.notesText.setText(notes);
        }


        CreateNotesWithAlarms(title, subtitle, notes);
        

        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, findViewById(R.id.custom_toast_container));
        TextView toastText = layout.findViewById(R.id.custom_toast_message);
        toastText.setText(getString(R.string.auto_save_success));

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }


    private void CreateNotesWithAlarms(String title, String subtitle, String notes) {
        Date date = new Date();
        CharSequence sequence = DateFormat.format("dd/MM/yyyy", date.getTime());

        Notes notes1 = new Notes();
        notes1.notesTitle = title;
        notes1.notesSubtitle = subtitle;
        notes1.notes = notes;
        notes1.notesPriority = priority;
        notes1.notesDate = sequence.toString();
        notes1.latitude = selectedLatitude;
        notes1.longitude = selectedLongitude;
        notes1.locationName = selectedLocationName;
        notes1.hasLocationReminder = hasLocationReminder;
        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray();
            for (Long alarmTime : selectedAlarmTimes) {
                jsonArray.put(alarmTime);
            }
            notes1.alarmTimes = jsonArray.toString();
        } catch (Exception e) {
            notes1.alarmTimes = "[]";
        }
        notes1.hasAlarm = hasAlarm;
        notes1.backgroundColorIndex = new Random().nextInt(8);
        firestoreManager.addNote(notes1, new FirestoreManager.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                runOnUiThread(() -> {
                    setupAlarms(notes1.notesTitle, notes1.notes, 0);
                    LayoutInflater inflater = getLayoutInflater();
                    View layout = inflater.inflate(R.layout.toast_custom, (ViewGroup) findViewById(R.id.toast_layout_root));
                    Toast toast = new Toast(getApplicationContext());
                    toast.setDuration(Toast.LENGTH_SHORT);
                    toast.setView(layout);
                    toast.show();
                    finish();
                });
            }
            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(insertNotesActivity.this, "Not eklenirken hata: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.delete_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.done) {
            title = binding.notesBaslik.getText().toString();
            subtitle = binding.notesAltBaslik.getText().toString();
            notes = binding.notesText.getText().toString();

            if (title.isEmpty() || notes.isEmpty()) {
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(R.layout.toast_warning, findViewById(R.id.toast_layout_root));
                TextView text = layout.findViewById(R.id.toast_text);
                text.setText(getString(R.string.title_notes_required));

                Toast toast = new Toast(getApplicationContext());
                toast.setDuration(Toast.LENGTH_SHORT);
                toast.setView(layout);
                toast.show();
            } else {
                CreateNotes(title, subtitle, notes);
            }
        }
        return true;
    }

}
