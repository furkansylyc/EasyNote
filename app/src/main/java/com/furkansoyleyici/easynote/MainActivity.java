package com.furkansoyleyici.easynote;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import com.furkansoyleyici.easynote.Activity.BaseActivity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.FullScreenContentCallback;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.SearchView;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.furkansoyleyici.easynote.Activity.insertNotesActivity;
import com.furkansoyleyici.easynote.Activity.LanguageSelectionActivity;
import com.furkansoyleyici.easynote.Adapter.NotesAdapter;
import com.furkansoyleyici.easynote.Model.Notes;
import com.furkansoyleyici.easynote.Service.LocationService;
// import com.furkansoyleyici.easynote.ViewModel.NotesViewModel; // Artık kullanılmıyor
import com.furkansoyleyici.easynote.Auth.FirebaseAuthManager;
import com.furkansoyleyici.easynote.Auth.AccountDeleteManager;
import com.furkansoyleyici.easynote.Firebase.FirestoreManager;
import com.furkansoyleyici.easynote.Activity.LoginActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Gravity;
import android.widget.Switch;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.widget.Toast;
import java.util.concurrent.Executors;

public class MainActivity extends BaseActivity {

    FloatingActionButton newNotesButton, menuButton, deleteButton;

    RecyclerView notesRecycler;
    NotesAdapter adapter;
    TextView nofilter, yenieski, eskiyeni, fav, location;
    private InterstitialAd mInterstitialAd;

    List<Notes> allNotes = new ArrayList<>();
    boolean isSelectionMode = false;
    List<Notes> selectedNotes = new ArrayList<>();
    String currentFilter = "all";
    

    private FirebaseAuthManager authManager;
    private AccountDeleteManager accountDeleteManager;
    private FirestoreManager firestoreManager;
    private FirebaseUser currentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        authManager = new FirebaseAuthManager(this, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onAuthSuccess(FirebaseUser user) {
                currentUser = user;
                firestoreManager = new FirestoreManager();


                Executors.newSingleThreadExecutor().execute(() -> {
                    // notesViewModel.repository.notesDao.deleteAllNotes(); // Room bağımlılığı kaldırıldı
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Eski notlar temizlendi, bulut notlarınız yükleniyor...", Toast.LENGTH_SHORT).show();
                    });
                    firestoreManager.getAllNotes(new FirestoreManager.NotesCallback() {
                        @Override
                        public void onNotesLoaded(java.util.List<com.furkansoyleyici.easynote.Model.Notes> notes) {
                            if (notes != null && !notes.isEmpty()) {
                                Executors.newSingleThreadExecutor().execute(() -> {
                                    // for (com.furkansoyleyici.easynote.Model.Notes note : notes) { // Room bağımlılığı kaldırıldı
                                    //     notesViewModel.repository.insertNotes(note);
                                    // }
                                    runOnUiThread(() -> {
                                        Toast.makeText(MainActivity.this, "Bulut notlarınız yüklendi!", Toast.LENGTH_SHORT).show();
                                    });
                                });
                            }
                        }
                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "Bulut notlarınız yüklenemedi: " + error, Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                });
            }

            @Override
            public void onAuthFailure(String error) {

                startLoginActivity();
            }

            @Override
            public void onSignOut() {
                startLoginActivity();
            }
        });


        if (!authManager.isUserSignedIn()) {
            startLoginActivity();
            return;
        }

        currentUser = authManager.getCurrentUser();
        firestoreManager = new FirestoreManager();
        accountDeleteManager = new AccountDeleteManager(this);

        MobileAds.initialize(this, initializationStatus -> {});
        AdRequest adRequest = new AdRequest.Builder().build();

        loadInterstitialAd();

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        loadSavedBackground();

        newNotesButton = findViewById(R.id.newNotesButton);
        menuButton = findViewById(R.id.menuButton);
        deleteButton = findViewById(R.id.deleteButton);
        notesRecycler = findViewById(R.id.notesRecycler);

        // Önce filter view'larını tanımla
        nofilter = findViewById(R.id.nofilter);
        yenieski = findViewById(R.id.yenieski);
        eskiyeni = findViewById(R.id.eskiyeni);
        fav = findViewById(R.id.fav);
        location = findViewById(R.id.location);

        // Sonra layout manager ve adapter'ı ayarla
        notesRecycler.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new NotesAdapter(this, new ArrayList<>()); // Firestore tabanlı
        notesRecycler.setAdapter(adapter);


        nofilter.setBackgroundResource(R.drawable.filter_selected);

        // notesViewModel.getallNotes.observe(this, notes -> { // Room bağımlılığı kaldırıldı
        //     if (notes != null && filterNotesAllList == null) {
        //         filterNotesAllList = notes;
        //         setAdapter(notes);
        //     }
        // });

        nofilter.setOnClickListener( v -> {
            currentFilter = "all";
            nofilter.setBackgroundResource(R.drawable.filter_selected);
            yenieski.setBackgroundResource(R.drawable.filter_unselected);
            eskiyeni.setBackgroundResource(R.drawable.filter_unselected);
            fav.setBackgroundResource(R.drawable.filter_unselected);
            location.setBackgroundResource(R.drawable.filter_unselected);
            applyCurrentFilter();
        });

        yenieski.setOnClickListener(v -> {
            currentFilter = "new_old";
            yenieski.setBackgroundResource(R.drawable.filter_selected);
            nofilter.setBackgroundResource(R.drawable.filter_unselected);
            eskiyeni.setBackgroundResource(R.drawable.filter_unselected);
            fav.setBackgroundResource(R.drawable.filter_unselected);
            location.setBackgroundResource(R.drawable.filter_unselected);
            applyCurrentFilter();
        });
        eskiyeni.setOnClickListener(v -> {
            currentFilter = "old_new";
            eskiyeni.setBackgroundResource(R.drawable.filter_selected);
            yenieski.setBackgroundResource(R.drawable.filter_unselected);
            nofilter.setBackgroundResource(R.drawable.filter_unselected);
            fav.setBackgroundResource(R.drawable.filter_unselected);
            location.setBackgroundResource(R.drawable.filter_unselected);
            applyCurrentFilter();
        });
        fav.setOnClickListener(v -> {
            currentFilter = "favorites";
            fav.setBackgroundResource(R.drawable.filter_selected);
            yenieski.setBackgroundResource(R.drawable.filter_unselected);
            eskiyeni.setBackgroundResource(R.drawable.filter_unselected);
            location.setBackgroundResource(R.drawable.filter_unselected);
            nofilter.setBackgroundResource(R.drawable.filter_unselected);
            applyCurrentFilter();
        });
        location.setOnClickListener(v -> {
            currentFilter = "location";
            location.setBackgroundResource(R.drawable.filter_selected);
            nofilter.setBackgroundResource(R.drawable.filter_unselected);
            yenieski.setBackgroundResource(R.drawable.filter_unselected);
            eskiyeni.setBackgroundResource(R.drawable.filter_unselected);
            fav.setBackgroundResource(R.drawable.filter_unselected);
            applyCurrentFilter();
        });

        menuButton.setOnClickListener(v -> showModernMenu());

        deleteButton.setOnClickListener(v -> deleteSelectedNotes());

        newNotesButton.setOnClickListener(v -> {
            if (mInterstitialAd != null) {
                mInterstitialAd.show(MainActivity.this);
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        startActivity(new Intent(MainActivity.this, insertNotesActivity.class));
                    }
                    @Override
                    public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                        startActivity(new Intent(MainActivity.this, insertNotesActivity.class));
                    }
                    @Override
                    public void onAdShowedFullScreenContent() {
                        mInterstitialAd = null;
                        loadInterstitialAd();
                    }
                });
            } else {
                startActivity(new Intent(MainActivity.this, insertNotesActivity.class));
            }
        });

        checkNotificationPermission();
        checkLocationPermission();
        startLocationService();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "not_channel",
                    getString(R.string.location_reminder_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }


    public void setAdapter(List<Notes> notes) {
        if (notes == null) {
            notes = new ArrayList<>();
        }
        
        if (adapter == null) {
            notesRecycler.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
            adapter = new NotesAdapter(MainActivity.this, notes);
            notesRecycler.setAdapter(adapter);
        } else {
            adapter.updateNotes(notes);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_notes,menu);

        MenuItem menuItem = menu.findItem(R.id.search_bar);
        SearchView searchView =(SearchView) menuItem.getActionView();
                searchView.setQueryHint(getString(R.string.menu_search));
        
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                NotesFilter(newText);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        

        if (firestoreManager != null && adapter != null) {
            firestoreManager.getAllNotes(new FirestoreManager.NotesCallback() {
                @Override
                public void onNotesLoaded(java.util.List<com.furkansoyleyici.easynote.Model.Notes> notes) {
                    runOnUiThread(() -> {
                        allNotes = notes != null ? notes : new ArrayList<>();
                        applyCurrentFilter();
                    });
                }
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Notlar yüklenemedi: " + error, Toast.LENGTH_LONG).show();
                    });
                }
            });
        }
        
        applyCurrentFilter();
        if (adapter != null) {
            if (allNotes != null) {
                setAdapter(allNotes);
            } else {

            }
        }
    }



    private void showModernMenu() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_menu, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        View languageOption = bottomSheetView.findViewById(R.id.languageOption);
        languageOption.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Intent intent = new Intent(this, LanguageSelectionActivity.class);
            startActivity(intent);
        });

        View settingsOption = bottomSheetView.findViewById(R.id.settingsOption);
        settingsOption.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showSettingsDialog();
        });

        bottomSheetDialog.show();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.settings_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        View backgroundOption = dialogView.findViewById(R.id.backgroundOption);
        backgroundOption.setOnClickListener(v -> {
            dialog.dismiss();
            showBackgroundSelectionDialog();
        });
        
        Switch notificationSwitch = dialogView.findViewById(R.id.notificationSwitch);
        boolean notificationsEnabled = getNotificationSetting();
        notificationSwitch.setChecked(notificationsEnabled);
        
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setNotificationSetting(isChecked);
            if (isChecked) {
                android.widget.Toast.makeText(this, getString(R.string.notifications_enabled), android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(this, getString(R.string.notifications_disabled), android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        
        android.widget.Button logoutButton = dialogView.findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            dialog.dismiss();
            showLogoutDialog();
        });

        android.widget.Button deleteAccountButton = dialogView.findViewById(R.id.deleteAccountButton);
        deleteAccountButton.setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteAccountOptionsDialog();
        });

        android.widget.Button closeButton = dialogView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());
    }



    private void setupBackgroundOptions(View dialogView, AlertDialog dialog) {
        String currentBackground = getCurrentBackground();
        
        int[] backgroundIds = {
            R.id.bg1_option, R.id.bg2_option, R.id.bg3_option, R.id.bg4_option, R.id.bg5_option,
            R.id.bg6_option, R.id.bg7_option, R.id.bg8_option, R.id.bg9_option, R.id.bg10_option,R.id.bg11_option
        };
        
        String[] backgroundNames = {"bg1", "bg2", "bg3", "bg4", "bg5", "bg6", "bg7", "bg8", "bg9", "bg10","bg11"};
        
        for (int i = 0; i < backgroundIds.length; i++) {
            ImageView bgOption = dialogView.findViewById(backgroundIds[i]);
            String bgName = backgroundNames[i];
            
            if (bgName.equals(currentBackground)) {
                bgOption.setAlpha(0.7f);
                bgOption.setScaleX(0.9f);
                bgOption.setScaleY(0.9f);
            }
            
            bgOption.setOnClickListener(v -> {
                setBackground(bgName);
                dialog.dismiss();
                android.widget.Toast.makeText(this, "Arka plan değiştirildi!", android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showBackgroundSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.background_selection_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        View customPhotoOption = dialogView.findViewById(R.id.customPhotoOption);
        customPhotoOption.setOnClickListener(v -> {
            dialog.dismiss();
            selectCustomPhoto();
        });

        setupBackgroundOptions(dialogView, dialog);
        
        android.widget.Button closeButton = dialogView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());
    }

    private void setupBackgroundOptionsInSettings(View dialogView, AlertDialog dialog) {
        String currentBackground = getCurrentBackground();
        
        int[] backgroundIds = {
            R.id.bg1_option, R.id.bg2_option, R.id.bg3_option, R.id.bg4_option, R.id.bg5_option,
            R.id.bg6_option, R.id.bg7_option, R.id.bg8_option, R.id.bg9_option, R.id.bg10_option,R.id.bg11_option
        };
        
        String[] backgroundNames = {"bg1", "bg2", "bg3", "bg4", "bg5", "bg6", "bg7", "bg8", "bg9", "bg10","bg11"};
        
        for (int i = 0; i < backgroundIds.length; i++) {
            ImageView bgOption = dialogView.findViewById(backgroundIds[i]);
            String bgName = backgroundNames[i];
            
            if (bgName.equals(currentBackground)) {
                bgOption.setAlpha(0.7f);
                bgOption.setScaleX(0.9f);
                bgOption.setScaleY(0.9f);
            }
            
            bgOption.setOnClickListener(v -> {
                setBackground(bgName);
                android.widget.Toast.makeText(this, "Arka plan değiştirildi!", android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }

    private String getCurrentBackground() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        return prefs.getString("background", "bg1");
    }

    private void setBackground(String backgroundName) {
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putString("background", backgroundName);
        editor.apply();
        
        android.widget.RelativeLayout mainLayout = findViewById(R.id.main);
        if (mainLayout != null) {
            if (backgroundName.startsWith("custom_")) {
                String customPhotoPath = getCustomPhotoPath();
                if (customPhotoPath != null && new File(customPhotoPath).exists()) {
                    mainLayout.setBackground(null);
                    android.graphics.drawable.BitmapDrawable drawable = new android.graphics.drawable.BitmapDrawable(getResources(), customPhotoPath);
                    drawable.setGravity(Gravity.CENTER);
                    mainLayout.setBackground(drawable);
                } else {
                    mainLayout.setBackgroundResource(R.drawable.bg1);
                }
            } else {
                int backgroundResource = getBackgroundResource(backgroundName);
                mainLayout.setBackgroundResource(backgroundResource);
            }
        }
    }

    private int getBackgroundResource(String backgroundName) {
        switch (backgroundName) {
            case "bg1": return R.drawable.bg1;
            case "bg2": return R.drawable.bg2;
            case "bg3": return R.drawable.bg3;
            case "bg4": return R.drawable.bg4;
            case "bg5": return R.drawable.bg5;
            case "bg6": return R.drawable.bg6;
            case "bg7": return R.drawable.bg7;
            case "bg8": return R.drawable.bg8;
            case "bg9": return R.drawable.bg9;
            case "bg10": return R.drawable.bg10;
            case "bg11" : return R.drawable.bg11;
            default: return R.drawable.bg1;
        }
    }

    private void loadSavedBackground() {
        String savedBackground = getCurrentBackground();
        android.widget.RelativeLayout mainLayout = findViewById(R.id.main);
        if (mainLayout != null) {
            if (savedBackground.startsWith("custom_")) {
                String customPhotoPath = getCustomPhotoPath();
                if (customPhotoPath != null && new File(customPhotoPath).exists()) {
                    mainLayout.setBackground(null);
                    android.graphics.drawable.BitmapDrawable drawable = new android.graphics.drawable.BitmapDrawable(getResources(), customPhotoPath);
                    drawable.setGravity(Gravity.CENTER);
                    mainLayout.setBackground(drawable);
                } else {
                    mainLayout.setBackgroundResource(R.drawable.bg1);
                }
            } else {
                int backgroundResource = getBackgroundResource(savedBackground);
                mainLayout.setBackgroundResource(backgroundResource);
            }
        }
    }

    private void selectCustomPhoto() {
        photoPickerLauncher.launch("image/*");
    }

    private void processSelectedPhoto(Uri uri) {
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
            String photoPath = saveCustomPhoto(bitmap);
            setCustomBackground(photoPath);
            android.widget.Toast.makeText(this, "Fotoğraf arka plan olarak ayarlandı!", android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            android.widget.Toast.makeText(this, "Fotoğraf yüklenirken hata oluştu!", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private String saveCustomPhoto(Bitmap bitmap) {
        try {
            File photoFile = new File(getFilesDir(), "custom_photo.jpg");
            FileOutputStream fos = new FileOutputStream(photoFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return photoFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setCustomBackground(String photoPath) {
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putString("background", "custom_photo");
        editor.putString("custom_photo_path", photoPath);
        editor.apply();
        
        android.widget.RelativeLayout mainLayout = findViewById(R.id.main);
        if (mainLayout != null) {
            mainLayout.setBackground(null);
            android.graphics.drawable.BitmapDrawable drawable = new android.graphics.drawable.BitmapDrawable(getResources(), photoPath);
            drawable.setGravity(Gravity.CENTER);
            mainLayout.setBackground(drawable);
        }
    }

    private String getCustomPhotoPath() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        return prefs.getString("custom_photo_path", null);
    }

    private boolean getNotificationSetting() {
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        return prefs.getBoolean("notifications_enabled", true);
    }

    private void setNotificationSetting(boolean enabled) {
        android.content.SharedPreferences prefs = getSharedPreferences("app_settings", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("notifications_enabled", enabled);
        editor.apply();
    }

    private void NotesFilter(String newText) {
        if (allNotes == null) {
            return;
        }
        
        ArrayList<Notes> FilterNames = new ArrayList<>();

        for(Notes notes:this.allNotes){

            if (notes.notesTitle.toLowerCase().contains(newText.toLowerCase()) ||
                    notes.notesSubtitle.toLowerCase().contains(newText.toLowerCase())) {
                FilterNames.add(notes);
            }

        }
        this.adapter.searchNotes(FilterNames);

    }

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    showPermissionExplanationDialog();
                }
            });

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.notification_permission_title))
                .setMessage(getString(R.string.notification_permission_message))
                .setPositiveButton(getString(R.string.settings), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                        startActivity(intent);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startLocationService();
                } else {
                    showLocationPermissionExplanationDialog();
                }
            });

    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    processSelectedPhoto(uri);
                }
            });

    private void showLocationPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.location_permission_title))
                .setMessage(getString(R.string.location_permission_message))
                .setPositiveButton(getString(R.string.settings), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                        startActivity(intent);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void startLocationService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            Intent serviceIntent = new Intent(this, LocationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }


    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this,
                "ca-app-pub-9711702385554215/1208088501",
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        mInterstitialAd = ad;
                        Log.d("Admob", "Interstitial loaded again.");
                    }

                    @Override
                    public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError adError) {
                        mInterstitialAd = null;
                        Log.e("Admob", "Interstitial failed again: " + adError.getMessage());
                    }
                });
    }

    public void enterSelectionMode() {
        isSelectionMode = true;
        selectedNotes.clear();
        if (adapter != null) {
            adapter.setSelectionMode(true);
        }
        updateSelectionModeUI();
    }

    public void exitSelectionMode() {
        isSelectionMode = false;
        selectedNotes.clear();
        if (adapter != null) {
            adapter.setSelectionMode(false);
        }
        updateSelectionModeUI();
    }

    public void toggleNoteSelection(Notes note) {
        if (selectedNotes.contains(note)) {
            selectedNotes.remove(note);
        } else {
            selectedNotes.add(note);
        }
        updateSelectionModeUI();
    }

    public boolean isNoteSelected(Notes note) {
        return selectedNotes.contains(note);
    }

    private void updateSelectionModeUI() {
        if (isSelectionMode) {
            newNotesButton.setVisibility(View.GONE);
            menuButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.VISIBLE);
            
            if (selectedNotes.isEmpty()) {
                exitSelectionMode();
            }
        } else {
            newNotesButton.setVisibility(View.VISIBLE);
            menuButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.GONE);
        }
    }

    public void deleteSelectedNotes() {
        if (selectedNotes.isEmpty()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.delete_notes_title))
                .setMessage(getString(R.string.delete_notes_message, selectedNotes.size()))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    for (Notes note : selectedNotes) {
                        // notesViewModel.deleteNote(note); // Room bağımlılığı kaldırıldı
                    }
                    selectedNotes.clear();
                    exitSelectionMode();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    public boolean isInSelectionMode() {
        return isSelectionMode;
    }


    public void applyCurrentFilter() {
        List<Notes> filtered = new ArrayList<>();
        switch (currentFilter) {
            case "all":
                filtered = allNotes;
                break;
            case "new_old":
                filtered = new ArrayList<>(allNotes);
                filtered.sort((a, b) -> b.notesPriority.compareTo(a.notesPriority));
                break;
            case "old_new":
                filtered = new ArrayList<>(allNotes);
                filtered.sort((a, b) -> a.notesPriority.compareTo(b.notesPriority));
                break;
            case "favorites":
                for (Notes note : allNotes) {
                    if (note.isFavorite == 1) filtered.add(note);
                }
                break;
            case "location":
                for (Notes note : allNotes) {
                    if (note.hasLocationReminder) filtered.add(note);
                }
                break;
        }
        adapter.updateNotes(filtered);
    }


    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void syncLocalDataToFirestore() {
        if (firestoreManager != null && adapter != null) {
            // notesViewModel.getallNotes.observe(this, localNotes -> { // Room bağımlılığı kaldırıldı
            //     if (localNotes != null && !localNotes.isEmpty()) {
            //         firestoreManager.syncUserData(localNotes, new FirestoreManager.FirestoreCallback() {
            //             @Override
            //             public void onSuccess(Object result) {
            //                 loadNotesFromFirestore();
            //             }

            //             @Override
            //             public void onFailure(String error) {
            //                 Toast.makeText(MainActivity.this, getString(R.string.sync_error, error), Toast.LENGTH_SHORT).show();
            //             }
            //         });
            //     } else {
            //         loadNotesFromFirestore();
            //     }
            // });
        }
    }

    private void loadNotesFromFirestore() {
        if (firestoreManager != null) {
            firestoreManager.getAllNotesWithFirestoreId(new FirestoreManager.NotesCallback() {
                @Override
                public void onNotesLoaded(List<Notes> notes) {
                    if (notes != null) {
                        allNotes = notes;
                        applyCurrentFilter();
                    }
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(MainActivity.this, getString(R.string.data_loading_error, error), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.logout))
                .setMessage(getString(R.string.logout_confirmation))
                .setPositiveButton(getString(R.string.logout), (dialog, which) -> {
                    authManager.signOut();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showDeleteAccountOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.delete_account_options_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        // Hemen Sil seçeneği
        View directDeleteOption = dialogView.findViewById(R.id.directDeleteOption);
        directDeleteOption.setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteAccountDialog();
        });

        // Talep Gönder seçeneği
        View requestDeleteOption = dialogView.findViewById(R.id.requestDeleteOption);
        requestDeleteOption.setOnClickListener(v -> {
            dialog.dismiss();
            openDeleteAccountUrl();
        });

        // İptal seçeneği
        View cancelOption = dialogView.findViewById(R.id.cancelOption);
        cancelOption.setOnClickListener(v -> dialog.dismiss());
    }

    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.delete_account_dialog, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCancelable(false);
        dialog.show();

        // Butonları bul ve click listener'ları ayarla
        com.google.android.material.button.MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);
        com.google.android.material.button.MaterialButton deleteButton = dialogView.findViewById(R.id.deleteButton);

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        deleteButton.setOnClickListener(v -> {
            dialog.dismiss();
            String apiKey = getString(R.string.default_web_client_id);
            
            accountDeleteManager.deleteAccountWithConfirmation(apiKey, new AccountDeleteManager.DeleteAccountCallback() {
                @Override
                public void onDeleteSuccess() {
                    Toast.makeText(MainActivity.this, getString(R.string.delete_account_success), Toast.LENGTH_LONG).show();
                    startLoginActivity();
                }

                @Override
                public void onDeleteFailure(String error) {
                    Toast.makeText(MainActivity.this, getString(R.string.delete_account_failed, error), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void openDeleteAccountUrl() {
        String url = "https://easynote-deleteaccount.vercel.app/";
        
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_cannot_open_url), Toast.LENGTH_SHORT).show();
        }
    }

}



