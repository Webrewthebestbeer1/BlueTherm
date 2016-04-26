package no.iegget.bluetherm;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import no.iegget.bluetherm.utils.Constants;

/**
 * Created by iver on 23/04/16.
 */
public class ChartFragment extends Fragment {

    LineChart chart;
    LineDataSet dataSet;
    List<Entry> entries;
    List<String> xVals;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chart, container, false);
        chart = (LineChart) view.findViewById(R.id.chart);
        entries = new ArrayList<>();
        xVals = new ArrayList<>();
        dataSet = new LineDataSet(entries, "Temperature");
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(dataSet);
        chart.setData(new LineData(xVals, dataSets));
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        //chart.setLogEnabled(true);
        EventBus.getDefault().register(this);
        return view;
    }

    @Subscribe
    public void onEvent(final Entry entry) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("ChartFragment", "entry value: " + entry.getVal() + " at: " + entry.getXIndex());
                chart.getLineData().addXValue(String.valueOf(entry.getXIndex()));
                chart.getLineData().addEntry(entry, 0);
                chart.notifyDataSetChanged();
                chart.setVisibleXRangeMaximum(Constants.VISIBLE_ENTRIES);
                chart.moveViewToX(chart.getLineData().getXValCount() - (Constants.VISIBLE_ENTRIES));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
