package no.iegget.bluetherm;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;

import org.greenrobot.eventbus.EventBus;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import no.iegget.bluetherm.devices.Dummy;
import no.iegget.bluetherm.devices.Thermometer;
import no.iegget.bluetherm.utils.Constants;
import no.iegget.bluetherm.utils.ThermometerReading;

/**
 * Created by iver on 24/04/16.
 */
public class BluetoothService extends Service {

    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice device;
    String deviceAddress;
    Thermometer mThermometer;
    List<Entry> entries;
    int xVal = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("BluetoothService", "started");
        SharedPreferences sharedPref = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        deviceAddress = sharedPref.getString(Constants.DEVICE_ADDRESS, Constants.NO_ADDRESS);
        mBluetoothAdapter = ((BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        mThermometer = new Dummy(device);

        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
        scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                readThermometer();
            }
        }, 0, Constants.READING_TICK, TimeUnit.MILLISECONDS);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void readThermometer() {
        Entry entry = new Entry(mThermometer.getTemperature(), xVal++);
        Log.i("BluetoothService", "device reads: " + entry.getVal());
        //entries.add(entry);
        EventBus.getDefault().post(entry);

        //Log.i("BluetoothService", "temp: " + mThermometer.getTemperature());
    }


}
