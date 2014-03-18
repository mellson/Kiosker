package dk.itu.kiosker.models;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.utils.RootHelper;

// Constants and other device specific settings.
public class Constants {
    public static long NAVIGATION_ANIMATION_TIME_MILLISECONDS = 200;
    public static long NAVIGATION_ONSCREEN_TIME_SECONDS = 3;
    public static String TAG = "Kiosker";
    public static String BASE_SETTINGS = "base";
    public static String JSON_BASE_URL_ID = "json_base_url";
    public static String FILE_ENDING = ".json";
    public static String KIOSKER_RESET_DEVICE_ID = "resetDevice";
    public static String SAFE_JSON = "kiosker_safe_json";
    public static String KIOSKER_ALLOW_HOME_ID = "kiosker_allow_home_id";
    public static String KIOSKER_DEVICE_ID = "kiosker_device_id";
    public static String KIOSKER_PASSWORD_HASH_ID = "kiosker_password_hash_id";
    public static String KIOSKER_MASTER_PASSWORD_HASH_ID = "kiosker_master_password_hash_id";
    public static String KIOSKER_PASSWORD_SALT_ID = "kiosker_password_salt_id";
    public static String KIOSKER_MASTER_PASSWORD_SALT_ID = "kiosker_master_password_salt_id";
    private static String INITIAL_RUN = "initial_run_of_application";
    private static String KIOSKER_ALLOW_SWITCHING_ID = "kiosker_allow_switching_id";
    public static String JSON_BASE_URL = "";
    public static String settingsText = "This is a dummy settings text";

    public static Boolean getAllowHome(Activity activity) {
        SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
        return prefs.getBoolean(KIOSKER_ALLOW_HOME_ID, false);
    }

    public static void setAllowHome(Activity activity, Boolean allowHome) {
        if (allowHome == null)
            allowHome = false;
        SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
        editor.putBoolean(KIOSKER_ALLOW_HOME_ID, allowHome);
        editor.commit();
    }

    public static String getDeviceId(Activity activity) {
        SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
        return prefs.getString(KIOSKER_DEVICE_ID, "");
    }

    public static void setDeviceId(Activity activity, String deviceId) {
        if (deviceId == null)
            deviceId = "";
        SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
        editor.putString(KIOSKER_DEVICE_ID, deviceId);
        editor.commit();
    }

    public static String getJsonBaseUrl(Activity activity) {
        SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
        return prefs.getString(JSON_BASE_URL_ID, "");
    }

    public static void setJsonBaseUrl(Activity activity, String baseUrl) {
        if (baseUrl == null)
            baseUrl = "";
        SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
        editor.putString(JSON_BASE_URL_ID, baseUrl);
        editor.commit();
        JSON_BASE_URL = baseUrl;
    }

    public static Boolean getInitialRun(Activity activity) {
        SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
        return prefs.getBoolean(INITIAL_RUN, true);
    }

    public static void setInitialRun(Activity activity, Boolean initialRun) {
        if (initialRun == null)
            initialRun = true;
        SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
        editor.putBoolean(INITIAL_RUN, initialRun);
        editor.commit();
    }

    public static Boolean isDeviceRooted() {
        return RootHelper.checkRootMethod1() || RootHelper.checkRootMethod2() || RootHelper.checkRootMethod3();
    }

    public static Boolean hasSafeSettings(MainActivity mainActivity) {
        return !LocalSettings.getSafeJson(mainActivity).isEmpty();
    }

    public static Boolean getAllowSwitching(Activity activity) {
        SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
        return prefs.getBoolean(KIOSKER_ALLOW_SWITCHING_ID, false);
    }

    public static void setAllowSwitching(MainActivity activity, Boolean allowSwitching) {
        if (allowSwitching == null)
            allowSwitching = false;
        SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
        editor.putBoolean(KIOSKER_ALLOW_SWITCHING_ID, allowSwitching);
        editor.commit();
    }

    public static String getPasswordHash(MainActivity activity) {
        SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
        return prefs.getString(KIOSKER_PASSWORD_HASH_ID, "");
    }

    public static void setPasswordHash(MainActivity activity, String passwordHash) {
        SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
        editor.putString(KIOSKER_PASSWORD_HASH_ID, passwordHash);
        editor.commit();
    }

    public static String getMasterPasswordHash(MainActivity activity) {
        SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
        return prefs.getString(KIOSKER_MASTER_PASSWORD_HASH_ID, "");
    }

    public static void setMasterPasswordHash(MainActivity activity, String masterPasswordHash) {
        SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
        editor.putString(KIOSKER_MASTER_PASSWORD_HASH_ID, masterPasswordHash);
        editor.commit();
    }

    public static void setPasswordSalt(MainActivity activity, String passwordSalt) {
        SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
        editor.putString(KIOSKER_PASSWORD_SALT_ID, passwordSalt);
        editor.commit();
    }

    public static String getPasswordSalt(MainActivity activity) {
        SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
        return prefs.getString(KIOSKER_PASSWORD_SALT_ID, "");
    }

    public static void setMasterPasswordSalt(MainActivity activity, String masterPasswordSalt) {
        SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();
        editor.putString(KIOSKER_MASTER_PASSWORD_SALT_ID, masterPasswordSalt);
        editor.commit();
    }

    public static String getMasterPasswordSalt(MainActivity activity) {
        SharedPreferences prefs = activity.getPreferences(Context.MODE_PRIVATE);
        return prefs.getString(KIOSKER_MASTER_PASSWORD_SALT_ID, "");
    }
}