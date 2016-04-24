package no.iegget.bluetherm.devices;

import android.bluetooth.BluetoothDevice;

/**
 * Created by iver on 21/04/16.
 */
public class HM10DS18B20 implements Thermometer {

    BluetoothDevice device;

    public HM10DS18B20(BluetoothDevice device) {
        this.device = device;
    }

    @Override
    public float getTemperature() {
        return 0;
    }

    @Override
    public BluetoothDevice getDevice() {
        return device;
    }
}
