package no.iegget.bluetherm.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import java.io.IOException;

import no.iegget.bluetherm.MainActivity;
import no.iegget.bluetherm.R;
import no.iegget.bluetherm.utils.Constants;
import no.iegget.bluetherm.ui.view.SlideButton;

public class AlarmActivity extends AppCompatActivity {

    Vibrator mVibrator;
    MediaPlayer mMediaPlayer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mVibrator.vibrate(1000000000);
        setContentView(R.layout.activity_alarm);
        //TextView desiredTemperatureText = (TextView) findViewById(R.id.desired_temperature);
        SharedPreferences sharedPref = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        //desiredTemperatureText.setText(String.valueOf(sharedPref.getFloat(Constants.DESIRED_TEMPERATURE, 50F)));
        sharedPref.edit().putBoolean(Constants.ALARM_ENABLED, false).commit();

        String defaultAlertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
        Uri alertSound = Uri.parse(sharedPref.getString(Constants.RINGTONE_URI, defaultAlertSound));
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(this, alertSound);
            final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        SlideButton dismissButton = (SlideButton) findViewById(R.id.dismiss);
        dismissButton.setSlideButtonListener(new SlideButton.SlideButtonListener() {
            @Override
            public void handleSlide() {
                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainIntent);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        mVibrator.cancel();
        mMediaPlayer.stop();
        mMediaPlayer.release();
        super.onDestroy();
    }
}
