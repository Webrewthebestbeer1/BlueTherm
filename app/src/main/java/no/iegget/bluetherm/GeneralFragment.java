package no.iegget.bluetherm;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import no.iegget.bluetherm.utils.Constants;
import no.iegget.bluetherm.utils.ThermometerReading;

/**
 * Created by iver on 23/04/16.
 */
public class GeneralFragment extends Fragment {

    Button forgetButton;
    Button disconnectButton;
    TextView deviceText;
    TextView connectionText;
    TextView currentTemperature;
    TextView desiredTemperature;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_general, container, false);
        currentTemperature = (TextView) view.findViewById(R.id.current_temperature);
        forgetButton = (Button) view.findViewById(R.id.forget_device);
        forgetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(view.getContext())
                        .setTitle("Forget device")
                        .setMessage("Do you want to continue?")
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences sharedPref = getActivity().getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                                sharedPref.edit().remove(Constants.DEVICE_ADDRESS).commit();
                                ((MainActivity) getContext()).onResume();
                            }
                        })
                        .show();
            }
        });
        disconnectButton = (Button) view.findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        EventBus.getDefault().register(this);
        return view;
    }

    @Subscribe
    public void onEvent(final Entry entry) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentTemperature.setText(String.valueOf(entry.getVal()));
            }
        });
    }
}
