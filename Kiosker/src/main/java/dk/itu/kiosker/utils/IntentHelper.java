package dk.itu.kiosker.utils;

import android.content.Intent;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;

public class IntentHelper {
    /**
     * Add the data to the intent which will start the settings activity.
     *
     * @param i
     * @param kioskerActivity
     */
    public static void addDataToSettingsIntent(Intent i, KioskerActivity kioskerActivity) {
        i.putExtra(Constants.KIOSKER_DEVICE_ID, Constants.getString(kioskerActivity, Constants.KIOSKER_DEVICE_ID));
        i.putExtra(Constants.KIOSKER_JSON_BASE_URL_ID, Constants.getString(kioskerActivity, Constants.KIOSKER_JSON_BASE_URL_ID));
        i.putExtra(Constants.KIOSKER_ALLOW_HOME_ID, Constants.getBoolean(kioskerActivity, Constants.KIOSKER_ALLOW_HOME_ID));
        i.putExtra(Constants.KIOSKER_PASSWORD_HASH_ID, Constants.getString(kioskerActivity, Constants.KIOSKER_PASSWORD_HASH_ID));
        i.putExtra(Constants.KIOSKER_MASTER_PASSWORD_HASH_ID, Constants.getString(kioskerActivity, Constants.KIOSKER_MASTER_PASSWORD_HASH_ID));
        i.putExtra(Constants.KIOSKER_PASSWORD_SALT_ID, Constants.getString(kioskerActivity, Constants.KIOSKER_PASSWORD_SALT_ID));
        i.putExtra(Constants.KIOSKER_MASTER_PASSWORD_SALT_ID, Constants.getString(kioskerActivity, Constants.KIOSKER_MASTER_PASSWORD_SALT_ID));
    }
}
