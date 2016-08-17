package no.iegget.bluetherm.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import no.iegget.bluetherm.ui.AlarmActivity;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(alarmIntent);
    }
}
