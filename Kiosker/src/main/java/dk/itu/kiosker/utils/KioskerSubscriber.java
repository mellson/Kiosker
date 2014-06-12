package dk.itu.kiosker.utils;

import android.app.Activity;

import rx.Subscriber;

public abstract class KioskerSubscriber extends Subscriber<Long> {
    private final String errMessage;
    private Activity activity;

    public KioskerSubscriber(String errMessage, Activity activity) {
        this.errMessage = errMessage;
        this.activity = activity;
    }

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {
        CustomerErrorLogger.log("Error while retrying internet connection.", e, activity);
    }
}
