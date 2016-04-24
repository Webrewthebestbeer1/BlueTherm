package no.iegget.bluetherm;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import no.iegget.bluetherm.utils.BluetoothConnectionEvent;
import no.iegget.bluetherm.utils.Constants;

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
        device.createBond();
        mThermometer = new Dummy(device);
        registerReceiver(mPairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        
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
        return null;
    }

    private void readThermometer() {
        Entry entry = new Entry(mThermometer.getTemperature(), xVal++);
        //Log.i("BluetoothService", "device reads: " + entry.getVal());
        EventBus.getDefault().post(entry);
    }


}
