package no.iegget.bluetherm.utils;

/**
 * Created by iver on 24/04/16.
 */
public class BluetoothConnectionEvent {

    private String name;
    private String status;

    public BluetoothConnectionEvent(String name, String status) {
        this.name = name;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }
}
