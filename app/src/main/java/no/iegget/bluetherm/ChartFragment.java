package no.iegget.bluetherm;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;

/**
 * Created by iver on 23/04/16.
 */
public class ChartFragment extends Fragment {

    LineChart chart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        chart = (LineChart) container.findViewById(R.id.chart);
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

}
