package no.iegget.bluetherm.models;

/**
 * Created by iver on 22/04/16.
 */
public class BluetoothDevice {

    private String address;
    private String name;

    public BluetoothDevice(String address, String name) {
        this.address = address;
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof BluetoothDevice){
            BluetoothDevice toCompare = (BluetoothDevice) o;
            return this.getAddress().equals(toCompare.getAddress());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}
