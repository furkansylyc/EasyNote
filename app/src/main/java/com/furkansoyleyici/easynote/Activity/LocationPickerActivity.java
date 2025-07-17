package com.furkansoyleyici.easynote.Activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.furkansoyleyici.easynote.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationPickerActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private EditText searchEditText;
    private Button searchButton, confirmButton;
    private TextView selectedLocationText;
    private LatLng selectedLocation;
    private String selectedLocationName;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int FOREGROUND_SERVICE_LOCATION_PERMISSION_REQUEST_CODE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_location_picker);

        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);
        confirmButton = findViewById(R.id.confirmButton);
        selectedLocationText = findViewById(R.id.selectedLocationText);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, getString(R.string.map_load_error), Toast.LENGTH_SHORT).show();
            finish();
        }

        searchButton.setOnClickListener(v -> searchLocation());
        confirmButton.setOnClickListener(v -> confirmLocation());

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (!checkFineLocationPermission()) {
            requestFineLocationPermission();
        } else if (!checkForegroundServiceLocationPermission()) {
            requestForegroundServiceLocationPermission();
        } else {

            if (mMap != null) {
                try {
                    mMap.setMyLocationEnabled(true);
                } catch (SecurityException e) {

                }
            }
        }
    }

    private boolean checkFineLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkForegroundServiceLocationPermission() {
        if (Build.VERSION.SDK_INT >= 35) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestFineLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void requestForegroundServiceLocationPermission() {
        if (Build.VERSION.SDK_INT >= 35) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.FOREGROUND_SERVICE_LOCATION},
                    FOREGROUND_SERVICE_LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (checkFineLocationPermission()) {
            try {
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {

            }


            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(39.9334, 32.8597), 6));
        } else {
            requestFineLocationPermission();
        }

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            selectedLocation = latLng;
            mMap.addMarker(new MarkerOptions().position(latLng).title("Seçilen Konum"));
            getAddressFromLatLng(latLng);
        });
    }

    private void searchLocation() {
        String searchQuery = searchEditText.getText().toString().trim();
        if (searchQuery.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_address), Toast.LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(searchQuery, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng location = new LatLng(address.getLatitude(), address.getLongitude());

                mMap.clear();
                selectedLocation = location;
                mMap.addMarker(new MarkerOptions().position(location).title(searchQuery));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));

                selectedLocationName = searchQuery;
                selectedLocationText.setText("Seçilen Konum: " + searchQuery);
            } else {
                Toast.makeText(this, getString(R.string.address_not_found), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.address_search_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder addressBuilder = new StringBuilder();

                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressBuilder.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) {
                        addressBuilder.append(", ");
                    }
                }

                selectedLocationName = addressBuilder.toString();
                selectedLocationText.setText(getString(R.string.selected_location, selectedLocationName));
            }
        } catch (IOException e) {
            selectedLocationName = getString(R.string.unknown_location);
            selectedLocationText.setText(getString(R.string.selected_location, selectedLocationName));
        }
    }

    private void confirmLocation() {
        if (selectedLocation != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("latitude", selectedLocation.latitude);
            resultIntent.putExtra("longitude", selectedLocation.longitude);
            resultIntent.putExtra("locationName", selectedLocationName);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, getString(R.string.select_location), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAndRequestPermissions();
            } else {
                Toast.makeText(this, "Konum izni gerekli", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == FOREGROUND_SERVICE_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mMap != null) {
                    try {
                        mMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) {

                    }
                }
            } else {
                Toast.makeText(this, "Foreground servis konum izni gerekli", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
