package com.example.mobilesecurity_hw1;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.example.mobilesecurity_hw1.databinding.ActivityMainBinding;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private WifiManager wifiManager;
    private BroadcastReceiver wifiScanReceiver;
    private BluetoothAdapter bluetoothAdapter;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                Boolean micGranted = result.getOrDefault(Manifest.permission.RECORD_AUDIO, false);
                boolean bluetoothGranted = true;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bluetoothGranted = Boolean.TRUE.equals(result.get(Manifest.permission.BLUETOOTH_CONNECT));
                }

                if ((fineGranted != null && fineGranted || coarseGranted != null && coarseGranted)
                        && bluetoothGranted && Boolean.TRUE.equals(micGranted)) {
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
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!hasPermissions()) {
            requestPermissions();
        } else {
            initWifiScanning();
            initSwitches();
        }

        binding.btnContinue.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SuccessActivity.class);
            startActivity(intent);
        });

    }

    private boolean hasPermissions() {
        boolean location = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean mic = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        boolean bluetooth = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetooth = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }

        return location && bluetooth && mic;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.RECORD_AUDIO
            });
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.RECORD_AUDIO
            });
        }
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

        binding.switchBattery.setOnClickListener(v -> {
            if (isBatteryAbove50()) {
                binding.switchBattery.setChecked(true);
                Toast.makeText(this, "Battery level is sufficient ✅", Toast.LENGTH_SHORT).show();
            } else {
                binding.switchBattery.setChecked(false);
                Toast.makeText(this, "Battery must be at least 50% ❌", Toast.LENGTH_SHORT).show();
            }
            updateContinueButtonState();
        });

        binding.switchBluetooth.setOnClickListener(v -> {
            if (isBluetoothEnabled()) {
                binding.switchBluetooth.setChecked(true);
                Toast.makeText(this, "Bluetooth is ON ✅", Toast.LENGTH_SHORT).show();
            } else {
                binding.switchBluetooth.setChecked(false);
                Toast.makeText(this, "Bluetooth is OFF ❌", Toast.LENGTH_SHORT).show();
            }
            updateContinueButtonState();
        });

        binding.switchNoise.setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 101);
                return;
            }

            new Thread(() -> {
                boolean noisy = detectNoiseWithAudioRecord();
                runOnUiThread(() -> {
                    if (noisy) {
                        binding.switchNoise.setChecked(true);
                        Toast.makeText(this, "Noisy environment detected ✅", Toast.LENGTH_SHORT).show();
                    } else {
                        binding.switchNoise.setChecked(false);
                        Toast.makeText(this, "Environment is too quiet ❌", Toast.LENGTH_SHORT).show();
                    }
                    updateContinueButtonState();
                });
            }).start();
        });

        binding.switchRingerMode.setOnClickListener(v -> {
            boolean normal = isRingerModeNormal();
            binding.switchRingerMode.setChecked(normal);
            Toast.makeText(this,
                    normal ? "Ringer mode is Normal ✅" : "Phone is in Silent/Vibrate ❌",
                    Toast.LENGTH_SHORT).show();
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

    private boolean isBatteryAbove50() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        int batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return batteryLevel >= 50;
    }

    private boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private boolean detectNoiseWithAudioRecord() {
        int bufferSize = AudioRecord.getMinBufferSize(44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        short[] buffer = new short[bufferSize];
        recorder.startRecording();

        long startTime = System.currentTimeMillis();
        long endTime = startTime + 1500;
        boolean noisy = false;

        while (System.currentTimeMillis() < endTime) {
            int read = recorder.read(buffer, 0, bufferSize);

            for (int i = 0; i < read; i++) {
                if (Math.abs(buffer[i]) > 2000) {
                    noisy = true;
                    break;
                }
            }

            if (noisy) break;
        }

        recorder.stop();
        recorder.release();

        return noisy;
    }

    private boolean isRingerModeNormal() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        return audioManager != null && audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
    }

    private void updateContinueButtonState() {
        boolean allChecked = binding.switchWifi.isChecked()
                && binding.switchBattery.isChecked()
                && binding.switchBluetooth.isChecked()
                && binding.switchNoise.isChecked()
                && binding.switchRingerMode.isChecked();
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
