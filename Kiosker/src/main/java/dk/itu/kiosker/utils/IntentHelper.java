package dk.itu.kiosker.utils;

import android.content.Intent;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;

public class IntentHelper {
    /**
     * Add the data to the intent which will start the settings activity.
     * @param i
     * @param kioskerActivity
     */
    public static void addDataToSettingsIntent(Intent i, KioskerActivity kioskerActivity) {
        i.putExtra(Constants.KIOSKER_DEVICE_ID, Constants.getDeviceId(kioskerActivity));
        i.putExtra(Constants.JSON_BASE_URL, Constants.getJsonBaseUrl(kioskerActivity));
        i.putExtra(Constants.KIOSKER_ALLOW_HOME_ID, Constants.getAllowHome(kioskerActivity));
        i.putExtra(Constants.KIOSKER_PASSWORD_HASH_ID, Constants.getPasswordHash(kioskerActivity));
        i.putExtra(Constants.KIOSKER_MASTER_PASSWORD_HASH_ID, Constants.getMasterPasswordHash(kioskerActivity));
        i.putExtra(Constants.KIOSKER_PASSWORD_SALT_ID, Constants.getPasswordSalt(kioskerActivity));
        i.putExtra(Constants.KIOSKER_MASTER_PASSWORD_SALT_ID, Constants.getMasterPasswordSalt(kioskerActivity));
    }
}
