package no.iegget.bluetherm;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import no.iegget.bluetherm.adapters.DeviceAdapter;
import no.iegget.bluetherm.models.BluetoothDevice;

/**
 * Created by iver on 21/04/16.
 */
public class DeviceScanActivity extends ListActivity {

    private boolean mScanning;
    private Handler mHandler;
    private Context mContext = getBaseContext();
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLeScanner;
    private ScanSettings mScanSettings;
    private List<ScanFilter> mScanFilters;

    private DeviceAdapter<String> mArrayAdapter;
    private List<BluetoothDevice> deviceList;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private ProgressBar spinner;
    private TextView progressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicescan);
        mHandler = new Handler();
        mContext = getApplicationContext();
        mBluetoothAdapter = ((BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        spinner = (ProgressBar) findViewById(R.id.scanningProgress);
        progressText = (TextView) findViewById(R.id.scanningText);

        deviceList = new ArrayList<>();
        mArrayAdapter = new DeviceAdapter<String>(this, R.layout.device_row_layout, R.id.deviceListText, deviceList);
        setListAdapter(mArrayAdapter);
    }

    private void updateFeedback() {
        if (mScanning) {
            progressText.setText("Scanning for Bluetooth devices ...");
            spinner.setVisibility(View.VISIBLE);
        }
        else {
            progressText.setText("Found " + deviceList.size() + " devices");
            spinner.setVisibility(View.GONE);
        }
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
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            if (result.getDevice().getName() != null && !deviceList.contains(result)) {
                deviceList.add(new BluetoothDevice(result.getDevice().getAddress(), result.getDevice().getName()));
                mArrayAdapter.notifyDataSetChanged();
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

}
