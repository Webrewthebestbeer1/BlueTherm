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
import no.iegget.bluetherm.MainActivity;
import no.iegget.bluetherm.R;
import no.iegget.bluetherm.utils.BluetoothConnectionEvent;
import no.iegget.bluetherm.utils.Constants;

public class ControlsFragment extends Fragment {

    private View view;
    private SharedPreferences sharedPreferences;
    private Button changeDesiredTemperatureButton;
    private TextView deviceText;
    private TextView connectionText;
    private TextView currentTemperature;
    private float setDesiredTemperature;
    private Switch alarmSwitch;
    private RadioGroup tempDirection;
    private ControlsFragmentListener listener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_general, container, false);
        sharedPreferences = getActivity().getSharedPreferences(
                Constants.SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
        bindViews();
        setupDesiredTemperature();
        setupTemperatureDirection();
        setupAlarm();
        EventBus.getDefault().register(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        alarmSwitch.setChecked(sharedPreferences.getBoolean(Constants.ALARM_ENABLED, false));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (MainActivity) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                        + " must implement ControlsFragmentListener");
        }
    }

    private void bindViews() {
        tempDirection = (RadioGroup) view.findViewById(R.id.temp_direction);
        currentTemperature = (TextView) view.findViewById(R.id.current_temperature);
        deviceText = (TextView) view.findViewById(R.id.device);
        connectionText = (TextView) view.findViewById(R.id.connection_status);
        changeDesiredTemperatureButton =
                (Button) view.findViewById(R.id.change_desired_temperature);
        alarmSwitch = (Switch) view.findViewById(R.id.alarm_activated);
    }

    private void setupDesiredTemperature() {
        setDesiredTemperature = sharedPreferences.getFloat(Constants.DESIRED_TEMPERATURE, 50F);
        changeDesiredTemperatureButton.setText(String.format(
                java.util.Locale.US,
                getString(R.string.degrees_celsius),
                setDesiredTemperature
        ));
        changeDesiredTemperatureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final EditText input = new EditText(getActivity());
                input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
                input.setRawInputType(Configuration.KEYBOARD_12KEY);
                input.setText(String.valueOf(setDesiredTemperature));

                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.change_desired_temperature)
                        .setView(input)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                setDesiredTemperature(Float.valueOf(input.getText().toString()));
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
    }

    private void setupTemperatureDirection() {
        tempDirection.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                sharedPreferences.edit().putBoolean(
                        Constants.TEMP_ASCENDING,
                        checkedId == R.id.ascending
                ).apply();
                listener.setAlarmAscending(checkedId == R.id.ascending);
            }
        });
        tempDirection.check(sharedPreferences.getBoolean(Constants.TEMP_ASCENDING, true) ? R.id.ascending : R.id.descending);
    }

    private void setupAlarm() {
        alarmSwitch.setChecked(sharedPreferences.getBoolean(Constants.ALARM_ENABLED, false));
        alarmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sharedPreferences.edit().putBoolean(Constants.ALARM_ENABLED, isChecked).apply();
            }
        });
    }

    private void setDesiredTemperature(float temperature) {
        sharedPreferences.edit().putFloat(Constants.DESIRED_TEMPERATURE, temperature).apply();
        setDesiredTemperature = temperature;
        changeDesiredTemperatureButton.setText(String.format(
                java.util.Locale.US,
                getString(R.string.degrees_celsius),
                setDesiredTemperature
        ));
        listener.onDesiredTemperatureChanged(setDesiredTemperature);
    }

    @Subscribe
    public void onEvent(final Entry entry) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentTemperature.setText(String.format(
                        java.util.Locale.US,
                        getString(R.string.degrees_celsius),
                        entry.getVal()
                ));
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

    public interface ControlsFragmentListener {
        void setAlarmAscending(boolean ascending);
        void onDesiredTemperatureChanged(float temperature);
    }
}
