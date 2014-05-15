package dk.itu.kiosker.utils;

import android.app.Activity;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import dk.itu.kiosker.models.Constants;

public class CustomerErrorLogger {
    public static void log(String err, Throwable e, Activity activity) {
        Log.e(Constants.TAG, err, e);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = formatter.format(new Date());
        String errorMessage = err + "\n" + time + "\n" + stackTrace;

        Constants.setString(activity, errorMessage, Constants.KIOSKER_LATEST_EXCEPTION_ID);
        logToCrashlytics(e, activity, errorMessage);
    }

    public static void logToCrashlytics(Throwable e, Activity activity, String errorMessage) {
        Crashlytics.setUserIdentifier(Constants.getString(activity, Constants.KIOSKER_DEVICE_ID));
        Crashlytics.log(errorMessage);
        Crashlytics.logException(e);
    }
}
