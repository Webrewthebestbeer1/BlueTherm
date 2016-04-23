package no.iegget.bluetherm.devices;

import android.bluetooth.BluetoothDevice;

/**
 * Created by iver on 21/04/16.
 */
public interface Thermometer {

    float getTemperature();
    BluetoothDevice getDevice();

}
