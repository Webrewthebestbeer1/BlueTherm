package no.iegget.bluetherm.devices;

import android.bluetooth.BluetoothDevice;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by iver on 23/04/16.
 */
public class Dummy implements Thermometer {
    @Override
    public float getTemperature() {
        return ThreadLocalRandom.current().nextLong(20, 95);
    }

    @Override
    public BluetoothDevice getDevice() {
        return null;
    }
}
