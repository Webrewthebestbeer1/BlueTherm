package no.iegget.bluetherm.utils;

import java.util.List;
import java.util.UUID;

/**
 * Created by iver on 23/04/16.
 */
public class BleAdvertisedData {
    private List<UUID> mUuids;
    private String mName;
    public BleAdvertisedData(List<UUID> uuids, String name){
        mUuids = uuids;
        mName = name;
    }

    public List<UUID> getUuids(){
        return mUuids;
    }

    public String getName(){
        return mName;
    }
}
