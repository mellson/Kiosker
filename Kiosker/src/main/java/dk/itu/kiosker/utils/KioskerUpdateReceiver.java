package dk.itu.kiosker.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;

public class KioskerUpdateReceiver extends BroadcastReceiver {
    /**
     * Restart Kiosker after an update.
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG, "Restarting Kiosker after an update");
        Intent i = new Intent(context, KioskerActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
