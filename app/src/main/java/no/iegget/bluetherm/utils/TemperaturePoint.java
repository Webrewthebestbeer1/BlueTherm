package no.iegget.bluetherm.utils;

public class TemperaturePoint {

    private float temperature;
    private long time;

    public TemperaturePoint(float temperature, long time) {
        this.temperature = temperature;
        this.time = time;
    }

    public float getTemperature() {
        return temperature;
    }

    public long getTime() {
        return time;
    }
}
