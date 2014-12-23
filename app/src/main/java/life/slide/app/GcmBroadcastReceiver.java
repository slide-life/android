package life.slide.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class GcmBroadcastReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = "Slide -> GCM";
    @Override
    public void onReceive(Context context, Intent intent) {
        final String registrationId = intent.getStringExtra("registration_id");
        if (registrationId != null && !registrationId.isEmpty())
            Log.i(TAG, "Got id: " + registrationId);
        // Explicitly specify that GcmIntentService will handle the intent.
        ComponentName comp = new ComponentName(context.getPackageName(),
                GcmIntentService.class.getName());
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
    }
}
