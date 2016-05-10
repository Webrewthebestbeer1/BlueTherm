package no.iegget.bluetherm.devices;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

/**
 * Created by iver on 23/04/16.
 */
public class Dummy extends Thermometer {

    BluetoothDevice device;
    float temperature = 10.0F;
    float direction = 1.0F;

    public Dummy(Context mContext) {
        super(mContext);
    }

    @Override
    public float getTemperature() {
        if (temperature > 80.0F) direction = -1.0F;
        if (temperature < 10.0F) direction = 1.0F;
        temperature += direction;
        return temperature;
    }
}
