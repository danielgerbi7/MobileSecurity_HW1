package com.example.mobilesecurity_hw1;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.mobilesecurity_hw1.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private WifiManager wifiManager;
    private BroadcastReceiver wifiScanReceiver;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (fineGranted != null && fineGranted || coarseGranted != null && coarseGranted) {
                    initWifiScanning();
                    initSwitches();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!hasLocationPermission()) {
            requestLocationPermission();
        } else {
            initWifiScanning();
            initSwitches();
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void initWifiScanning() {
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean found = isStern5gInRange();
                if (!found) {
                    binding.switchWifi.setChecked(false);
                    Toast.makeText(context, "Stern5g Wi-Fi not found nearby", Toast.LENGTH_SHORT).show();
                    updateContinueButtonState();
                }
            }
        };

        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.startScan();
    }

    private void initSwitches() {
        binding.switchWifi.setOnClickListener(v -> {
            boolean found = isStern5gInRange();
            if (found) {
                binding.switchWifi.setChecked(true);
                Toast.makeText(this, "Stern5g found nearby - Success!", Toast.LENGTH_SHORT).show();
            } else {
                binding.switchWifi.setChecked(false);
                Toast.makeText(this, "Stern5g Wi-Fi not found nearby", Toast.LENGTH_SHORT).show();
            }
            updateContinueButtonState();
        });
    }

    private boolean isStern5gInRange() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        List<ScanResult> results = wifiManager.getScanResults();
        for (ScanResult result : results) {
            if ("Stern5g".equals(result.SSID)) {
                return true;
            }
        }
        return false;
    }

    private void updateContinueButtonState() {
        boolean allChecked = binding.switchWifi.isChecked();
        binding.btnContinue.setEnabled(allChecked);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wifiScanReceiver != null) {
            unregisterReceiver(wifiScanReceiver);
        }
    }
}
