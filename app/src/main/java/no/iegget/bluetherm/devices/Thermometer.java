package no.iegget.bluetherm.devices;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import no.iegget.bluetherm.BluetoothService;

/**
 * Created by iver on 21/04/16.
 */
public abstract class Thermometer {

    public BluetoothService mService;
    public boolean mBound = false;
    private Context mContext;
    public final String COMMUNCATION_SERVICE = "";
    public final String TX_RX_UUID = "";
    public ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    public Thermometer(Context mContext) {
        this.mContext = mContext;
        Intent intent = new Intent(mContext, BluetoothService.class);
        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public float getTemperature() {
        return 0F;
    }

    public String getCommunicationService() { return COMMUNCATION_SERVICE; }

    public String getTxRxUUID() { return TX_RX_UUID; }
}
