package no.iegget.bluetherm.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import no.iegget.bluetherm.BluetoothService;
import no.iegget.bluetherm.MainActivity;
import no.iegget.bluetherm.R;
import no.iegget.bluetherm.utils.TemperaturePoint;

public class ChartFragment extends Fragment {

    LineChart chart;
    ChartFragmentListener listener;
    View view;

    private IntentFilter temperatureUpdatedFilter =
            new IntentFilter(BluetoothService.TEMPERATURE_UPDATED);
    private BroadcastReceiver temperatureUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            buildChart();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_chart, container, false);
        bindViews();
        setupChart();
        return view;
    }

    private void setupChart() {
        chart.setTouchEnabled(false);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
    }

    private void bindViews() {
        chart = (LineChart) view.findViewById(R.id.chart);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (MainActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement ChartFragmentListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(temperatureUpdatedReceiver, temperatureUpdatedFilter);
        buildChart();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity())
                .unregisterReceiver(temperatureUpdatedReceiver);
    }

    private void buildChart() {
        List<Entry> temperatureEntries = new ArrayList<>();
        final List<Long> timeEntries = new ArrayList<>();
        int xIndex = 0;
        for (TemperaturePoint entry : listener.getEntries()) {
            temperatureEntries.add(new Entry(
                    xIndex++,
                    entry.getTemperature()
            ));
            timeEntries.add(entry.getTime());
        }
        LineDataSet temperatureDataSet = new LineDataSet(temperatureEntries, "Temperature");
        temperatureDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(temperatureDataSet);
        LineData data = new LineData(dataSets);
        chart.getXAxis().setValueFormatter(new AxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                if (timeEntries.size() == 0) return null;
                return getTimeStamp(timeEntries.get((int) value % timeEntries.size()));
            }

            @Override
            public int getDecimalDigits() {
                return 0;
            }
        });
        chart.setData(data);
        chart.invalidate();
    }

    private String getTimeStamp(long date) {
        return SimpleDateFormat
                .getTimeInstance(SimpleDateFormat.SHORT)
                .format(new Date(date));
    }

    public interface ChartFragmentListener {
        CircularFifoQueue<TemperaturePoint> getEntries();
    }
}
