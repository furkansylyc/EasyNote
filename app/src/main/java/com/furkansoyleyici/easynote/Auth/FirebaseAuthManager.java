package com.furkansoyleyici.easynote.Auth;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.furkansoyleyici.easynote.R;

public class FirebaseAuthManager {
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private Context context;
    private AuthCallback authCallback;

    public interface AuthCallback {
        void onAuthSuccess(FirebaseUser user);
        void onAuthFailure(String error);
        void onSignOut();
    }

    public FirebaseAuthManager(Context context, AuthCallback callback) {
        this.context = context;
        this.authCallback = callback;
        this.mAuth = FirebaseAuth.getInstance();


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public boolean isUserSignedIn() {
        return mAuth.getCurrentUser() != null;
    }

    public void signInWithEmailAndPassword(String email, String password) {
        Log.d("FirebaseAuth", "Attempting to sign in with email: " + email);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d("FirebaseAuth", "Sign in successful for user: " + user.getEmail());
                        if (authCallback != null) {
                            authCallback.onAuthSuccess(user);
                        }
                    } else {
                        Exception exception = task.getException();
                        Log.e("FirebaseAuth", "Sign in failed", exception);
                        if (authCallback != null) {
                            String errorMessage = getLocalizedErrorMessage(exception);
                            authCallback.onAuthFailure(errorMessage);
                        }
                    }
                });
    }

    public void createUserWithEmailAndPassword(String email, String password) {
        Log.d("FirebaseAuth", "Attempting to create user with email: " + email);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Log.d("FirebaseAuth", "User creation successful for: " + user.getEmail());
                        if (authCallback != null) {
                            authCallback.onAuthSuccess(user);
                        }
                    } else {
                        Exception exception = task.getException();
                        Log.e("FirebaseAuth", "User creation failed", exception);
                        if (authCallback != null) {
                            String errorMessage = getLocalizedErrorMessage(exception);
                            authCallback.onAuthFailure(errorMessage);
                        }
                    }
                });
    }

    public Intent getGoogleSignInIntent() {
        return mGoogleSignInClient.getSignInIntent();
    }

    public void handleGoogleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Log.e("GoogleSignIn", "Google ile giriş başarısız: ", e);
            String errorMsg = context.getString(R.string.error_google_sign_in_failed) + " (Kod: " + e.getStatusCode() + ")";
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
            if (authCallback != null) {
                authCallback.onAuthFailure(errorMsg);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (authCallback != null) {
                            authCallback.onAuthSuccess(user);
                        }
                    } else {
                        if (authCallback != null) {
                            String errorMessage = getLocalizedErrorMessage(task.getException());
                            authCallback.onAuthFailure(errorMessage);
                        }
                    }
                });
    }

    public void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            if (authCallback != null) {
                authCallback.onSignOut();
            }
        });
    }

    public void resetPassword(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(context, context.getString(R.string.password_reset_sent), Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMessage = getLocalizedErrorMessage(task.getException());
                        Toast.makeText(context, context.getString(R.string.password_reset_failed, errorMessage), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getLocalizedErrorMessage(Exception exception) {
        if (exception == null) {
            Log.e("FirebaseAuth", "Exception is null");
            return context.getString(R.string.error_unknown);
        }

        String errorCode = exception.getMessage();
        Log.e("FirebaseAuth", "Error code: " + errorCode);

        if (errorCode == null) {
            return context.getString(R.string.error_unknown);
        }


        if (errorCode.contains("INVALID_EMAIL")) {
            return context.getString(R.string.error_invalid_email);
        } else if (errorCode.contains("WEAK_PASSWORD")) {
            return context.getString(R.string.error_weak_password);
        } else if (errorCode.contains("EMAIL_ALREADY_IN_USE")) {
            return context.getString(R.string.error_email_already_in_use);
        } else if (errorCode.contains("USER_NOT_FOUND")) {
            return context.getString(R.string.error_user_not_found);
        } else if (errorCode.contains("WRONG_PASSWORD")) {
            return context.getString(R.string.error_wrong_password);
        } else if (errorCode.contains("NETWORK_ERROR") || errorCode.contains("TIMEOUT")) {
            return context.getString(R.string.error_network_error);
        } else if (errorCode.contains("TOO_MANY_REQUESTS")) {
            return context.getString(R.string.error_too_many_requests);
        } else if (errorCode.contains("USER_DISABLED")) {
            return context.getString(R.string.error_user_disabled);
        } else if (errorCode.contains("OPERATION_NOT_ALLOWED")) {
            return context.getString(R.string.error_operation_not_allowed);
        } else {
            Log.e("FirebaseAuth", "Unknown error: " + errorCode);
            return context.getString(R.string.error_unknown) + " (" + errorCode + ")";
        }
    }
}