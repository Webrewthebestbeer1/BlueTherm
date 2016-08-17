package no.iegget.bluetherm;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.github.mikephil.charting.data.Entry;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.util.Collections;

import no.iegget.bluetherm.ui.AlarmActivity;
import no.iegget.bluetherm.ui.ChartFragment;
import no.iegget.bluetherm.ui.ControlsFragment;
import no.iegget.bluetherm.ui.DeviceScanActivity;
import no.iegget.bluetherm.ui.view.PagerAdapter;
import no.iegget.bluetherm.utils.TemperaturePoint;

public class MainActivity extends AppCompatActivity implements
        ControlsFragment.ControlsFragmentListener,
        ChartFragment.ChartFragmentListener {

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_SCAN_FOR_DEVICES = 2;
    public static final int REQUEST_RINGTONE = 999;
    public static final String SHARED_PREFERENCES_NAME = "BLUETHERM";

    private Toolbar toolbar;
    private TabLayout tabLayout;

    private Intent bluetoothServiceIntent;
    private BluetoothService bluetoothService;
    private boolean bound = false;
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            BluetoothService.LocalBinder binder = (BluetoothService.LocalBinder) service;
            bluetoothService = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        setupTabLayout();
        setSupportActionBar(toolbar);
        bluetoothServiceIntent = new Intent(this, BluetoothService.class);
    }

    private void setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab()
                .setText("Controls")
                .setIcon(R.drawable.ic_settings_white_24dp)
        );
        tabLayout.addTab(tabLayout.newTab()
                .setText("Chart")
                .setIcon(R.drawable.ic_timeline_white_24dp)
        );
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        final PagerAdapter adapter = new PagerAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void bindViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        if (bluetoothAdapter == null) {
            alertNoBluetoothAvailable();
        }
        else if (!bluetoothAdapter.isEnabled()) {
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        }
        else if (bluetoothAdapter.isEnabled()) {
            // use device from shared preferences
            if (!getAddressFromSharedPreferences().equals(BluetoothService.NO_ADDRESS)) {
                Log.i("Main", "using address " + getAddressFromSharedPreferences());
                bindService(bluetoothServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            // scan for devices
            } else {
                Log.i("Main", "no device in shared preferences. starting scan");
                Intent scanIntent = new Intent(this, DeviceScanActivity.class);
                startActivityForResult(scanIntent, REQUEST_SCAN_FOR_DEVICES);
            }
        }
    }

    private void alertNoBluetoothAvailable() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.no_bluetooth_title)
                .setMessage(R.string.no_bluetooth_message)
                .setCancelable(false)
                .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exitApplication();
                    }
                })
                .show().setCanceledOnTouchOutside(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void exitApplication() {
        if (bound)
            bluetoothService.cancelNotifications();
        this.finish();
        System.exit(0);
    }

    private String getAddressFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(
                SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
        return sharedPreferences.getString(BluetoothService.DEVICE_ADDRESS, BluetoothService.NO_ADDRESS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_exit:
                stopService(bluetoothServiceIntent);
                exitApplication();
                return true;
            case R.id.action_forget_device:
                forgetDevice();
                return true;
            case R.id.action_sound:
                showSoundSelector();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSoundSelector() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(
                RingtoneManager.EXTRA_RINGTONE_TITLE,
                getString(R.string.select_sound_title)
        );
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        startActivityForResult(intent, REQUEST_RINGTONE);
    }

    private void forgetDevice() {
        new AlertDialog.Builder(MainActivity.this)
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
                        SharedPreferences sharedPref = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                        sharedPref.edit().remove(BluetoothService.DEVICE_ADDRESS).commit();
                        Intent mStartActivity = new Intent(getApplicationContext(), MainActivity.class);
                        int mPendingIntentId = 123456;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        stopService(new Intent(getApplicationContext(), BluetoothService.class));
                        System.exit(0);
                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_RINGTONE) {
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uri != null) {
                    String ringTonePath = uri.toString();
                    SharedPreferences sharedPreferences = getSharedPreferences(
                            SHARED_PREFERENCES_NAME,
                            Context.MODE_PRIVATE
                    );
                    sharedPreferences.edit()
                            .putString(AlarmActivity.RINGTONE_URI, ringTonePath).apply();
                }
            }
        }
    }

    @Override
    public void setDirection(int direction) {
        if (bound)
            bluetoothService.setDirection(direction);
    }

    @Override
    public void onDesiredTemperatureChanged(float temperature) {
        if (bound)
            bluetoothService.setDesiredTemperature(temperature);
    }

    @Override
    public BluetoothDevice getDevice() {
        if (bound)
            return bluetoothService.getDevice();
        return null;
    }

    @Override
    public int getDeviceState() {
        if (bound)
            return bluetoothService.getDeviceState();
        else return 0;
    }

    @Override
    public CircularFifoQueue<TemperaturePoint> getEntries() {
        if (bound)
            return bluetoothService.getEntries();
        return new CircularFifoQueue<>();
    }
}
