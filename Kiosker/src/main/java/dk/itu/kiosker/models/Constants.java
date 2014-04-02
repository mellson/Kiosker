package dk.itu.kiosker.models;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.utils.RootHelper;

// Constants and other device specific settings.
public class Constants {
    public static long NAVIGATION_ANIMATION_TIME_MILLISECONDS = 200;
    public static long NAVIGATION_ONSCREEN_TIME_SECONDS = 3;
    private static final String KIOSKER_SSID_ID = "kiosker_ssid_id";
    public static String TAG = "Kiosker";
    public static String BASE_SETTINGS = "base";
    public static String JSON_BASE_URL_ID = "json_base_url";
    public static String FILE_ENDING = ".json";
    public static String KIOSKER_RESET_DEVICE_ID = "resetDevice";
    public static String SAFE_JSON = "kiosker_safe_json";
    public static String KIOSKER_KILL_APP_ID = "Kill Kiosker";
    private static String KIOSKER_HOME_URL_ID = "kiosker_home_url_id";
    public static String KIOSKER_ALLOW_HOME_ID = "kiosker_allow_home_id";
    public static String KIOSKER_DEVICE_ID = "kiosker_device_id";
    private static String KIOSKER_DIMMED_BRIGHTNESS_ID = "kiosker_dimmed_brightness";
    private static String KIOSKER_BRIGHTNESS_ID = "kiosker_brightness_id";
    public static String KIOSKER_WRONG_OR_NO_PASSWORD_ID = "kiosker_wrong_or_no_password_id";
    public static String KIOSKER_PASSWORD_HASH_ID = "kiosker_password_hash_id";
    public static String KIOSKER_MASTER_PASSWORD_HASH_ID = "kiosker_master_password_hash_id";
    public static String KIOSKER_PASSWORD_SALT_ID = "kiosker_password_salt_id";
    public static String KIOSKER_MASTER_PASSWORD_SALT_ID = "kiosker_master_password_salt_id";
    public static String JSON_BASE_URL = "";
    public static String settingsText = "No settings loaded.";
    private static String KIOSKER_LATEST_EXCEPTION_ID = "kiosker_latest_exception_id";
    private static String INITIAL_RUN = "initial_run_of_application";
    private static String KIOSKER_ALLOW_SWITCHING_ID = "kiosker_allow_switching_id";

    public static Boolean getAllowHome(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(KIOSKER_ALLOW_HOME_ID, Context.MODE_PRIVATE);
        boolean allowHome = prefs.getBoolean(KIOSKER_ALLOW_HOME_ID, false);
        return allowHome;
    }

    public static void setAllowHome(Activity activity, Boolean allowHome) {
        if (allowHome == null)
            allowHome = false;
        SharedPreferences.Editor editor = activity.getSharedPreferences(KIOSKER_ALLOW_HOME_ID, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KIOSKER_ALLOW_HOME_ID, allowHome);
        editor.apply();
    }

    public static String getDeviceId(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(KIOSKER_DEVICE_ID,Context.MODE_PRIVATE);
        return prefs.getString(KIOSKER_DEVICE_ID, "");
    }

    public static void setDeviceId(Activity activity, String deviceId) {
        if (deviceId == null)
            deviceId = "";
        SharedPreferences.Editor editor = activity.getSharedPreferences(KIOSKER_DEVICE_ID,Context.MODE_PRIVATE).edit();
        editor.putString(KIOSKER_DEVICE_ID, deviceId);
        editor.apply();
    }

    public static String getJsonBaseUrl(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(JSON_BASE_URL_ID,Context.MODE_PRIVATE);
        return prefs.getString(JSON_BASE_URL_ID, "");
    }

    public static void setJsonBaseUrl(Activity activity, String baseUrl) {
        if (baseUrl == null)
            baseUrl = "";
        SharedPreferences.Editor editor = activity.getSharedPreferences(JSON_BASE_URL_ID,Context.MODE_PRIVATE).edit();
        editor.putString(JSON_BASE_URL_ID, baseUrl);
        editor.commit();
        JSON_BASE_URL = baseUrl;
    }

    public static Boolean getInitialRun(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(INITIAL_RUN,Context.MODE_PRIVATE);
        return prefs.getBoolean(INITIAL_RUN, true);
    }

    public static void setInitialRun(Activity activity, Boolean initialRun) {
        if (initialRun == null)
            initialRun = true;
        SharedPreferences.Editor editor = activity.getSharedPreferences(INITIAL_RUN, Context.MODE_PRIVATE).edit();
        editor.putBoolean(INITIAL_RUN, initialRun);
        editor.apply();
    }

    public static Boolean isDeviceRooted() {
        return RootHelper.checkRootMethod1() || RootHelper.checkRootMethod2() || RootHelper.checkRootMethod3();
    }

    public static Boolean hasSafeSettings(KioskerActivity kioskerActivity) {
        return !LocalSettings.getSafeJson(kioskerActivity).isEmpty();
    }

    public static Boolean getAllowSwitching(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(KIOSKER_ALLOW_SWITCHING_ID, Context.MODE_PRIVATE);
        return prefs.getBoolean(KIOSKER_ALLOW_SWITCHING_ID, false);
    }

    public static void setAllowSwitching(KioskerActivity activity, Boolean allowSwitching) {
        if (allowSwitching == null)
            allowSwitching = false;
        SharedPreferences.Editor editor = activity.getSharedPreferences(KIOSKER_ALLOW_SWITCHING_ID, Context.MODE_PRIVATE).edit();
        editor.putBoolean(KIOSKER_ALLOW_SWITCHING_ID, allowSwitching);
        editor.apply();
    }

    public static String getPasswordHash(KioskerActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(KIOSKER_PASSWORD_HASH_ID, Context.MODE_PRIVATE);
        return prefs.getString(KIOSKER_PASSWORD_HASH_ID, "");
    }

    public static void setPasswordHash(KioskerActivity activity, String passwordHash) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(KIOSKER_PASSWORD_HASH_ID, Context.MODE_PRIVATE).edit();
        editor.putString(KIOSKER_PASSWORD_HASH_ID, passwordHash);
        editor.apply();
    }

    public static String getMasterPasswordHash(KioskerActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(KIOSKER_MASTER_PASSWORD_HASH_ID, Context.MODE_PRIVATE);
        return prefs.getString(KIOSKER_MASTER_PASSWORD_HASH_ID, "");
    }

    public static void setMasterPasswordHash(KioskerActivity activity, String masterPasswordHash) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(KIOSKER_MASTER_PASSWORD_HASH_ID, Context.MODE_PRIVATE).edit();
        editor.putString(KIOSKER_MASTER_PASSWORD_HASH_ID, masterPasswordHash);
        editor.apply();
    }

    public static void setPasswordSalt(KioskerActivity activity, String passwordSalt) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(KIOSKER_PASSWORD_SALT_ID, Context.MODE_PRIVATE).edit();
        editor.putString(KIOSKER_PASSWORD_SALT_ID, passwordSalt);
        editor.apply();
    }

    public static String getPasswordSalt(KioskerActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(KIOSKER_PASSWORD_SALT_ID, Context.MODE_PRIVATE);
        return prefs.getString(KIOSKER_PASSWORD_SALT_ID, "");
    }

    public static void setMasterPasswordSalt(KioskerActivity activity, String masterPasswordSalt) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(KIOSKER_MASTER_PASSWORD_SALT_ID, Context.MODE_PRIVATE).edit();
        editor.putString(KIOSKER_MASTER_PASSWORD_SALT_ID, masterPasswordSalt);
        editor.apply();
    }

    public static String getMasterPasswordSalt(KioskerActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(KIOSKER_MASTER_PASSWORD_SALT_ID, Context.MODE_PRIVATE);
        return prefs.getString(KIOSKER_MASTER_PASSWORD_SALT_ID, "");
    }

    public static boolean isNetworkAvailable(KioskerActivity activity) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static String getLatestError(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(KIOSKER_LATEST_EXCEPTION_ID, Context.MODE_PRIVATE);
        return prefs.getString(KIOSKER_LATEST_EXCEPTION_ID, "");
    }

    public static void setLatestError(String errorMessage, Activity activity) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(KIOSKER_LATEST_EXCEPTION_ID, Context.MODE_PRIVATE).edit();
        editor.putString(KIOSKER_LATEST_EXCEPTION_ID, errorMessage);
        editor.apply();
    }

    public static float getBrightness(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(KIOSKER_BRIGHTNESS_ID, Context.MODE_PRIVATE);
        return prefs.getFloat(KIOSKER_BRIGHTNESS_ID, 1.0f);
    }

    public static void setBrightness(Activity activity, float brightness) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(KIOSKER_BRIGHTNESS_ID, Context.MODE_PRIVATE).edit();
        editor.putFloat(KIOSKER_BRIGHTNESS_ID, brightness);
        editor.apply();
    }

    public static float getDimmedBrightness(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(KIOSKER_DIMMED_BRIGHTNESS_ID, Context.MODE_PRIVATE);
        return prefs.getFloat(KIOSKER_DIMMED_BRIGHTNESS_ID, 1.0f);
    }

    public static void setDimmedBrightness(Activity activity, float brightness) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(KIOSKER_DIMMED_BRIGHTNESS_ID, Context.MODE_PRIVATE).edit();
        editor.putFloat(KIOSKER_DIMMED_BRIGHTNESS_ID, brightness);
        editor.apply();
    }

    public static String getHomeUrl(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(KIOSKER_HOME_URL_ID, Context.MODE_PRIVATE);
        return prefs.getString(KIOSKER_HOME_URL_ID, "");
    }

    public static void setHomeUrl(Activity activity, String homeUrl) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(KIOSKER_HOME_URL_ID, Context.MODE_PRIVATE).edit();
        editor.putString(KIOSKER_HOME_URL_ID, homeUrl);
        editor.apply();
    }

    public static void killApp(Context context) {
        Intent intent = new Intent(context, KioskerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Constants.KIOSKER_KILL_APP_ID, true);
        context.startActivity(intent);
    }

    public static void restartApp(Context context) {
        Intent intent = new Intent(context, KioskerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Constants.KIOSKER_KILL_APP_ID, false);
        context.startActivity(intent);
    }

    public static void setSSID(Activity activity, String ssid) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(KIOSKER_SSID_ID, Context.MODE_PRIVATE).edit();
        editor.putString(KIOSKER_SSID_ID, ssid);
        editor.apply();
    }

    public static String getSSID(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(KIOSKER_SSID_ID, Context.MODE_PRIVATE);
        return prefs.getString(KIOSKER_SSID_ID, "");
    }
}