package no.iegget.bluetherm;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import no.iegget.bluetherm.adapters.DeviceAdapter;
import no.iegget.bluetherm.utils.Constants;

/**
 * Created by iver on 21/04/16.
 */
public class DeviceScanActivity extends ListActivity {

    private boolean mScanning;
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLeScanner;
    private ScanSettings mScanSettings;
    private List<ScanFilter> mScanFilters;
    private BluetoothGatt mGatt;

    private DeviceAdapter<String> mArrayAdapter;
    private List<BluetoothDevice> deviceList;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10_000;

    private ProgressBar spinner;
    private TextView progressText;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicescan);
        mHandler = new Handler();
        mBluetoothAdapter = ((BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        spinner = (ProgressBar) findViewById(R.id.scanningProgress);
        progressText = (TextView) findViewById(R.id.scanningText);

        deviceList = new ArrayList<>();
        mArrayAdapter = new DeviceAdapter<String>(this, R.layout.device_row_layout, R.id.deviceListName, deviceList);
        setListAdapter(mArrayAdapter);

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        BluetoothDevice bluetoothDevice = (BluetoothDevice) this.getListAdapter().getItem(position);
        Log.i("DeviceScanActivity", "clicked on " + bluetoothDevice.getName());
        connectToDevice(bluetoothDevice);
        alertConnection();
    }

    private void updateFeedback() {
        if (mScanning) spinner.setVisibility(View.VISIBLE);
        else spinner.setVisibility(View.GONE);
        String resultText = "No devices found";
        if (deviceList.size() == 1)  resultText = "Found 1 device";
        if (deviceList.size() > 1) resultText = "Found " + deviceList.size() + " devices";
        progressText.setText(resultText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mScanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        mScanFilters = new ArrayList<>();
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (mGatt != null) {
            mGatt.close();
            mGatt = null;
        }
        super.onDestroy();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mLeScanner.stopScan(mScanCallback);
                    updateFeedback();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mLeScanner.startScan(mScanFilters, mScanSettings, mScanCallback);
            updateFeedback();
        } else {
            mScanning = false;
            mLeScanner.stopScan(mScanCallback);
            updateFeedback();
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //Log.i("callbackType", String.valueOf(callbackType));
            //Log.i("result", result.toString());

            if (!deviceList.contains(result.getDevice())) {
                deviceList.add(result.getDevice());
                mArrayAdapter.notifyDataSetChanged();
                updateFeedback();
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
            mProgressDialog = ProgressDialog.show(this, "Connecting to " + device.getName(), "Please wait", true);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    mProgressDialog.dismiss();
                    alertConnection();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    mGatt = null;
                    mProgressDialog.dismiss();
                    alertNoConnection();
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
                    mGatt = null;
                    mProgressDialog.dismiss();
                    alertNoConnection();
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get
                    (0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            gatt.disconnect();
        }
    };

    private void alertNoConnection() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DeviceScanActivity.this, "Could not connect to device", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void alertConnection() {
        scanLeDevice(false);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DeviceScanActivity.this, "Connection established", Toast.LENGTH_SHORT).show();
            }
        });
        storeAddressInSharedPreferences(mGatt.getDevice().getAddress());
        finish();
    }

    private void storeAddressInSharedPreferences(String address) {
        SharedPreferences sharedPref = this.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(Constants.DEVICE_ADDRESS, address);
        editor.commit();
    }
}
