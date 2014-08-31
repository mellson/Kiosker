package dk.itu.kiosker.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;

public class KioskerUpdateReceiver extends BroadcastReceiver {

    /**
     * Restarts Kiosker after an update.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String nameOfUpdatedApp = intent.getDataString();
        if (nameOfUpdatedApp.contains("dk.itu.kiosker")) {
            Log.d(Constants.TAG, "Restarting Kiosker after an update");
            Intent i = new Intent(context, KioskerActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
