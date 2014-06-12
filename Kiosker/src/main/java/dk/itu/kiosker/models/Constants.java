package dk.itu.kiosker.models;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.utils.CustomerErrorLogger;
import dk.itu.kiosker.utils.RootHelper;
import rx.Observable;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

// Constants and other device specific settings.
public class Constants {
    public static long NAVIGATION_ANIMATION_TIME_MILLISECONDS = 200;
    public static long NAVIGATION_ONSCREEN_TIME_SECONDS = 3;
    public static final String KIOSKER_SSID_ID = "kiosker_ssid_id";
    public static String TAG = "Kiosker";
    public static String BASE_SETTINGS = "base";
    public static String FILE_ENDING = ".json";
    public static String KIOSKER_RESET_DEVICE_ID = "resetDevice";
    public static String SAFE_JSON = "kiosker_safe_json";
    public static String KIOSKER_KILL_APP_ID = "Kill Kiosker";
    public static String KIOSKER_HOME_URL_ID = "kiosker_home_url_id";
    public static String KIOSKER_JSON_BASE_URL_ID = "kiosker_json_base_url";
    public static String KIOSKER_ALLOW_HOME_ID = "kiosker_allow_home_id";
    public static String KIOSKER_RESET_WEBCACHE = "kiosker_reset_webcache";
    public static String KIOSKER_MANUAL_WIFI = "kiosker_reset_webcache";
    public static String KIOSKER_DEVICE_ID = "kiosker_device_id";
    public static String KIOSKER_DIMMED_BRIGHTNESS_ID = "kiosker_dimmed_brightness";
    public static String KIOSKER_BRIGHTNESS_ID = "kiosker_brightness_id";
    public static String KIOSKER_WRONG_OR_NO_PASSWORD_ID = "kiosker_wrong_or_no_password_id";
    public static String KIOSKER_PASSWORD_HASH_ID = "kiosker_password_hash_id";
    public static String KIOSKER_MASTER_PASSWORD_HASH_ID = "kiosker_master_password_hash_id";
    public static String KIOSKER_PASSWORD_SALT_ID = "kiosker_password_salt_id";
    public static String KIOSKER_MASTER_PASSWORD_SALT_ID = "kiosker_master_password_salt_id";
    public static String KIOSKER_LATEST_EXCEPTION_ID = "kiosker_latest_exception_id";
    public static String KIOSKER_INITIAL_RUN = "initial_run_of_application";
    public static String KIOSKER_ALLOW_SWITCHING_ID = "kiosker_allow_switching_id";

    public static String JSON_BASE_URL = "";
    public static String settingsText = "No settings loaded.";

    public static Boolean isDeviceRooted() {
        return RootHelper.checkRootMethod1() || RootHelper.checkRootMethod2() || RootHelper.checkRootMethod3();
    }

    public static Boolean hasSafeSettings(KioskerActivity kioskerActivity) {
        return !LocalSettings.getSafeJson(kioskerActivity).isEmpty();
    }

    public static boolean isNetworkAvailable(final KioskerActivity activity) {
        final boolean[] isNetworkAvailable = {false};
        ConnectivityManager connMgr = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        final boolean connected = netInfo != null && netInfo.isConnected();
        Observable.from(1L).subscribeOn(Schedulers.newThread()).toBlockingObservable().forEach(new Action1<Long>() {
            @Override
            public void call(Long aLong) {
                if (connected) {
                    try {
                        URL url = new URL("http://www.google.com");
                        HttpURLConnection http = (HttpURLConnection) url.openConnection();
                        http.setRequestProperty("User-Agent", "Android Application");
                        http.setRequestProperty("Connection", "close");
                        http.setConnectTimeout(30 * 1000);
                        http.connect();
                        isNetworkAvailable[0] = (http.getResponseCode() == 200);
                    } catch (IOException e) {
                        CustomerErrorLogger.log("Error while checking if network is available.", e, activity);
                    }
                }
            }
        });
        return isNetworkAvailable[0];
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

    public static String getString(Activity activity, String key) {
        SharedPreferences prefs = activity.getSharedPreferences(key, Context.MODE_PRIVATE);
        return prefs.getString(key, "");
    }

    public static void setString(Activity activity, String string, String key) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(key, Context.MODE_PRIVATE).edit();
        editor.putString(key, string);
        editor.apply();
        editor.commit();
    }

    public static boolean getBoolean(Activity activity, String key) {
        SharedPreferences prefs = activity.getSharedPreferences(key, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, false);
    }

    public static void setBoolean(Activity activity, Boolean bool, String key) {
        if (bool == null)
            bool = false;
        SharedPreferences.Editor editor = activity.getSharedPreferences(key, Context.MODE_PRIVATE).edit();
        editor.putBoolean(key, bool);
        editor.apply();
    }

    public static float getFloat(Activity activity, String key) {
        SharedPreferences prefs = activity.getSharedPreferences(key, Context.MODE_PRIVATE);
        return prefs.getFloat(key, 1.0f);
    }

    public static void setFloat(Activity activity, float f, String key) {
        SharedPreferences.Editor editor = activity.getSharedPreferences(key, Context.MODE_PRIVATE).edit();
        editor.putFloat(key, f);
        editor.apply();
    }
}