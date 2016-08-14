package no.iegget.bluetherm.ui;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import no.iegget.bluetherm.BluetoothService;
import no.iegget.bluetherm.R;
import no.iegget.bluetherm.utils.BluetoothConnectionEvent;
import no.iegget.bluetherm.utils.Constants;

public class GeneralFragment extends Fragment {

    private Button changeDesiredTemperatureButton;
    private TextView deviceText;
    private TextView connectionText;
    private TextView currentTemperature;
    private float setDesiredTemperature;
    private Switch alarmSwitch;
    private RadioGroup tempDirection;

    private BluetoothService mService;
    private boolean mBound = false;

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), BluetoothService.class);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_general, container, false);

        final SharedPreferences sharedPref = getActivity().getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        setDesiredTemperature = sharedPref.getFloat(Constants.DESIRED_TEMPERATURE, 50F);

        tempDirection = (RadioGroup) view.findViewById(R.id.temp_direction);
        tempDirection.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                sharedPref.edit().putBoolean(Constants.TEMP_ASCENDING, checkedId == R.id.ascending).commit();
                if (mBound) mService.setAscending(checkedId == R.id.ascending);
            }
        });
        tempDirection.check(sharedPref.getBoolean(Constants.TEMP_ASCENDING, true) ? R.id.ascending : R.id.descending);

        alarmSwitch = (Switch) view.findViewById(R.id.alarm_activated);
        alarmSwitch.setChecked(sharedPref.getBoolean(Constants.ALARM_ENABLED, false));
        alarmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sharedPref.edit().putBoolean(Constants.ALARM_ENABLED, isChecked).commit();
            }
        });

        currentTemperature = (TextView) view.findViewById(R.id.current_temperature);
        deviceText = (TextView) view.findViewById(R.id.device);
        connectionText = (TextView) view.findViewById(R.id.connection_status);

        changeDesiredTemperatureButton = (Button) view.findViewById(R.id.change_desired_temperature);
        changeDesiredTemperatureButton.setText(String.format(java.util.Locale.US, getResources().getString(R.string.degrees_celsius), setDesiredTemperature));
        changeDesiredTemperatureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder inputDialog = new AlertDialog.Builder(getActivity());
                inputDialog.setTitle(getResources().getString(R.string.change_desired_temperature));
                final EditText input = new EditText(getActivity());
                input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                input.setRawInputType(Configuration.KEYBOARD_12KEY);
                input.setText(String.valueOf(setDesiredTemperature));
                inputDialog.setView(input);
                inputDialog.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setDesiredTemperature(Float.valueOf(input.getText().toString()));
                    }
                });
                inputDialog.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                inputDialog.show();
            }
        });

        EventBus.getDefault().register(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        final SharedPreferences sharedPref = getActivity().getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        alarmSwitch.setChecked(sharedPref.getBoolean(Constants.ALARM_ENABLED, false));
    }

    private void setDesiredTemperature(float temperature) {
        Log.i("GeneralFragment", "desired temp: " + temperature);
        SharedPreferences sharedPref = getActivity().getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        sharedPref.edit().putFloat(Constants.DESIRED_TEMPERATURE, temperature).commit();
        setDesiredTemperature = temperature;
        changeDesiredTemperatureButton.setText(String.format(java.util.Locale.US, getResources().getString(R.string.degrees_celsius), setDesiredTemperature));
        if (mBound) mService.setDesiredTemperature(setDesiredTemperature);
    }

    @Subscribe
    public void onEvent(final Entry entry) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentTemperature.setText(String.format(java.util.Locale.US, getResources().getString(R.string.degrees_celsius), entry.getVal()));
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

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.setDesiredTemperature(setDesiredTemperature);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}
