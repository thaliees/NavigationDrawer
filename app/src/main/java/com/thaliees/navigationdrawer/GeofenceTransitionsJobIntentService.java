package com.thaliees.navigationdrawer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class GeofenceTransitionsJobIntentService extends JobIntentService {
    private static final int JOB_ID = 2000;
    private static final String CHANNEL_ID = "Notification_Geofence";
    private List<Geofence> data = new ArrayList<>();

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, GeofenceTransitionsJobIntentService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent.hasError()) return;

        // In my case, I want to execute actions depending on the geofence in which the user is, so
        // I create the geofence list that MapsActivity generates.
        // Note. You may want to use SharedPreferences or SQLite to save this data from MapsActivity and access it here easily without rebuilding this data
        getGeofences();

        eventHandle(geofencingEvent);
    }

    private List<LatLng> createGeofences(){
        List<LatLng> points = new ArrayList<>();
        points.add(new LatLng(16.773577, -93.112314));
        points.add(new LatLng(16.771954, -93.1122589));
        points.add(new LatLng(16.7664486, -93.1160911));
        points.add(new LatLng(16.784028, -93.111808));
        points.add(new LatLng(16.7848428, -93.1139568));
        points.add(new LatLng(16.7551746, -93.1235983));

        return points;
    }

    private void getGeofences() {
        List<LatLng> points = createGeofences();
        // Create a geofence or a list of geofences. Use coordinates near your location.
        // Note: On single-user devices, there is a limit of 100 geofences per app. For multi-user devices, the limit is 100 geofences per app per device user.
        int count = 1;
        for (LatLng region : points) {
            data.add(new Geofence.Builder().
                    setRequestId("Point " + count).
                    setCircularRegion(region.latitude, region.longitude, 100).
                    setExpirationDuration(Geofence.NEVER_EXPIRE).
                    setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT).
                    setLoiteringDelay(1).
                    build());
            count++;
        }
    }

    private void eventHandle(GeofencingEvent geofencingEvent){
        // Get the geofences that were triggered. A single event can trigger multiple geofences.
        List<Geofence> trigger = geofencingEvent.getTriggeringGeofences();
        // Get the transition details as a String.
        String details = getGeofenceDetails(trigger);

        String text = "";
        int transition = geofencingEvent.getGeofenceTransition();
        switch (transition){
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                text = "You are here: " + details + " and is constant!";
                break;

            case Geofence.GEOFENCE_TRANSITION_EXIT:
                text =  details + " say: Bye, user!";
                break;

            case Geofence.GEOFENCE_TRANSITION_ENTER:
                text = "Welcome to " + details + ", user!";
                break;
        }
        // Send notification and log the transition details.
        sendNotification(text);
    }

    private String getGeofenceDetails(List<Geofence> triggeringGeofences) {
        // Get the Ids of each geofence that was triggered.
        ArrayList<String> gIdsList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            gIdsList.add(geofence.getRequestId());
        }

        String gIdsString = TextUtils.join(", ",  gIdsList);

        String custom = ". " +dataInfo(gIdsList);
        return gIdsString + custom;
    }

    private String dataInfo(ArrayList<String> ids){
        for (int i = 0; i < ids.size(); i++) {
            for (Geofence idG : data) {
                if (ids.get(i).equals(idG.getRequestId()))
                    return "(Play x info)";
            }
        }
        return "";
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MapsActivity.
     */
    private void sendNotification(String message) {
        // Intent to start the Maps Activity
        Intent notificationIntent = MapsActivity.makeNotificationIntent(this);

        // Now we need a task stack builder that we can add the notification intent to.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MapsActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        // Get the pending intent from the stack builder
        PendingIntent notificationPendingIntent= stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_menu_location)
                        .setContentTitle(getString(R.string.app_name))
                        .setColor(getResources().getColor(R.color.colorPrimary))
                        .setContentText(message)
                        .setContentIntent(notificationPendingIntent)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }

        mNotificationManager.notify(0, notificationBuilder.build());
    }
}
