package no.iegget.bluetherm.devices;

import android.bluetooth.BluetoothDevice;

/**
 * Created by iver on 21/04/16.
 */
public class HM10DS18B20 implements Thermometer {
    @Override
    public float getTemperature() {
        return 0;
    }

    @Override
    public BluetoothDevice getDevice() {
        return null;
    }
}
