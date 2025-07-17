package com.furkansoyleyici.easynote.Firebase;

import android.util.Log;

import androidx.annotation.NonNull;

import com.furkansoyleyici.easynote.Model.Notes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager {
    private static final String TAG = "FirestoreManager";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    public interface FirestoreCallback {
        void onSuccess(Object result);
        void onFailure(String error);
    }

    public interface NotesCallback {
        void onNotesLoaded(List<Notes> notes);
        void onError(String error);
    }

    public FirestoreManager() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        updateUserId();
    }

    private void updateUserId() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        }
    }

    private String getUserId() {
        updateUserId();
        return userId;
    }

    private CollectionReference getNotesCollection() {
        return db.collection("users").document(getUserId()).collection("notes");
    }


    public void addNote(Notes note, FirestoreCallback callback) {
        if (getUserId() == null) {
            Log.e(TAG, "Kullanıcı giriş yapmamış - userId: " + userId);
            callback.onFailure("Kullanıcı giriş yapmamış");
            return;
        }

        Log.d(TAG, "Not ekleniyor - userId: " + getUserId());
        Log.d(TAG, "Not başlığı: " + note.notesTitle);

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("notesTitle", note.notesTitle);
        noteData.put("notesSubtitle", note.notesSubtitle);
        noteData.put("notes", note.notes);
        noteData.put("notesDate", note.notesDate);
        noteData.put("notesPriority", note.notesPriority);
        noteData.put("isFavorite", note.isFavorite);
        noteData.put("latitude", note.latitude);
        noteData.put("longitude", note.longitude);
        noteData.put("locationName", note.locationName);
        noteData.put("hasLocationReminder", note.hasLocationReminder);
        noteData.put("alarmTimes", note.alarmTimes);
        noteData.put("hasAlarm", note.hasAlarm);
        noteData.put("backgroundColorIndex", note.backgroundColorIndex);
        noteData.put("createdAt", System.currentTimeMillis());

        Log.d(TAG, "Firestore koleksiyonu: users/" + getUserId() + "/notes");

        getNotesCollection()
                .add(noteData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Not başarıyla eklendi: " + documentReference.getId());
                    Log.d(TAG, "Firestore path: " + documentReference.getPath());
                    note.firestoreId = documentReference.getId();
                    callback.onSuccess(note);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Not eklenirken hata oluştu", e);
                    Log.e(TAG, "Hata detayı: " + e.getMessage());
                    callback.onFailure(e.getMessage());
                });
    }


    public void updateNote(String noteId, Notes note, FirestoreCallback callback) {
        if (getUserId() == null) {
            callback.onFailure("Kullanıcı giriş yapmamış");
            return;
        }

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("notesTitle", note.notesTitle);
        noteData.put("notesSubtitle", note.notesSubtitle);
        noteData.put("notes", note.notes);
        noteData.put("notesDate", note.notesDate);
        noteData.put("notesPriority", note.notesPriority);
        noteData.put("isFavorite", note.isFavorite);
        noteData.put("latitude", note.latitude);
        noteData.put("longitude", note.longitude);
        noteData.put("locationName", note.locationName);
        noteData.put("hasLocationReminder", note.hasLocationReminder);
        noteData.put("alarmTimes", note.alarmTimes);
        noteData.put("hasAlarm", note.hasAlarm);
        noteData.put("backgroundColorIndex", note.backgroundColorIndex);
        noteData.put("updatedAt", System.currentTimeMillis());

        getNotesCollection().document(noteId)
                .update(noteData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Not başarıyla güncellendi");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Not güncellenirken hata oluştu", e);
                    callback.onFailure(e.getMessage());
                });
    }


    public void deleteNote(String noteId, FirestoreCallback callback) {
        Log.d(TAG, "FirestoreManager: Not silme işlemi başlatılıyor - NoteID: " + noteId);
        
        if (getUserId() == null) {
            Log.e(TAG, "FirestoreManager: Kullanıcı giriş yapmamış - userId: " + userId);
            callback.onFailure("Kullanıcı giriş yapmamış");
            return;
        }

        Log.d(TAG, "FirestoreManager: Kullanıcı ID: " + getUserId() + ", Not ID: " + noteId);
        Log.d(TAG, "FirestoreManager: Silinecek belge yolu: users/" + getUserId() + "/notes/" + noteId);

        getNotesCollection().document(noteId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ FirestoreManager: Not başarıyla silindi - NoteID: " + noteId);
                    callback.onSuccess("Not başarıyla silindi");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ FirestoreManager: Not silinirken hata oluştu - NoteID: " + noteId, e);
                    callback.onFailure("Not silinirken hata: " + e.getMessage());
                });
    }


    public void deleteNoteByContent(Notes note, FirestoreCallback callback) {
        Log.d(TAG, "FirestoreManager: İçerik eşleştirmesi ile not silme başlatılıyor");
        Log.d(TAG, "FirestoreManager: Aranan not - Başlık: '" + note.notesTitle + "', Tarih: '" + note.notesDate + "', İçerik: '" + note.notes + "'");
        
        if (getUserId() == null) {
            Log.e(TAG, "FirestoreManager: Kullanıcı giriş yapmamış - userId: " + userId);
            callback.onFailure("Kullanıcı giriş yapmamış");
            return;
        }


        getNotesCollection()
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        Log.d(TAG, "FirestoreManager: Firestore'da " + querySnapshot.size() + " not bulundu");
                        

                        for (QueryDocumentSnapshot document : querySnapshot) {
                            String fsTitle = document.getString("notesTitle");
                            String fsDate = document.getString("notesDate");
                            String fsContent = document.getString("notes");
                            Log.d(TAG, "FirestoreManager: Firestore notu - ID: " + document.getId() + 
                                      ", Başlık: '" + fsTitle + "', Tarih: '" + fsDate + "', İçerik: '" + fsContent + "'");
                        }
                        

                        for (QueryDocumentSnapshot document : querySnapshot) {
                            String fsTitle = document.getString("notesTitle");
                            String fsDate = document.getString("notesDate");
                            String fsContent = document.getString("notes");
                            

                            String searchTitle = note.notesTitle != null ? note.notesTitle.trim() : "";
                            String searchDate = note.notesDate != null ? note.notesDate.trim() : "";
                            String searchContent = note.notes != null ? note.notes.trim() : "";
                            
                            String docTitle = fsTitle != null ? fsTitle.trim() : "";
                            String docDate = fsDate != null ? fsDate.trim() : "";
                            String docContent = fsContent != null ? fsContent.trim() : "";
                            
                            Log.d(TAG, "FirestoreManager: Eşleştirme kontrolü - Aranan: '" + searchTitle + "' vs Firestore: '" + docTitle + "'");
                            

                            boolean titleMatch = searchTitle.equals(docTitle);
                            boolean dateMatch = searchDate.equals(docDate);
                            boolean contentMatch = searchContent.equals(docContent);
                            
                            Log.d(TAG, "FirestoreManager: Eşleştirme sonuçları - Başlık: " + titleMatch + ", Tarih: " + dateMatch + ", İçerik: " + contentMatch);
                            

                            boolean shouldMatch = titleMatch && contentMatch;
                            if (shouldMatch) {
                                String documentId = document.getId();
                                Log.d(TAG, "FirestoreManager: Eşleşen not bulundu - DocumentID: " + documentId);
                                
                                document.getReference().delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "✅ FirestoreManager: İçerik eşleştirmesi ile not başarıyla silindi - DocumentID: " + documentId);
                                            callback.onSuccess("Not başarıyla silindi");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "❌ FirestoreManager: İçerik eşleştirmesi ile not silinirken hata - DocumentID: " + documentId, e);
                                            callback.onFailure("Not silinirken hata: " + e.getMessage());
                                        });
                                return;
                            }
                        }
                        
                        Log.w(TAG, "FirestoreManager: Eşleşen not bulunamadı - Başlık: '" + note.notesTitle + "'");
                        callback.onFailure("Eşleşen not bulunamadı");
                        
                    } else {
                        Log.e(TAG, "FirestoreManager: Not arama hatası", task.getException());
                        callback.onFailure("Not arama hatası: " + task.getException().getMessage());
                    }
                });
    }


    public void getAllNotes(NotesCallback callback) {
        getAllNotesWithFirestoreId(callback);
    }


    public void getAllNotesWithFirestoreId(NotesCallback callback) {
        if (getUserId() == null) {
            callback.onError("Kullanıcı giriş yapmamış");
            return;
        }

        getNotesCollection()
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Notes> notesList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Notes note = documentToNotes(document);
                            note.firestoreId = document.getId();
                            notesList.add(note);
                        }
                        callback.onNotesLoaded(notesList);
                    } else {
                        Log.w(TAG, "Notlar getirilirken hata oluştu", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }


    public void getFavoriteNotes(NotesCallback callback) {
        if (getUserId() == null) {
            callback.onError("Kullanıcı giriş yapmamış");
            return;
        }

        getNotesCollection()
                .whereEqualTo("isFavorite", 1)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Notes> notesList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Notes note = documentToNotes(document);
                            note.id = Integer.parseInt(document.getId());
                            notesList.add(note);
                        }
                        callback.onNotesLoaded(notesList);
                    } else {
                        Log.w(TAG, "Favori notlar getirilirken hata oluştu", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }


    public void getLocationNotes(NotesCallback callback) {
        if (getUserId() == null) {
            callback.onError("Kullanıcı giriş yapmamış");
            return;
        }

        getNotesCollection()
                .whereEqualTo("hasLocationReminder", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Notes> notesList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Notes note = documentToNotes(document);
                            note.id = Integer.parseInt(document.getId());
                            notesList.add(note);
                        }
                        callback.onNotesLoaded(notesList);
                    } else {
                        Log.w(TAG, "Konum notları getirilirken hata oluştu", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }


    public void searchNotes(String query, NotesCallback callback) {
        if (getUserId() == null) {
            callback.onError("Kullanıcı giriş yapmamış");
            return;
        }

        getNotesCollection()
                .orderBy("notesTitle")
                .startAt(query)
                .endAt(query + '\uf8ff')
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Notes> notesList = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Notes note = documentToNotes(document);
                            note.id = Integer.parseInt(document.getId());
                            notesList.add(note);
                        }
                        callback.onNotesLoaded(notesList);
                    } else {
                        Log.w(TAG, "Not arama hatası", task.getException());
                        callback.onError(task.getException().getMessage());
                    }
                });
    }


    private Notes documentToNotes(DocumentSnapshot document) {
        Notes note = new Notes();
        note.notesTitle = document.getString("notesTitle") != null ? document.getString("notesTitle") : "";
        note.notesSubtitle = document.getString("notesSubtitle") != null ? document.getString("notesSubtitle") : "";
        note.notes = document.getString("notes") != null ? document.getString("notes") : "";
        note.notesDate = document.getString("notesDate") != null ? document.getString("notesDate") : "";
        note.notesPriority = document.getString("notesPriority") != null ? document.getString("notesPriority") : "1";
        Long isFavoriteLong = document.getLong("isFavorite");
        note.isFavorite = isFavoriteLong != null ? isFavoriteLong.intValue() : 0;
        Double latitude = document.getDouble("latitude");
        note.latitude = latitude != null ? latitude : 0.0;
        Double longitude = document.getDouble("longitude");
        note.longitude = longitude != null ? longitude : 0.0;
        note.locationName = document.getString("locationName") != null ? document.getString("locationName") : "";
        note.hasLocationReminder = document.getBoolean("hasLocationReminder") != null ? document.getBoolean("hasLocationReminder") : false;
        note.alarmTimes = document.getString("alarmTimes") != null ? document.getString("alarmTimes") : "";
        note.hasAlarm = document.getBoolean("hasAlarm") != null ? document.getBoolean("hasAlarm") : false;
        Long bgIndex = document.getLong("backgroundColorIndex");
        note.backgroundColorIndex = bgIndex != null ? bgIndex.intValue() : 0;
        note.firestoreId = document.getId() != null ? document.getId() : "";
        return note;
    }


    public void syncUserData(List<Notes> localNotes, FirestoreCallback callback) {
        if (getUserId() == null) {
            callback.onFailure("Kullanıcı giriş yapmamış");
            return;
        }

        getNotesCollection().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    document.getReference().delete();
                }
          
                final int[] successCount = {0};
                final int totalCount = localNotes.size();
                
                if (totalCount == 0) {
                    callback.onSuccess("Senkronizasyon tamamlandı");
                    return;
                }
                
                for (Notes note : localNotes) {
                    addNote(note, new FirestoreCallback() {
                        @Override
                        public void onSuccess(Object result) {
                            successCount[0]++;
                            if (successCount[0] == totalCount) {
                                callback.onSuccess("Senkronizasyon tamamlandı");
                            }
                        }

                        @Override
                        public void onFailure(String error) {
                            callback.onFailure("Senkronizasyon hatası: " + error);
                        }
                    });
                }
            } else {
                callback.onFailure("Mevcut veriler temizlenirken hata oluştu");
            }
        });
    }
} 