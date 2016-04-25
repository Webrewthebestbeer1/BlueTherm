package no.iegget.bluetherm;

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
        TextView desiredTemperatureText = (TextView) findViewById(R.id.desired_temperature);
        SharedPreferences sharedPref = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        desiredTemperatureText.setText(String.valueOf(sharedPref.getFloat(Constants.DESIRED_TEMPERATURE, 50F)));
        sharedPref.edit().putBoolean(Constants.ALARM_ENABLED, false).commit();

        Button dismissButton = (Button) findViewById(R.id.dismiss);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(mainIntent);
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
