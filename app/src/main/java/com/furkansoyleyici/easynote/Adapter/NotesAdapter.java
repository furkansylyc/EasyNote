package com.furkansoyleyici.easynote.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.content.SharedPreferences;
import com.furkansoyleyici.easynote.Activity.updateNotesActivity;
import com.furkansoyleyici.easynote.MainActivity;
import com.furkansoyleyici.easynote.Model.Notes;
import com.furkansoyleyici.easynote.R;
import com.furkansoyleyici.easynote.Firebase.FirestoreManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.notesViewHolder> {

     private FirestoreManager firestoreManager;
     MainActivity mainActivity;
     List<Notes> notes;
     SharedPreferences sharedPreferences;
     SharedPreferences.Editor editor;
     boolean isSelectionMode = false;
     
          private static final int[] PASTEL_COLORS = {
         0xFFFFD1DC,
         0xFFFFF3B0,
         0xFFAEDFF7,
         0xFFC1F0C1,
         0xFFFFD6A5,
         0xFFB5EAD7,
         0xFFDEF1FF,
         0xFFFFE0AC,
         0xFFE2F0CB,
         0xFFFDCBFA,
         0xFFFFC9C9,
         0xFFCBC3E3,
         0xFFFFABAB,
         0xFF9EE0F6,
         0xFFF7DAD9,
         0xFFBDE0FE
     };


    public NotesAdapter(@NonNull MainActivity mainActivity, List<Notes> notes) {
        this.mainActivity = mainActivity;
        this.notes = notes != null ? notes : new ArrayList<>();
        sharedPreferences = mainActivity.getSharedPreferences("favorites", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        this.firestoreManager = new FirestoreManager();
    }

    public void searchNotes(List<Notes> filterredName){
        this.notes = filterredName != null ? filterredName : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void updateNotes(List<Notes> newNotes) {
        this.notes = newNotes != null ? newNotes : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public notesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new notesViewHolder(LayoutInflater.from(mainActivity)
                .inflate(R.layout.item_notes, parent, false));
    }


    @Override
    public void onBindViewHolder(@NonNull notesViewHolder holder, int position) {
        if (notes == null || position >= notes.size()) {
            return;
        }
        Notes note = notes.get(position);
        if (note == null) {
            return;
        }
        
        // Rastgele pastel renk seçimi için backgroundColorIndex kullan
        int colorIndex = 0;
        if (note.backgroundColorIndex >= 0 && note.backgroundColorIndex < 8) {
            colorIndex = note.backgroundColorIndex;
        }
        int pastelColor = PASTEL_COLORS[colorIndex];
        holder.cardView.setCardBackgroundColor(pastelColor);

        boolean isFavorite = note.isFavorite == 1;
        holder.favoriteButton.setImageResource(isFavorite ? R.drawable.ic_favorite : R.drawable.fav);

        holder.favoriteButton.setOnClickListener(v -> {
            v.setEnabled(false);
            try {
                // firestoreId null ise favori işlemi yapma
                if (note.firestoreId == null) {
                    v.setEnabled(true);
                    return;
                }
                note.isFavorite = isFavorite ? 0 : 1;
                holder.favoriteButton.setImageResource(note.isFavorite == 1 ? R.drawable.ic_favorite : R.drawable.fav);
                notifyItemChanged(holder.getAdapterPosition());

                String firestoreId = note.firestoreId;
                if (firestoreId != null && !firestoreId.isEmpty()) {
                    firestoreManager.updateNote(firestoreId, note, new FirestoreManager.FirestoreCallback() {
                        @Override
                        public void onSuccess(Object result) {

                        }
                        @Override
                        public void onFailure(String error) {

                        }
                    });
                }
            } finally {
                v.setEnabled(true);
            }
        });



        switch (note.notesPriority) {
            case "1":
                holder.notesPriority.setBackgroundResource(R.drawable.green_tag);
                break;
            case "2":
                holder.notesPriority.setBackgroundResource(R.drawable.yellow_tag);
                break;
            case "3":
                holder.notesPriority.setBackgroundResource(R.drawable.red_tag);
                break;
        }


        holder.title.setText(note.notesTitle);
        holder.subtitle.setText(note.notesSubtitle);
        holder.notesDate.setText(note.notesDate);

        if (note.hasLocationReminder) {
            holder.notesLocation.setVisibility(View.VISIBLE);
        } else {
            holder.notesLocation.setVisibility(View.GONE);
        }

        if (note.hasAlarm) {
            holder.notesAlarm.setVisibility(View.VISIBLE);
        } else {
            holder.notesAlarm.setVisibility(View.GONE);
        }


        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                mainActivity.toggleNoteSelection(note);
                updateSelectionUI(holder, note);
            } else {
                Intent intent = new Intent(mainActivity, updateNotesActivity.class);
                intent.putExtra("id", note.id);
                intent.putExtra("title", note.notesTitle);
                intent.putExtra("subtitle", note.notesSubtitle);
                intent.putExtra("priority", note.notesPriority);
                intent.putExtra("notes", note.notes);
                intent.putExtra("latitude", note.latitude);
                intent.putExtra("longitude", note.longitude);
                intent.putExtra("locationName", note.locationName);
                intent.putExtra("hasLocationReminder", note.hasLocationReminder);
                intent.putExtra("alarmTimes", note.alarmTimes);
                intent.putExtra("hasAlarm", note.hasAlarm);
                intent.putExtra("firestoreId", note.firestoreId); // FirestoreId eklendi
                mainActivity.startActivity(intent);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                mainActivity.enterSelectionMode();
                mainActivity.toggleNoteSelection(note);
                updateSelectionUI(holder, note);
                return true;
            }
            return false;
        });

        updateSelectionUI(holder, note);
    }

    private void updateSelectionUI(notesViewHolder holder, Notes note) {
        if (isSelectionMode) {
            holder.selectionOverlay.setVisibility(View.VISIBLE);
            if (mainActivity.isNoteSelected(note)) {
                holder.selectionOverlay.setBackgroundResource(R.drawable.selection_selected);
            } else {
                holder.selectionOverlay.setBackgroundResource(R.drawable.selection_unselected);
            }
        } else {
            holder.selectionOverlay.setVisibility(View.GONE);
        }
    }


    @Override
    public int getItemCount() {
        return notes != null ? notes.size() : 0;
    }

    static class notesViewHolder extends RecyclerView.ViewHolder {

        TextView title, subtitle, notesDate;
        View notesPriority, notesLocation, notesAlarm;
        ImageButton favoriteButton;
        androidx.cardview.widget.CardView cardView;
        View selectionOverlay;

        public notesViewHolder(View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.notesBaslik);
            subtitle = itemView.findViewById(R.id.notesAltBaslik);
            notesDate = itemView.findViewById(R.id.notesDate);
            notesPriority = itemView.findViewById(R.id.notesPriority);
            notesLocation = itemView.findViewById(R.id.notesLocation);
            notesAlarm = itemView.findViewById(R.id.notesAlarm);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
            cardView = itemView.findViewById(R.id.cardView);
            selectionOverlay = itemView.findViewById(R.id.selectionOverlay);
        }
    }


}
