package com.yantonet.printer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Set;
import java.util.UUID;

public class PrintActivity extends AppCompatActivity {

    private final String PRINTER_NAME = "AB-320M";
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
        statusText = findViewById(R.id.status_text);

        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri dataUri = intent.getData();
            if (dataUri != null) {
                handleIntentData(dataUri);
            }
        }
    }

    private void handleIntentData(Uri dataUri) {
        try {
            String encodedData = dataUri.getHost();
            String decodedData = URLDecoder.decode(encodedData, "UTF-8");

            statusText.setText("Data diterima:\n\n" + decodedData + "\n\nMencoba mencetak...");
            Toast.makeText(this, "Data diterima, memulai proses cetak...", Toast.LENGTH_SHORT).show();

            new Thread(() -> printToBluetooth(decodedData)).start();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            statusText.setText("Error: Gagal memproses data struk.");
            Toast.makeText(this, "Error: Gagal memproses data", Toast.LENGTH_LONG).show();
        }
    }

    private void printToBluetooth(String textToPrint) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showToast("Perangkat tidak mendukung Bluetooth.");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            showToast("Izin Bluetooth belum diberikan.");
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        BluetoothDevice printerDevice = null;
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equalsIgnoreCase(PRINTER_NAME)) {
                printerDevice = device;
                break;
            }
        }

        if (printerDevice == null) {
            showToast("Printer '" + PRINTER_NAME + "' tidak ditemukan.");
            return;
        }

        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            BluetoothSocket socket = printerDevice.createRfcommSocketToServiceRecord(uuid);
            socket.connect();
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(textToPrint.getBytes());
            outputStream.write("\n\n\n".getBytes());
            outputStream.close();
            socket.close();
            showToast("Berhasil mencetak!");
            new Handler().postDelayed(this::finish, 3000);
        } catch (IOException e) {
            e.printStackTrace();
            showToast("Gagal terhubung atau mencetak.");
        }
    }

    private void showToast(final String message) {
        runOnUiThread(() -> {
            Toast.makeText(PrintActivity.this, message, Toast.LENGTH_LONG).show();
            statusText.setText(message);
        });
    }
}
