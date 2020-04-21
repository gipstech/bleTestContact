package com.gipstech.bletestcontact;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
{
    private static final ParcelUuid UID_SERVICE =
            ParcelUuid.fromString("0000feaa-0000-1000-8000-00805f9b34fb");

    private static final byte[] EDDYSTONE_NAMESPACE_FILTER = {
            0x00, //Frame type
            0x00, //TX power
            (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xaa,
            (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xaa,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    private static final byte[] NAMESPACE_FILTER_MASK = {
            (byte)0xFF,
            0x00,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    private static byte[] EDDYSTONE_SERVICE_DATA = {
            0x00, //Frame type
            -21, //TX power
            (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xaa,
            (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xaa, (byte)0xaa,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    AdvertiseData advData;
    ScanSettings scanSettings;
    List<ScanFilter> scanFilters;
    BluetoothLeScanner bleScanner;
    AdvertiseSettings advSettings;
    BluetoothLeAdvertiser bleAdvertiser;
    RssiListAdapter rssiListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set the list adapter
        rssiListAdapter = new RssiListAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(rssiListAdapter);

        // Retrieve the instance id
        byte[] instanceId = getInstanceId();

        // Show the instanceId
        TextView textView = findViewById(R.id.textView);
        textView.setText("ID: " + Utils.toHexString(instanceId));

        // Copy instanceId into service data
        System.arraycopy(instanceId, 0, EDDYSTONE_SERVICE_DATA, 12, 6);

        BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Configure scanning
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();

        scanSettings = new ScanSettings.Builder().
                setReportDelay(0).
                setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).
                build();

        scanFilters = new ArrayList<>();

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(UID_SERVICE)
                .setServiceData(UID_SERVICE, EDDYSTONE_NAMESPACE_FILTER, NAMESPACE_FILTER_MASK)
                .build();

        scanFilters.add(filter);

        // Configure advertising
        bleAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        advSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .setConnectable(false)
                .build();

        ParcelUuid pUuid = parcelUuidFromShortUUID(0xFEAA);

        advData = new AdvertiseData.Builder()
                .addServiceData(pUuid, EDDYSTONE_SERVICE_DATA)
                .addServiceUuid(pUuid)
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build();

        if (bleScanner == null)
        {
            textView.setText("Bluetooth scan not available.\nPlease activate bluetooth and restart the app.");
        }
        else if (bleAdvertiser == null)
        {
            textView.setText("Bluetooth advertising not available.\nPlease activate bluetooth and restart the app.");
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // Ask the permission to the user
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION }, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        // Check the permission has been granted
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(this, "You must accept the permission", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (bleScanner != null)
        {
            bleScanner.startScan(scanFilters, scanSettings, myScanCallback);
        }

        if (bleAdvertiser != null)
        {
            bleAdvertiser.startAdvertising(advSettings, advData, myAdvertisingCallback);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (bleScanner != null)
        {
            bleScanner.stopScan(myScanCallback);
        }

        if (bleAdvertiser != null)
        {
            bleAdvertiser.stopAdvertising(myAdvertisingCallback);
        }
    }

    private ParcelUuid parcelUuidFromShortUUID(long serviceId)
    {
        return new ParcelUuid(new UUID(0x1000 | (serviceId << 32), 0x800000805F9B34FBL));
    }

    byte[] getInstanceId()
    {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String instanceIdAsString = sharedPref.getString("pref_instance_id", null);

        byte[] instanceId;

        if (instanceIdAsString == null)
        {
            // Generate a random instance id
            Random random = new Random();
            instanceId = new byte[6];
            random.nextBytes(instanceId);

            instanceIdAsString = Utils.toHexString(instanceId);
            sharedPref.edit().putString("pref_instance_id", instanceIdAsString).commit();
        }
        else
        {
            instanceId = Utils.fromHexString(instanceIdAsString);
        }

        return instanceId;
    }

    ScanCallback myScanCallback = new ScanCallback()
    {
        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            for (ScanResult result : results)
            {
                rssiListAdapter.add(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode)
        {
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            rssiListAdapter.add(result);
        }
    };

    AdvertiseCallback myAdvertisingCallback = new AdvertiseCallback()
    {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect)
        {
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode)
        {
            super.onStartFailure(errorCode);
        }
    };
}