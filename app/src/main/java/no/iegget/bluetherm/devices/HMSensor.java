package no.iegget.bluetherm.devices;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

/**
 * Created by iver on 21/04/16.
 */
public class HMSensor extends Thermometer {

    private final String TAG = this.getClass().getSimpleName();
    private final String COMMUNICATION_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private final String TX_RX_UUID = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private final String GET_TEMPERATURE_COMMAND = "AT+TEMP?";
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;

    public HMSensor(Context mContext) {
        super(mContext);
        characteristicTX = new BluetoothGattCharacteristic(UUID.fromString(TX_RX_UUID), BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE);
        characteristicRX = new BluetoothGattCharacteristic(UUID.fromString(TX_RX_UUID), BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
    }

    public String getServiceUUID() {
        return TX_RX_UUID;
    }

    public String getCommunicationService() { return COMMUNICATION_SERVICE; }

    @Override
    public float getTemperature() {
        if (mBound) {
            String cmd = GET_TEMPERATURE_COMMAND;
            byte b = 0x00;
            byte tmp[] = cmd.getBytes();
            byte tx[] = new byte[tmp.length +1];
            tx[0] = b;
            for (int i = 1; i < tmp.length + 1; i++) {
                tx[i] = tmp[i - 1];
            }
            characteristicTX.setValue(tx);
            mService.setCharacteristicNotification(characteristicRX, true);
            mService.writeCharacteristic(characteristicTX);
        } else {
            Log.w(TAG, "not bound!");
        }

        return 0F;
    }
}
