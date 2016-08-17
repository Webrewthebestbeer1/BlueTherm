package no.iegget.bluetherm.ui;

import android.Manifest;
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import no.iegget.bluetherm.BluetoothService;
import no.iegget.bluetherm.MainActivity;
import no.iegget.bluetherm.R;

public class DeviceScanActivity extends AppCompatActivity {

    private boolean scanning;
    private Handler handler;
    private ScanSettings scanSettings;
    private List<ScanFilter> scanFilters;
    private BluetoothGatt gatt;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner leScanner;

    private DeviceAdapter<String> arrayAdapter;
    private List<BluetoothDevice> deviceList;

    private static final long SCAN_PERIOD_MS = 10_000L;

    private ProgressDialog progressDialog;
    private Toolbar toolbar;
    private ListView listView;
    private SwipeRefreshLayout swipeContainer;
    private LinearLayout noPermissions;
    private Button enablePermissions;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicescan);
        bindViews();
        setSupportActionBar(toolbar);
        setupBluetoothScanning();
        setupDeviceList();
        setupSwipeContainer();
        setupEnablePermissionsButton();
    }

    private void bindViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        listView = (ListView) findViewById(R.id.device_list);
        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        noPermissions = (LinearLayout) findViewById(R.id.no_permissions);
        enablePermissions = (Button) findViewById(R.id.enable_permissions);
    }

    private void setupEnablePermissionsButton() {
        enablePermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAppSettings();
            }
        });
    }

    private void setupSwipeContainer() {
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (hasPermissions() && !scanning) scanLeDevice(true);
            }
        });
    }

    private void setupBluetoothScanning() {
        handler = new Handler();
        bluetoothAdapter = ((BluetoothManager) getApplicationContext()
                .getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        leScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    private void setupDeviceList() {
        deviceList = new ArrayList<>();
        arrayAdapter = new DeviceAdapter<>(
                this,
                R.layout.device_row_layout,
                R.id.deviceListName,
                deviceList
        );
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice bluetoothDevice = arrayAdapter.getItem(position);
                connectToDevice(bluetoothDevice);
                alertConnection();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        setToolbarTitle(getString(R.string.scan_for_devices));
        return true;
    }

    private void setToolbarTitle(String title) {
        assert getSupportActionBar() != null;
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onBackPressed() {
    }

    private void updateFeedback() {
        if (scanning && !swipeContainer.isRefreshing()) {
            swipeContainer.post(new Runnable() {
                @Override
                public void run() {
                    swipeContainer.setRefreshing(true);
                }
            });
        }
        else if (!scanning) swipeContainer.setRefreshing(false);
        String resultText = getString(R.string.no_devices_found);
        if (deviceList.size() == 1)  resultText = getString(R.string.found_one_device);
        if (deviceList.size() > 1) resultText = String.format(
                getString(R.string.found_x_devices),
                deviceList.size()
        );
        setToolbarTitle(resultText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        scanFilters = new ArrayList<>();
        if (hasPermissions()) {
            scanLeDevice(true);
            showNoPermissions(false);
        }
        else showNoPermissions(true);
    }

    private void showNoPermissions(boolean enabled) {
        if (enabled) {
            listView.setVisibility(View.GONE);
            noPermissions.setVisibility(View.VISIBLE);
            swipeContainer.setEnabled(false);
        } else {
            listView.setVisibility(View.VISIBLE);
            noPermissions.setVisibility(View.GONE);
            swipeContainer.setEnabled(true);
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName())
        );
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private boolean hasPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                return false;
            } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_COARSE_LOCATION);
            }
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_COARSE_LOCATION) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    showNoPermissions(false);
                    scanLeDevice(true);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (gatt != null) {
            gatt.close();
            gatt = null;
        }
        super.onDestroy();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    leScanner.stopScan(mScanCallback);
                    updateFeedback();
                }
            }, SCAN_PERIOD_MS);

            scanning = true;
            leScanner.startScan(scanFilters, scanSettings, mScanCallback);
            updateFeedback();
        } else {
            scanning = false;
            leScanner.stopScan(mScanCallback);
            updateFeedback();
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (!deviceList.contains(result.getDevice())) {
                deviceList.add(result.getDevice());
                arrayAdapter.notifyDataSetChanged();
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
        if (gatt == null) {
            gatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
            progressDialog = ProgressDialog.show(
                    this,
                    String.format(
                            getString(R.string.connecting_to),
                            device.getName()
                    ),
                    getString(R.string.please_wait), true
            );
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    gatt.discoverServices();
                    progressDialog.dismiss();
                    alertConnection();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    DeviceScanActivity.this.gatt = null;
                    progressDialog.dismiss();
                    alertNoConnection();
                    break;
                default:
                    DeviceScanActivity.this.gatt = null;
                    progressDialog.dismiss();
                    alertNoConnection();
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            gatt.readCharacteristic(services.get(1).getCharacteristics().get
                    (0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            gatt.disconnect();
        }
    };

    private void alertNoConnection() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(
                        DeviceScanActivity.this,
                        getString(R.string.could_not_connect),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void alertConnection() {
        scanLeDevice(false);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(
                        DeviceScanActivity.this,
                        getString(R.string.connection_established),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
        storeAddressInSharedPreferences(gatt.getDevice().getAddress());
        finish();
    }

    private void storeAddressInSharedPreferences(String address) {
        SharedPreferences sharedPref = this.getSharedPreferences(
                MainActivity.SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(BluetoothService.DEVICE_ADDRESS, address);
        editor.apply();
    }
}
