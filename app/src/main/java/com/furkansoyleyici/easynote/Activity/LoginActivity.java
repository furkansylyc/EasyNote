package com.furkansoyleyici.easynote.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.furkansoyleyici.easynote.Auth.FirebaseAuthManager;
import com.furkansoyleyici.easynote.MainActivity;
import com.furkansoyleyici.easynote.R;
import com.furkansoyleyici.easynote.Activity.LanguageSelectionActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity implements FirebaseAuthManager.AuthCallback {
    
    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;
    private MaterialButton googleSignInButton;
    private TextView forgotPasswordText;
    private ProgressBar progressBar;
    
    private FirebaseAuthManager authManager;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String lang = LanguageSelectionActivity.getCurrentLanguage(this);
        if (lang == null || lang.isEmpty()) {
            lang = "tr";
            LanguageSelectionActivity.setLanguage(this, lang);
        }

        setContentView(R.layout.activity_login);

       ImageView flagTR = findViewById(R.id.flagTR);
       ImageView flagEN = findViewById(R.id.flagEN);
       ImageView flagDE = findViewById(R.id.flagDE);

        flagTR.setOnClickListener(v -> {
            LanguageSelectionActivity.setLanguage(this, "tr");
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        });
        flagEN.setOnClickListener(v -> {
            LanguageSelectionActivity.setLanguage(this, "en");
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        });
        flagDE.setOnClickListener(v -> {
            LanguageSelectionActivity.setLanguage(this, "de");
            Intent intent = getIntent();
            finish();
            startActivity(intent);
        });

        authManager = new FirebaseAuthManager(this, this);
        

        if (authManager.isUserSignedIn()) {
            startMainActivity();
            return;
        }
        
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> {
            if (isLoginMode) {
                performLogin();
            } else {
                performRegister();
            }
        });

        registerButton.setOnClickListener(v -> toggleMode());

        googleSignInButton.setOnClickListener(v -> {
            Intent signInIntent = authManager.getGoogleSignInIntent();
            startActivityForResult(signInIntent, 9001);
        });

        forgotPasswordText.setOnClickListener(v -> {
            showForgotPasswordDialog();
        });
    }

    private void performLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        authManager.signInWithEmailAndPassword(email, password);
    }

    private void performRegister() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, getString(R.string.password_min_length), Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        authManager.createUserWithEmailAndPassword(email, password);
    }

    private void toggleMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            loginButton.setText(getString(R.string.login));
            registerButton.setText(getString(R.string.register));
            forgotPasswordText.setVisibility(View.VISIBLE);
        } else {
            loginButton.setText(getString(R.string.register));
            registerButton.setText(getString(R.string.login));
            forgotPasswordText.setVisibility(View.GONE);
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
        registerButton.setEnabled(!show);
        googleSignInButton.setEnabled(!show);
    }

    private void showForgotPasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.forgot_password));

        
        final EditText emailInput = new EditText(this);
        emailInput.setHint(getString(R.string.email));
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(emailInput);

        builder.setPositiveButton(getString(R.string.send), (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, getString(R.string.please_enter_email), Toast.LENGTH_SHORT).show();
                return;
            }
            
            authManager.resetPassword(email);
        });

        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 9001) {
            if (resultCode == RESULT_OK && data != null) {
                authManager.handleGoogleSignInResult(data);
            }
        }
    }

    
    @Override
    public void onAuthSuccess(FirebaseUser user) {
        showProgress(false);
        Toast.makeText(this, getString(R.string.welcome_message, user.getEmail()), Toast.LENGTH_SHORT).show();
        startMainActivity();
    }

    @Override
    public void onAuthFailure(String error) {
        showProgress(false);
        Toast.makeText(this, getString(R.string.auth_error, error), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSignOut() {
        
    }
} 