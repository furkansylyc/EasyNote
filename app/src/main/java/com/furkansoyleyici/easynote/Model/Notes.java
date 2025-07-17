package com.furkansoyleyici.easynote.Model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Notes_Database")
public class Notes {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "notes_title")
    public String notesTitle;

    @ColumnInfo(name = "notes_subtitle")
    public String notesSubtitle;


    @ColumnInfo(name = "notes")
    public String notes;
    @ColumnInfo(name = "notes_date")
    public String notesDate;

    @ColumnInfo(name = "notes_priority")
    public String notesPriority;

    @ColumnInfo(name = "isFavorite")
    public int isFavorite;

    @ColumnInfo(name = "latitude")
    public double latitude;

    @ColumnInfo(name = "longitude")
    public double longitude;

    @ColumnInfo(name = "location_name")
    public String locationName;

    @ColumnInfo(name = "has_location_reminder")
    public boolean hasLocationReminder;

    @ColumnInfo(name = "alarm_times")
    public String alarmTimes;

    @ColumnInfo(name = "has_alarm")
    public boolean hasAlarm;

    @ColumnInfo(name = "firestore_id")
    public String firestoreId;

    @ColumnInfo(name = "background_color_index")
    public int backgroundColorIndex = 0;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notes notes = (Notes) o;
        // Sadece id ile karşılaştırma yapılır, null hatası riski yoktur.
        return id == notes.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
