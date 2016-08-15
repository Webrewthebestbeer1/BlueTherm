package no.iegget.bluetherm;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
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
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import no.iegget.bluetherm.ui.ControlsFragment;
import no.iegget.bluetherm.ui.view.PagerAdapter;
import no.iegget.bluetherm.ui.DeviceScanActivity;
import no.iegget.bluetherm.utils.Constants;

public class MainActivity extends AppCompatActivity implements
        ControlsFragment.ControlsFragmentListener {

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
            //bluetoothService.setDesiredTemperature(setDesiredTemperature);
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
                .setText("Graph")
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
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        if (bluetoothAdapter == null) {
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
                    .show();

        }
        else if (!bluetoothAdapter.isEnabled()) {
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
        }
        else if (bluetoothAdapter.isEnabled()) {
            // use device from shared preferences
            if (!getAddressFromSharedPreferences().equals(Constants.NO_ADDRESS)) {
                Log.i("Main", "using address " + getAddressFromSharedPreferences());
                bindService(bluetoothServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            // scan for devices
            } else {
                Log.i("Main", "no device in shared preferences. starting scan");
                Intent scanIntent = new Intent(this, DeviceScanActivity.class);
                startActivityForResult(scanIntent, Constants.SCAN_FOR_DEVICES);
            }
        }
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
        this.finish();
        System.exit(0);
    }

    private String getAddressFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(
                Constants.SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE
        );
        return sharedPreferences.getString(Constants.DEVICE_ADDRESS, Constants.NO_ADDRESS);
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
        startActivityForResult(intent, Constants.REQUEST_RINGTONE);
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
                        SharedPreferences sharedPref = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                        sharedPref.edit().remove(Constants.DEVICE_ADDRESS).apply();
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
            if (requestCode == Constants.REQUEST_RINGTONE) {
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uri != null) {
                    String ringTonePath = uri.toString();
                    SharedPreferences sharedPreferences = getSharedPreferences(
                            Constants.SHARED_PREFERENCES_NAME,
                            Context.MODE_PRIVATE
                    );
                    sharedPreferences.edit()
                            .putString(Constants.RINGTONE_URI, ringTonePath).apply();
                }
            }
        }
    }

    @Override
    public void setAlarmAscending(boolean ascending) {
        if (bound)
            bluetoothService.setAscending(ascending);
    }

    @Override
    public void onDesiredTemperatureChanged(float temperature) {
        if (bound)
            bluetoothService.setDesiredTemperature(temperature);
    }
}
