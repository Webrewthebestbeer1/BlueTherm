package no.iegget.bluetherm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import no.iegget.bluetherm.utils.BluetoothConnectionEvent;
import no.iegget.bluetherm.utils.Constants;

public class BluetoothService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice device;
    private BluetoothGatt mGatt;
    private BluetoothGattService mGattService;
    private String deviceAddress;
    private CircularFifoQueue<Entry> entries;
    private CircularFifoQueue<Integer> xValues;
    int xVal = 0;
    private float desiredTemperature;
    private boolean alarmActivated = false;
    private boolean alarmEnabled;
    private boolean ascending;
    private final String TAG = getClass().getSimpleName();

    private final String COMMUNICATION_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private final String TX_RX_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private final String GET_TEMPERATURE_COMMAND = "AT+TEMP?";
    private final String AT_RESPONSE = "OK+Get:";
    private final float ERROR_TEMPERATURE = 85F;
    private BluetoothGattCharacteristic characteristicComm;

    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "started");
        SharedPreferences sharedPref = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        deviceAddress = sharedPref.getString(Constants.DEVICE_ADDRESS, Constants.NO_ADDRESS);
        alarmEnabled = sharedPref.getBoolean(Constants.ALARM_ENABLED, false);
        ascending = sharedPref.getBoolean(Constants.TEMP_ASCENDING, true);
        setDesiredTemperature(sharedPref.getFloat(Constants.DESIRED_TEMPERATURE, 50F));
        mBluetoothAdapter = ((BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        connectToDevice(device);

        entries = new CircularFifoQueue<>(Constants.VISIBLE_ENTRIES);
        xValues = new CircularFifoQueue<>(Constants.VISIBLE_ENTRIES);
    }

    private void startTemperatureFetching() {
        Log.i(TAG, "staring temp fetching");
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                sendCommand(GET_TEMPERATURE_COMMAND);
            }
        }, 0, Constants.READING_TICK_MS, TimeUnit.MILLISECONDS);
    }



    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.ALARM_ENABLED)) {
            alarmEnabled = sharedPreferences.getBoolean(Constants.ALARM_ENABLED, false);
        }
    }

    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        String bondState;
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    bondState = getResources().getString(R.string.connected);
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    bondState = getResources().getString(R.string.disconnected);
                    mGatt = null;
                    break;
                default:
                    bondState = getResources().getString(R.string.unknown);
                    mGatt = null;
            }
            EventBus.getDefault().post(new BluetoothConnectionEvent(device.getName(), bondState));

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService service : gatt.getServices()) {
                    if (COMMUNICATION_SERVICE_UUID.equalsIgnoreCase(service.getUuid().toString())) {
                        mGattService = service;
                        Log.i(TAG, "found service " + service.getUuid().toString());
                        for (BluetoothGattCharacteristic characteristic : mGattService.getCharacteristics()) {
                            if (characteristic.getUuid().toString().equalsIgnoreCase(TX_RX_UUID)) {
                                Log.i(TAG, "found comm characteristic");
                                characteristicComm = characteristic;
                                setCharacteristicNotification(characteristicComm, true);
                                startTemperatureFetching();
                                return;
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.w("onCharacteristicRead", characteristic.toString());
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            String response = new String(characteristic.getValue());
            Log.i(TAG, response);
            if (response.startsWith(AT_RESPONSE)) {
                String value = response.substring(AT_RESPONSE.length());
                float temperature = Float.MAX_VALUE;
                try {
                    temperature = Float.valueOf(value);
                } catch (NumberFormatException e) {
                    Log.e(TAG, e.getMessage());
                } finally {
                    if (temperature != Float.MAX_VALUE && temperature != ERROR_TEMPERATURE) {
                        updateTemperature(temperature);
                    }
                }
            }

        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setDesiredTemperature(float temperature) {
        this.desiredTemperature = temperature;
        Log.i(TAG, "temp set to: " + desiredTemperature);
        alarmActivated = false;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    private void updateTemperature(float temperature) {
        int x = xVal++;
        Entry entry = new Entry(temperature, x);
        entries.add(entry);
        xValues.add(x);
        if (entry.getVal() > desiredTemperature && !alarmActivated && alarmEnabled && ascending) {
            startAlarm();
        } else if (entry.getVal() < desiredTemperature && !alarmActivated && alarmEnabled && !ascending) {
            startAlarm();
        }
        EventBus.getDefault().post(entry);
    }

    private void startAlarm() {
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this.getApplicationContext(),
                234234234,
                intent,
                0
        );
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, 0, pendingIntent);
        alarmActivated = true;
        Log.i(TAG, "alarm activated");
    }

    public List<Entry> getEntriesAsList() {
        return new ArrayList<>(this.entries);
    }

    public List<Integer> getXValuesAsList() {
        return new ArrayList<>(this.xValues);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.i(TAG, "writing " + characteristic.getStringValue(0));
        mGatt.writeCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mGatt.setCharacteristicNotification(characteristic, enabled);
    }

    private void sendCommand(String cmd) {
        characteristicComm.setValue(cmd.getBytes());
        writeCharacteristic(characteristicComm);

    }

}
