package no.iegget.bluetherm.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.RemoteViews;

import no.iegget.bluetherm.MainActivity;
import no.iegget.bluetherm.R;

public class NotificationUtil {

    public static final int NOTIFICATION_ID = 1;
    public static final int DISCONNECTED_NOTIFICATION_ID = 2;

    public static void showDisconnectedNotification(Context context) {
        Log.w("LOL", "disc. notif.!");
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_warning_white_24dp)
                .setContent(getNotificationContentDisconnected(context))
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .setVibrate(new long[]{0, 1000})
                .setDefaults(getDefaults())
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setLights(
                        ContextCompat.getColor(context, R.color.disconnectedLedColor),
                        1000,
                        500
                )
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(DISCONNECTED_NOTIFICATION_ID, notification);
    }

    public static void updateNotification(Context context, float currentTemperature,
                                           String deviceAddress,
                                           float desiredTemperature, boolean alarmEnabled) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_timeline_white_24dp)
                .setContent(getNotificationContentConnected(context, currentTemperature))
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                //.setDefaults(getDefaults())
                .setStyle(new NotificationCompat.BigTextStyle())
                .build();
        notification.bigContentView = getNotificationContentConnectedBig(
                context, currentTemperature, deviceAddress, desiredTemperature, alarmEnabled
        );
        notification.flags |= Notification.FLAG_NO_CLEAR;
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private static int getDefaults() {
        return Notification.DEFAULT_LIGHTS
                | Notification.DEFAULT_VIBRATE
                | Notification.DEFAULT_SOUND;
    }

    private static RemoteViews getNotificationContentDisconnected(Context context) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification);
        remoteViews.setTextViewText(R.id.current_status, context.getString(R.string.disconnected));
        remoteViews.setTextColor(
                R.id.current_status,
                ContextCompat.getColor(context, R.color.redText)
        );
        return remoteViews;
    }

    private static RemoteViews getNotificationContentConnected(Context context, float currentTemperature) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.notification);
        String temperature = String.format(
                context.getString(R.string.notification_current_temperature),
                String.valueOf(currentTemperature)
        );
        remoteViews.setTextViewText(R.id.current_status, temperature);
        return remoteViews;
    }

    private static RemoteViews getNotificationContentConnectedBig(Context context,
                                                           float currentTemperature,
                                                           String deviceAddress,
                                                           float desiredTemperature,
                                                           boolean alarmEnabled) {
        RemoteViews remoteViews =
                new RemoteViews(context.getPackageName(), R.layout.notification_big);
        String temperature = String.format(
                context.getString(R.string.notification_current_temperature),
                String.valueOf(currentTemperature)
        );
        String connectedTo = String.format(
                context.getString(R.string.notification_connected_to),
                deviceAddress
        );
        String alarmSetFor = String.format(
                context.getString(R.string.notification_alarm_set_for),
                desiredTemperature
        );
        remoteViews.setTextViewText(R.id.current_status, temperature);
        remoteViews.setTextViewText(R.id.connected_to, connectedTo);
        if (alarmEnabled)
            remoteViews.setTextViewText(R.id.alarm_temperature, alarmSetFor);
        else remoteViews.setTextViewText(R.id.alarm_temperature, null);
        return remoteViews;
    }

    public static void cancelNotifications(Context context) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }
}
