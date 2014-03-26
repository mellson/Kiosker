package dk.itu.kiosker.utils;

import android.app.Activity;

import com.flurry.android.FlurryAgent;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import dk.itu.kiosker.models.Constants;

public class CustomerErrorLogger {
    public static void log(String err, Throwable e, Activity activity) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        Format formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = formatter.format(new Date());
        String errorMessage = err + "\n" + time + "\n" + stackTrace;

        Constants.setLatestError(errorMessage, activity);
        logToGoogleAnalytics(errorMessage, e, activity);
        logToFlurry(err, errorMessage, activity, e);
    }

    public static void logToFlurry(String err, String message, Activity activity, Throwable e) {
        FlurryAgent.setUserId(Constants.getDeviceId(activity));
        FlurryAgent.onError(err, message, e.getClass().getName());
    }

    public static void logToGoogleAnalytics(String err, Throwable e, Activity activity) {
        String deviceId = Constants.getDeviceId(activity);
        EasyTracker easyTracker = EasyTracker.getInstance(activity);
        easyTracker.send(MapBuilder.createException(new StandardExceptionParser(activity, null)
                        .getDescription(deviceId + " - " + err, e), false)
                        .build()
        );
    }
}
