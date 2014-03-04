package dk.itu.kiosker.models;

import android.app.Activity;
import android.content.SharedPreferences;

import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.utils.RootHelper;

// Constants and other device specific settings.
public class Constants {
    public static String TAG = "Kiosker";
    public static String JSON_BASE_URL = "";
    public static String BASE_SETTINGS = "kiosker_settings";
    public static String JSON_BASE_URL_ID = "json_base_url";
    public static String FILE_ENDING = ".json";
    public static String INITIAL_RUN = "initial_run_of_application";
    public static String KIOSKER_REFRESH_SETTINGS_ID = "refreshSettings";
    public static String KIOSKER_RESET_DEVICE_ID = "resetDevice";
    public static String SAFE_JSON = "kiosker_safe_json";
    public static String KIOSKER_ALLOW_HOME_ID = "kiosker_allow_home_id";
    public static String KIOSKER_DEVICE_ID = "kiosker_device_id";
    public static String settingsText = "This is a dummy settings text";

    public static Boolean getAllowHome(Activity activity) {
        SharedPreferences prefs = activity.getPreferences(activity.MODE_PRIVATE);
        Boolean allowHome = prefs.getBoolean(KIOSKER_ALLOW_HOME_ID, false);
        return allowHome;
    }

    public static void setAllowHome(Activity activity, Boolean allowHome) {
        SharedPreferences.Editor editor = activity.getPreferences(activity.MODE_PRIVATE).edit();
        editor.putBoolean(KIOSKER_ALLOW_HOME_ID, allowHome);
        editor.commit();
    }

    public static String getDeviceId(Activity activity) {
        SharedPreferences prefs = activity.getPreferences(activity.MODE_PRIVATE);
        String deviceId = prefs.getString(KIOSKER_DEVICE_ID, "");
        return deviceId;
    }

    public static void setDeviceId(Activity activity, String deviceId) {
        SharedPreferences.Editor editor = activity.getPreferences(activity.MODE_PRIVATE).edit();
        editor.putString(KIOSKER_DEVICE_ID, deviceId);
        editor.commit();
    }

    public static String getJsonBaseUrl(Activity activity) {
        SharedPreferences prefs = activity.getPreferences(activity.MODE_PRIVATE);
        String baseUrl = prefs.getString(JSON_BASE_URL_ID, "");
        return baseUrl;
    }

    public static void setJsonBaseUrl(Activity activity, String baseUrl) {
        SharedPreferences.Editor editor = activity.getPreferences(activity.MODE_PRIVATE).edit();
        editor.putString(JSON_BASE_URL_ID, baseUrl);
        editor.commit();
        JSON_BASE_URL = baseUrl;
    }

    public static Boolean getInitialRun(Activity activity) {
        SharedPreferences prefs = activity.getPreferences(activity.MODE_PRIVATE);
        return prefs.getBoolean(INITIAL_RUN, true);
    }

    public static void setInitialRun(Activity activity, Boolean initialRun) {
        SharedPreferences.Editor editor = activity.getPreferences(activity.MODE_PRIVATE).edit();
        editor.putBoolean(INITIAL_RUN, initialRun);
        editor.commit();
    }

    public static Boolean isDeviceRooted() {
        return RootHelper.checkRootMethod1() || RootHelper.checkRootMethod2() || RootHelper.checkRootMethod3();
    }

    public static Boolean hasSafeSettings(MainActivity mainActivity) {
        return !LocalSettings.getSafeJson(mainActivity).isEmpty();
    }
}