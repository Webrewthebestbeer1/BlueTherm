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

import java.util.ArrayList;
import java.util.List;

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

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mContext = getApplicationContext();
        mBluetoothAdapter = ((BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
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
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mLeScanner.startScan(mScanFilters, mScanSettings, mScanCallback);
        } else {
            mScanning = false;
            mLeScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
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
