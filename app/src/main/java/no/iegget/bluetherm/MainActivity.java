package no.iegget.bluetherm;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import no.iegget.bluetherm.ui.view.PagerAdapter;
import no.iegget.bluetherm.ui.DeviceScanActivity;
import no.iegget.bluetherm.utils.Constants;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private Intent bluetoothServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothServiceIntent = new Intent(this, BluetoothService.class);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setText("General"));
        tabLayout.addTab(tabLayout.newTab().setText("Graph"));
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
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        // check if bluetooth adapter is present on device
        if (mBluetoothAdapter == null) {
            Log.w("MainActivity", "no bluetooth available");
            new AlertDialog.Builder(this)
                    .setTitle("No Bluetooth available")
                    .setMessage("This app only works on devices with Bluetooth")
                    .setCancelable(false)
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            exitApplication();
                        }
                    })
                    .show();

        }
        // check if bluetooth adapter is enabled
        else if (!mBluetoothAdapter.isEnabled()) {
            startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT);
        }
        else if (mBluetoothAdapter.isEnabled()) {
            // use device from shared preferences
            if (!getAddressFromSharedPreferences().equals(Constants.NO_ADDRESS)) {
                Log.i("Main", "using address " + getAddressFromSharedPreferences());
                startService(bluetoothServiceIntent);
            // scan for devices
            } else {
                Log.i("Main", "no device in shared preferences. starting scan");
                Intent scanIntent = new Intent(this, DeviceScanActivity.class);
                startActivityForResult(scanIntent, Constants.SCAN_FOR_DEVICES);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void exitApplication() {
        this.finish();
        System.exit(0);
    }

    private String getAddressFromSharedPreferences() {
        SharedPreferences sharedPref = this.getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return sharedPref.getString(Constants.DEVICE_ADDRESS, Constants.NO_ADDRESS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {
            case R.id.action_exit:
                stopService(bluetoothServiceIntent);
                exitApplication();
                break;
            case R.id.action_forget_device:
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
                                sharedPref.edit().remove(Constants.DEVICE_ADDRESS).commit();
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
                break;
            case R.id.action_sound:
                Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select ringtone for notifications:");
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                MainActivity.this.startActivityForResult(intent, Constants.REQUEST_RINGTONE);
                break;
            default:
                break;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == Constants.REQUEST_RINGTONE) {
                Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                if (uri != null) {
                    String ringTonePath = uri.toString();
                    SharedPreferences sharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                    sharedPreferences.edit().putString(Constants.RINGTONE_URI, ringTonePath).commit();
                    Log.i("MainActivity", "ringtone path " + ringTonePath);
                }
            }
        }
    }
}
