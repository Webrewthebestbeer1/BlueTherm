package no.iegget.bluetherm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
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
import android.media.RingtoneManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.github.mikephil.charting.data.Entry;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import no.iegget.bluetherm.utils.AlarmReceiver;
import no.iegget.bluetherm.utils.NotificationUtil;
import no.iegget.bluetherm.utils.TemperaturePoint;

public class BluetoothService extends Service implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = getClass().getSimpleName();

    public static final int READING_TICK_MS = 2_000;
    public static final int VISIBLE_ENTRIES = 30;
    public static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String NO_ADDRESS = "NO_ADDRESS";
    public static final String DESIRED_TEMPERATURE = "DESIRED_TEMPERATURE";
    public static final String ALARM_ENABLED = "ALARM_ENABLED";
    public static final String DIRECTION = "DIRECTION";
    public static final int DIRECTION_DESCENDING = 0;
    public static final int DIRECTION_ASCENDING = 1;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;
    private BluetoothGatt gatt;
    private BluetoothGattService gattService;
    private String deviceAddress;
    private int xVal = 0;
    private float desiredTemperature;
    private boolean alarmActivated = false;
    private boolean alarmEnabled;
    private int direction;
    private CircularFifoQueue<TemperaturePoint> entries;

    private final String COMMUNICATION_SERVICE_UUID = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private final String TX_RX_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private final String GET_TEMPERATURE_COMMAND = "AT+TEMP?";
    private final String AT_RESPONSE = "OK+Get:";
    private final float ERROR_TEMPERATURE = 85F;
    private BluetoothGattCharacteristic characteristicComm;

    private final IBinder mBinder = new LocalBinder();

    public static final String DEVICE_STATE_CHANGED = "DEVICE_STATE_CHANGED";
    public static final String TEMPERATURE_UPDATED = "TEMPERATURE_UPDATED";
    public static final String CURRENT_TEMPERATURE = "CURRENT_TEMPERATURE";
    private boolean isFetching = false;
    private boolean isShowingDisconnectedNotification = false;
    private int deviceState = 0;
    private float currentTemperature;
    private Timer timerTask;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "started");
        SharedPreferences sharedPref = getSharedPreferences(
                MainActivity.SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        deviceAddress = sharedPref.getString(DEVICE_ADDRESS, NO_ADDRESS);
        alarmEnabled = sharedPref.getBoolean(ALARM_ENABLED, false);
        direction = sharedPref.getInt(DIRECTION, DIRECTION_ASCENDING);
        setDesiredTemperature(sharedPref.getFloat(DESIRED_TEMPERATURE, 50F));
        bluetoothAdapter = ((BluetoothManager) getApplicationContext()
                .getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        if (timerTask == null) timerTask = new Timer();
        connectToDevice(device);
        entries = new CircularFifoQueue<>(VISIBLE_ENTRIES);
    }

    private void onConnected() {
        if (!isFetching) {
            Log.i(TAG, "starting fetch");
            timerTask.purge();
            timerTask.schedule(new FetchTemperatureTask(), READING_TICK_MS);
            isFetching = true;
        }
        if (isShowingDisconnectedNotification) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(NotificationUtil.DISCONNECTED_NOTIFICATION_ID);
            isShowingDisconnectedNotification = false;
        }
    }

    private void onDisconnected() {
        if (isFetching) {
            timerTask.purge();
            isFetching = false;
        }
        if (!isShowingDisconnectedNotification) {
            NotificationUtil.showDisconnectedNotification(this);
            isShowingDisconnectedNotification = true;
        }
        timerTask.schedule(new ReconnectTask(), READING_TICK_MS);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(ALARM_ENABLED)) {
            alarmEnabled = sharedPreferences.getBoolean(ALARM_ENABLED, false);
        }
    }

    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    public void connectToDevice(BluetoothDevice device) {
        if (gatt == null) {
            gatt = device.connectGatt(this, false, gattCallback);
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            deviceState = newState;
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    gatt.discoverServices();
                    break;
                default:
                    BluetoothService.this.gatt = null;
                    onDisconnected();
                    break;
            }
            LocalBroadcastManager.getInstance(BluetoothService.this)
                    .sendBroadcast(new Intent(DEVICE_STATE_CHANGED));
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService service : gatt.getServices()) {
                    if (COMMUNICATION_SERVICE_UUID.equalsIgnoreCase(service.getUuid().toString())) {
                        gattService = service;
                        Log.i(TAG, "found service " + service.getUuid().toString());
                        for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                            if (characteristic.getUuid().toString().equalsIgnoreCase(TX_RX_UUID)) {
                                Log.i(TAG, "found comm characteristic");
                                characteristicComm = characteristic;
                                setCharacteristicNotification(characteristicComm, true);
                                onConnected();
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
                                                 characteristic, int status) {}

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            String response = new String(characteristic.getValue());
            Log.i(TAG, response);
            if (response.startsWith(AT_RESPONSE)) {
                String value = response.substring(AT_RESPONSE.length());
                Log.w("LOL", "response is " + value);
                try {
                    float temperature = Float.valueOf(value);
                    if (temperature != ERROR_TEMPERATURE)
                        updateTemperature(temperature);
                } catch (NumberFormatException e) {
                    Log.e(TAG, e.getMessage());
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
        alarmActivated = false;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    private void updateTemperature(float temperature) {
        currentTemperature = temperature;
        int x = xVal++;
        TemperaturePoint entry = new TemperaturePoint(temperature, new Date().getTime());
        entries.add(entry);
        if (!alarmActivated && alarmEnabled) {
            if (currentTemperature >= desiredTemperature && direction == DIRECTION_ASCENDING)
                startAlarm();
            else if (currentTemperature <= desiredTemperature && direction == DIRECTION_DESCENDING)
                startAlarm();
        }
        Intent intent = new Intent(TEMPERATURE_UPDATED);
        intent.putExtra(CURRENT_TEMPERATURE, temperature);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        NotificationUtil.updateNotification(
                this, currentTemperature, deviceAddress, desiredTemperature, alarmEnabled
        );
    }

    public CircularFifoQueue<TemperaturePoint> getEntries() {
        return entries;
    }

    private void startAlarm() {
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                0 //PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, 0, pendingIntent);
        alarmActivated = true;
        Log.i(TAG, "alarm activated");
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || gatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            connectToDevice(device);
            return;
        }
        Log.i(TAG, "writing " + characteristic.getStringValue(0));
        gatt.writeCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (bluetoothAdapter == null || gatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        gatt.setCharacteristicNotification(characteristic, enabled);
    }

    private void sendCommand(String cmd) {
        characteristicComm.setValue(cmd.getBytes());
        writeCharacteristic(characteristicComm);
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public int getDeviceState() {
        return deviceState;
    }

    class FetchTemperatureTask extends TimerTask {

        @Override
        public void run() {
            sendCommand(GET_TEMPERATURE_COMMAND);
            timerTask.schedule(new FetchTemperatureTask(), READING_TICK_MS);
        }
    }

    class ReconnectTask extends TimerTask {

        @Override
        public void run() {
            connectToDevice(device);
            timerTask.schedule(new ReconnectTask(), READING_TICK_MS);
        }
    }
}
