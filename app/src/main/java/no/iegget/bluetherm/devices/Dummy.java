package no.iegget.bluetherm.devices;

import android.bluetooth.BluetoothDevice;

/**
 * Created by iver on 23/04/16.
 */
public class Dummy implements Thermometer {

    BluetoothDevice device;
    float temperature = 10.0F;
    float direction = 1.0F;

    public Dummy(BluetoothDevice device) {
        this.device = device;
    }

    @Override
    public float getTemperature() {
        if (temperature > 80.0F) direction = -1.0F;
        if (temperature < 10.0F) direction = 1.0F;
        temperature += direction;
        return temperature;
    }

    @Override
    public BluetoothDevice getDevice() {
        return device;
    }
}
