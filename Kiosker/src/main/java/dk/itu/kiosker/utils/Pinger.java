package dk.itu.kiosker.utils;

import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;

import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;
import rx.Observable;
import rx.Subscriber;

public class Pinger {
    private static Subscriber<Long> pingSubscriber;

    public static void start(KioskerActivity kioskerActivity) {
        Observable.timer(10, TimeUnit.SECONDS).repeat().subscribe(getPingSubscriber(kioskerActivity));
    }

    private static Subscriber<? super Long> getPingSubscriber(final KioskerActivity kioskerActivity) {
        if (pingSubscriber != null && !pingSubscriber.isUnsubscribed())
            pingSubscriber.unsubscribe();
        pingSubscriber = new Subscriber<Long>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                CustomerErrorLogger.log("Error while trying to ping.", e, kioskerActivity);
            }

            @Override
            public void onNext(Long aLong) {
                HttpRequest request =  HttpRequest.get(Constants.KIOSKER_PING_URL);
                if (!request.ok())
                    Log.d(Constants.TAG, "Could not ping " + Constants.KIOSKER_PING_URL);
            }
        };
        return pingSubscriber;
    }
}
