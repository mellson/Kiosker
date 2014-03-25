package dk.itu.kiosker.utils;

import android.app.Activity;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;

import dk.itu.kiosker.models.Constants;

public class GoogleAnalyticsCustomerErrorLogger {
    public static void log(String err, Throwable e, Activity activity) {
        String deviceId = Constants.getDeviceId(activity);
        EasyTracker easyTracker = EasyTracker.getInstance(activity);
        easyTracker.send(MapBuilder.createException(new StandardExceptionParser(activity, null)
                        .getDescription(Thread.currentThread().getName(), e)
                        , false)
                        .set(deviceId, err)
                        .build()
        );
    }
}
