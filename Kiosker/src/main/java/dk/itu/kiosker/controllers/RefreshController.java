package dk.itu.kiosker.controllers;

import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.utils.KioskerSubscriber;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class RefreshController {
    private final KioskerActivity kioskerActivity;
    public boolean deviceShouldBeReset;
    private Subscriber<Long> shortRefreshSubscriber;

    public RefreshController(KioskerActivity kioskerActivity) {
        this.kioskerActivity = kioskerActivity;
    }

    public void startShortRefreshSubscription() {
        Observable.timer(5, TimeUnit.MINUTES).observeOn(AndroidSchedulers.mainThread()).subscribe(getShortRefreshSubscriber());
    }

    private Subscriber<Long> getShortRefreshSubscriber() {
        shortRefreshSubscriber = new KioskerSubscriber("Error while getting a short refresh subscriber.", kioskerActivity) {
            @Override
            public void onNext(Long aLong) {
                if (!kioskerActivity.userIsInteractingWithDevice) {
                    deviceShouldBeReset = false;
                    shortRefreshSubscriber.unsubscribe();
                    kioskerActivity.refreshDevice();
                } else
                    deviceShouldBeReset = true;
            }
        };
        return shortRefreshSubscriber;
    }

    public void stopShortRefreshSubscription() {
        if (shortRefreshSubscriber != null && !shortRefreshSubscriber.isUnsubscribed())
            shortRefreshSubscriber.unsubscribe();
    }
}
