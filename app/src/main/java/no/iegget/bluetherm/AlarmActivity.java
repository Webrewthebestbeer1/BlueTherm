package no.iegget.bluetherm;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import no.iegget.bluetherm.utils.Constants;
import no.iegget.bluetherm.utils.SlideButton;
import no.iegget.bluetherm.utils.SlideButtonListener;

/**
 * Created by iver on 25/04/16.
 */
public class AlarmActivity extends AppCompatActivity {

    Vibrator mVibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mVibrator.vibrate(1000000000);
        setContentView(R.layout.activity_alarm);
        //TextView desiredTemperatureText = (TextView) findViewById(R.id.desired_temperature);
        SharedPreferences sharedPref = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        //desiredTemperatureText.setText(String.valueOf(sharedPref.getFloat(Constants.DESIRED_TEMPERATURE, 50F)));
        sharedPref.edit().putBoolean(Constants.ALARM_ENABLED, false).commit();

        SlideButton dismissButton = (SlideButton) findViewById(R.id.dismiss);
        dismissButton.setSlideButtonListener(new SlideButtonListener() {
            @Override
            public void handleSlide() {
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainIntent);
                KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
                keyguardLock.reenableKeyguard();
                keyguardLock = null;
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        mVibrator.cancel();
        super.onDestroy();
    }
}
