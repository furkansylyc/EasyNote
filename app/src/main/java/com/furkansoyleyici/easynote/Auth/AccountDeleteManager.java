package com.furkansoyleyici.easynote.Auth;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.furkansoyleyici.easynote.Firebase.FirestoreManager;
import com.furkansoyleyici.easynote.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class AccountDeleteManager {
    private static final String TAG = "AccountDeleteManager";
    private static final String FIREBASE_AUTH_URL = "https://identitytoolkit.googleapis.com/v1/accounts:delete";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private Context context;
    private FirebaseAuth mAuth;
    private FirestoreManager firestoreManager;
    private Executor executor;
    private OkHttpClient httpClient;

    public interface DeleteAccountCallback {
        void onDeleteSuccess();
        void onDeleteFailure(String error);
    }

    public AccountDeleteManager(Context context) {
        this.context = context;
        this.mAuth = FirebaseAuth.getInstance();
        this.firestoreManager = new FirestoreManager();
        this.executor = Executors.newSingleThreadExecutor();
        this.httpClient = new OkHttpClient();
    }

    public void deleteUserAccount(String apiKey, DeleteAccountCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onDeleteFailure(context.getString(R.string.error_user_not_logged_in));
            return;
        }

        currentUser.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String idToken = task.getResult().getToken();
                        if (idToken != null) {
                            deleteAccountFromFirebase(idToken, apiKey, callback);
                        } else {
                            callback.onDeleteFailure(context.getString(R.string.error_token_not_available));
                        }
                    } else {
                        callback.onDeleteFailure(context.getString(R.string.error_token_refresh_failed));
                    }
                });
    }

    private void deleteAccountFromFirebase(String idToken, String apiKey, DeleteAccountCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("idToken", idToken);

                RequestBody body = RequestBody.create(requestBody.toString(), JSON);
                Request request = new Request.Builder()
                        .url(FIREBASE_AUTH_URL + "?key=" + apiKey)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Firebase hesap silme başarılı");
                        
                        deleteUserDataFromFirestore(() -> {
                            signOutUser();
                            callback.onDeleteSuccess();
                        });
                    } else {
                        Log.e(TAG, "Firebase hesap silme hatası: " + response.code() + " - " + responseBody);
                        String errorMessage = parseFirebaseError(responseBody);
                        callback.onDeleteFailure(errorMessage);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Network hatası", e);
                callback.onDeleteFailure(context.getString(R.string.error_network_error));
            } catch (JSONException e) {
                Log.e(TAG, "JSON hatası", e);
                callback.onDeleteFailure(context.getString(R.string.error_invalid_response));
            }
        });
    }

    private void deleteUserDataFromFirestore(Runnable onComplete) {
        firestoreManager.getAllNotes(new FirestoreManager.NotesCallback() {
            @Override
            public void onNotesLoaded(java.util.List<com.furkansoyleyici.easynote.Model.Notes> notes) {
                if (notes.isEmpty()) {
                    onComplete.run();
                    return;
                }

                final int[] deletedCount = {0};
                final int totalCount = notes.size();

                for (com.furkansoyleyici.easynote.Model.Notes note : notes) {
                    if (note.firestoreId != null) {
                        firestoreManager.deleteNote(note.firestoreId, new FirestoreManager.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                deletedCount[0]++;
                                if (deletedCount[0] == totalCount) {
                                    onComplete.run();
                                }
                            }

                            @Override
                            public void onFailure(String error) {
                                Log.w(TAG, "Not silinirken hata: " + error);
                                deletedCount[0]++;
                                if (deletedCount[0] == totalCount) {
                                    onComplete.run();
                                }
                            }
                        });
                    } else {
                        deletedCount[0]++;
                        if (deletedCount[0] == totalCount) {
                            onComplete.run();
                        }
                    }
                }
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Kullanıcı verileri getirilirken hata: " + error);
                onComplete.run();
            }
        });
    }

    private void signOutUser() {
        mAuth.signOut();
        Log.d(TAG, "Kullanıcı çıkış yapıldı");
    }

    private String parseFirebaseError(String responseBody) {
        try {
            JSONObject errorJson = new JSONObject(responseBody);
            JSONObject error = errorJson.optJSONObject("error");
            if (error != null) {
                String message = error.optString("message", "");
                int code = error.optInt("code", 0);
                
                switch (code) {
                    case 400:
                        return context.getString(R.string.error_invalid_request);
                    case 401:
                        return context.getString(R.string.error_unauthorized);
                    case 403:
                        return context.getString(R.string.error_forbidden);
                    case 404:
                        return context.getString(R.string.error_user_not_found);
                    case 429:
                        return context.getString(R.string.error_too_many_requests);
                    default:
                        if (message.contains("TOKEN_EXPIRED")) {
                            return context.getString(R.string.error_token_expired);
                        } else if (message.contains("USER_NOT_FOUND")) {
                            return context.getString(R.string.error_user_not_found);
                        } else if (message.contains("INVALID_ID_TOKEN")) {
                            return context.getString(R.string.error_invalid_token);
                        } else {
                            return context.getString(R.string.error_unknown) + " (" + message + ")";
                        }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Hata mesajı parse edilemedi", e);
        }
        return context.getString(R.string.error_unknown);
    }

    public void deleteAccountWithConfirmation(String apiKey, DeleteAccountCallback callback) {
        new android.app.AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.delete_account_title))
                .setMessage(context.getString(R.string.delete_account_confirmation))
                .setPositiveButton(context.getString(R.string.delete_yes), (dialog, which) -> {
                    deleteUserAccount(apiKey, callback);
                })
                .setNegativeButton(context.getString(R.string.delete_no), (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(false)
                .show();
    }
} 