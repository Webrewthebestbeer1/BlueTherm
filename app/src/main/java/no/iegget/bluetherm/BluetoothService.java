package no.iegget.bluetherm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.util.CircularArray;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import no.iegget.bluetherm.devices.Dummy;
import no.iegget.bluetherm.devices.Thermometer;
import no.iegget.bluetherm.utils.BluetoothConnectionEvent;
import no.iegget.bluetherm.utils.Constants;

/**
 * Created by iver on 24/04/16.
 */
public class BluetoothService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice device;
    String deviceAddress;
    Thermometer mThermometer;
    CircularFifoQueue<Entry> entries;
    CircularFifoQueue<Integer> xValues;
    int xVal = 0;
    private float desiredTemperature;
    private boolean alarmActivated = false;
    private boolean alarmEnabled;
    private boolean ascending;

    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("BluetoothService", "started");
        SharedPreferences sharedPref = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        deviceAddress = sharedPref.getString(Constants.DEVICE_ADDRESS, Constants.NO_ADDRESS);
        alarmEnabled = sharedPref.getBoolean(Constants.ALARM_ENABLED, false);
        ascending = sharedPref.getBoolean(Constants.TEMP_ASCENDING, true);
        setDesiredTemperature(sharedPref.getFloat(Constants.DESIRED_TEMPERATURE, 50F));
        mBluetoothAdapter = ((BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        device.createBond();
        mThermometer = new Dummy(device);
        registerReceiver(mPairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

        entries = new CircularFifoQueue<>(Constants.VISIBLE_ENTRIES);
        xValues = new CircularFifoQueue<>(Constants.VISIBLE_ENTRIES);

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                readThermometer();
            }
        }, 0, Constants.READING_TICK, TimeUnit.MILLISECONDS);
    }

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                postBluetoothConnectionEvent();
            }
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.ALARM_ENABLED)) {
            alarmEnabled = sharedPreferences.getBoolean(Constants.ALARM_ENABLED, false);
        }
    }

    public class LocalBinder extends Binder {
        BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    private void postBluetoothConnectionEvent() {
        String bondState;
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_NONE:
                bondState = getResources().getString(R.string.disconnected);
                break;
            case BluetoothDevice.BOND_BONDING:
                bondState = getResources().getString(R.string.connecting);
                break;
            case BluetoothDevice.BOND_BONDED:
                bondState = getResources().getString(R.string.connected);
                break;
            default:
                bondState = getResources().getString(R.string.unknown);
                break;
        }
        EventBus.getDefault().post(new BluetoothConnectionEvent(device.getName(), bondState));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void setDesiredTemperature(float temperature) {
        this.desiredTemperature = temperature;
        Log.i("BluetoothService", "temp set to: " + desiredTemperature);
        alarmActivated = false;
    }

    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }

    private void readThermometer() {
        float temp = mThermometer.getTemperature();
        int x = xVal++;
        Entry entry = new Entry(temp, x);
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
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), pendingIntent);
        alarmActivated = true;
        Log.i("BluetoothService", "alarm activated");
    }

    public List<Entry> getEntriesAsList() {
        return new ArrayList<>(this.entries);
    }

    public List<Integer> getXValuesAsList() {
        return new ArrayList<>(this.xValues);
    }

}
