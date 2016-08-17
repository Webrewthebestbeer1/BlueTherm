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

import no.iegget.bluetherm.BluetoothService;
import no.iegget.bluetherm.MainActivity;
import no.iegget.bluetherm.R;
import no.iegget.bluetherm.ui.view.SlideButton;

public class AlarmActivity extends AppCompatActivity {

    public static final String RINGTONE_URI = "RINGTONE_URI";

    private Vibrator mVibrator;
    private MediaPlayer mMediaPlayer;
    private SlideButton dismissSlider;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        bindViews();
        setWindowFlags();
        setupVibrator();
        disableAlarm();
        playAlarmSound();
        setupDismissSlider();
    }

    private void bindViews() {
        dismissSlider = (SlideButton) findViewById(R.id.dismiss);
    }

    private void setupDismissSlider() {
        dismissSlider.setSlideButtonListener(new SlideButton.SlideButtonListener() {
            @Override
            public void handleSlide() {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void playAlarmSound() {
        SharedPreferences sharedPreferences =
                getSharedPreferences(MainActivity.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        String defaultAlertSound =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString();
        Uri alertSound = Uri.parse(sharedPreferences.getString(
                RINGTONE_URI,
                defaultAlertSound
        ));

        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(this, alertSound);
            final AudioManager audioManager =
                    (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disableAlarm() {
        SharedPreferences sharedPreferences =
                getSharedPreferences(MainActivity.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(BluetoothService.ALARM_ENABLED, false).apply();
    }

    private void setupVibrator() {
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mVibrator.vibrate(1000000000);
    }

    private void setWindowFlags() {
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        mVibrator.cancel();
        mMediaPlayer.stop();
        mMediaPlayer.release();
        super.onDestroy();
    }
}
