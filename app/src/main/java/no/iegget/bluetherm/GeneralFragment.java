package no.iegget.bluetherm;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

import no.iegget.bluetherm.utils.BluetoothConnectionEvent;
import no.iegget.bluetherm.utils.Constants;

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
        deviceText = (TextView) view.findViewById(R.id.device);
        connectionText = (TextView) view.findViewById(R.id.connection_status);
        forgetButton = (Button) view.findViewById(R.id.forget_device);
        forgetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(view.getContext())
                        .setTitle("Forget device")
                        .setMessage("This will delete all data and restart the app. Do you want to continue?")
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
                                Intent mStartActivity = new Intent(getContext(), MainActivity.class);
                                int mPendingIntentId = 123456;
                                PendingIntent mPendingIntent = PendingIntent.getActivity(getContext(), mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                                AlarmManager mgr = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
                                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                                getActivity().stopService(new Intent(getActivity(), BluetoothService.class));
                                System.exit(0);
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

    @Subscribe
    public void onEvent(final BluetoothConnectionEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceText.setText(event.getName());
                connectionText.setText(event.getStatus());
            }
        });
    }
}
