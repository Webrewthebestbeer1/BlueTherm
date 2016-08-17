package no.iegget.bluetherm.ui;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import no.iegget.bluetherm.BluetoothService;
import no.iegget.bluetherm.MainActivity;
import no.iegget.bluetherm.R;

public class ControlsFragment extends Fragment implements
        AlarmDialogFragment.AlarmDialogListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private View view;
    private SharedPreferences sharedPreferences;
    private TextView deviceText;
    private TextView connectionText;
    private TextView currentTemperature;
    private float setDesiredTemperature;
    private int direction;
    private Button alarmButton;
    private ControlsFragmentListener listener;
    private AlarmDialogFragment alarmDialog;

    private IntentFilter deviceStateFilter =
            new IntentFilter(BluetoothService.DEVICE_STATE_CHANGED);
    private BroadcastReceiver deviceStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateDeviceStatus();
        }
    };
    private IntentFilter temperatureUpdatedFilter =
            new IntentFilter(BluetoothService.TEMPERATURE_UPDATED);
    private BroadcastReceiver temperatureUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(BluetoothService.CURRENT_TEMPERATURE))
                updateTemperature(intent.getFloatExtra(BluetoothService.CURRENT_TEMPERATURE, 0F));
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_controls, container, false);
        sharedPreferences = getActivity().getSharedPreferences(
                MainActivity.SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
        bindViews();
        setupAlarm();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDeviceStatus();
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(deviceStateReceiver, deviceStateFilter);
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(temperatureUpdatedReceiver, temperatureUpdatedFilter);
        setAlarmUI(sharedPreferences.getBoolean(BluetoothService.ALARM_ENABLED, false));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity())
                .unregisterReceiver(deviceStateReceiver);
        LocalBroadcastManager.getInstance(getActivity())
                .unregisterReceiver(temperatureUpdatedReceiver);
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

    private void updateDeviceStatus() {
        updateDeviceName();
        updateDeviceState();
    }

    private void updateDeviceName() {
        BluetoothDevice device = listener.getDevice();
        if (device != null)
            deviceText.setText(device.getAddress());
        updateDeviceState();
    }

    private void updateDeviceState() {
        int state = listener.getDeviceState();
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                connectionText.setText(R.string.connected);
                break;
            case BluetoothProfile.STATE_CONNECTING:
                connectionText.setText(R.string.connecting);
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                connectionText.setText(R.string.disconnecting);
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                connectionText.setText(R.string.disconnected);
                break;
            default:
                connectionText.setText(R.string.unknown);
                break;
        }
    }

    private void bindViews() {
        currentTemperature = (TextView) view.findViewById(R.id.current_temperature);
        deviceText = (TextView) view.findViewById(R.id.device);
        connectionText = (TextView) view.findViewById(R.id.connection_status);
        alarmButton = (Button) view.findViewById(R.id.alarm_button);
    }

    private void setupAlarm() {
        setDesiredTemperature = sharedPreferences
                .getFloat(BluetoothService.DESIRED_TEMPERATURE, 50F);
        direction = sharedPreferences
                .getInt(BluetoothService.DIRECTION, BluetoothService.DIRECTION_ASCENDING);
        alarmDialog = new AlarmDialogFragment();
        alarmDialog.setListener(this);
        alarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alarmDialog.show(
                        getFragmentManager(),
                        getString(R.string.tag_alarm_dialog)
                );
            }
        });
    }

    private void updateTemperature(float temperature) {
        currentTemperature.setText(String.format(
                java.util.Locale.US,
                getString(R.string.degrees_celsius),
                temperature
        ));
    }

    private void setAlarmUI(boolean activated) {
        if (activated) {
            alarmButton.setBackground(
                    getActivity().getDrawable(R.drawable.alarm_button_activated)
            );
            alarmButton.setText(String.format(
                    getString(R.string.active_alarm_text),
                    setDesiredTemperature
            ));
            alarmButton.setTextSize(32);
            if (direction == BluetoothService.DIRECTION_DESCENDING) {
                alarmButton.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        R.drawable.ic_arrow_downward_blue_48dp,
                        0,
                        0
                );
            } else {
                alarmButton.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        0,
                        R.drawable.ic_arrow_upward_white_48dp
                );
            }
        } else {
            alarmButton.setBackground(
                    getActivity().getDrawable(R.drawable.alarm_button_deactivated)
            );
            alarmButton.setText(R.string.set_alarm);
            alarmButton.setTextSize(16);
            alarmButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    @Override
    public void onDismissAlarm() {
        setAlarmUI(false);
        sharedPreferences.edit().putBoolean(BluetoothService.ALARM_ENABLED, false).apply();
    }

    @Override
    public void onSetAlarm(float temperature, int direction) {
        listener.onDesiredTemperatureChanged(temperature);
        listener.setDirection(direction);
        sharedPreferences.edit()
                .putFloat(BluetoothService.DESIRED_TEMPERATURE, temperature).apply();
        setDesiredTemperature = temperature;
        sharedPreferences.edit().putBoolean(BluetoothService.ALARM_ENABLED, true).apply();
        savePreferences(temperature, direction);
        this.direction = direction;
        setAlarmUI(true);
    }

    private void savePreferences(float temperature, int direction) {
        sharedPreferences.edit()
                .putFloat(BluetoothService.DESIRED_TEMPERATURE, temperature).apply();
        sharedPreferences.edit().putInt(BluetoothService.DIRECTION, direction).apply();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(BluetoothService.ALARM_ENABLED)) {
            setAlarmUI(sharedPreferences.getBoolean(BluetoothService.ALARM_ENABLED, false));
        }
    }

    public interface ControlsFragmentListener {
        void setDirection(int direction);
        void onDesiredTemperatureChanged(float temperature);
        BluetoothDevice getDevice();
        int getDeviceState();
    }
}
