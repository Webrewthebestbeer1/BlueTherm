package no.iegget.bluetherm.utils;

import java.util.Date;

/**
 * Created by iver on 24/04/16.
 */
public class ThermometerReading {

    private float reading;
    private Date date;

    public ThermometerReading(float reading, Date date) {
        this.reading = reading;
        this.date = date;
    }

    public float getReading() {
        return reading;
    }

    public Date getDate() {
        return date;
    }
}
