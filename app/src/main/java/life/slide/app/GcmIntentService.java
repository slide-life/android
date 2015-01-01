package life.slide.app;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmIntentService extends IntentService {
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "Slide -> GCM";
    private static final int REQUEST = 0;
    private static final String REQUEST_INTENT = "request-received";

    private NotificationManager mNotificationManager;
    private DataStore dataStore;

    public GcmIntentService() {
        super("GcmIntentService");

        dataStore = DataStore.getSingletonInstance(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " +
                        extras.toString());
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                int updateType = Integer.parseInt(intent.getStringExtra("type"));
                switch (updateType) {
                    case REQUEST:
                        String requestJson = intent.getStringExtra("json");
                        Log.i(TAG, "Json request received: " + requestJson);


                        //send out intent
                        Intent received = new Intent(REQUEST_INTENT);
                        received.putExtra("request", requestJson);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(received);

                    default:
                }
                // Post notification of received message.
                sendNotification("Received: " + extras.toString());
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setContentTitle("GCM Notification")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
