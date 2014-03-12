package dk.itu.kiosker.controllers;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.utils.Time;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class RefreshController {
    private final MainActivity mainActivity;
    public boolean deviceShouldBeReset;
    private Subscriber<Long> shortRefreshSubscriber;
    private Subscriber<Long> settingsSubscription;

    public RefreshController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    /**
     * Start fetching settings every 12 hours.
     */
    public void startLongRefreshSubscription() {
        if (settingsSubscription != null && !settingsSubscription.isUnsubscribed())
            settingsSubscription.unsubscribe();

        // Create the action
        settingsSubscription = new Subscriber<Long>() {
            @Override
            public void onCompleted() {
                Log.d(Constants.TAG, "Got settings.");
            }

            @Override
            public void onError(Throwable e) {
                Log.e(Constants.TAG, "Error while getting settings.", e);
            }

            @Override
            public void onNext(Long aLong) {
                if (!mainActivity.userIsInteractingWithDevice) mainActivity.refreshDevice();
                else deviceShouldBeReset = true;
            }
        };

        // Update settings at 8 in the morning and 20 in the night.
        int secondsUntil8 = Time.secondsUntil(8, 0);
        int secondsUntil20 = Time.secondsUntil(20, 0);
        int secondsUntilNextUpdate = secondsUntil8 < secondsUntil20 ? secondsUntil8 : secondsUntil20;

        settingsSubscription.onNext(1L);
        Observable<Long> settingsObservable = Observable.from(1L);
        settingsObservable
                .delay(12, TimeUnit.HOURS)
                .repeat()
                .delaySubscription(secondsUntilNextUpdate, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(settingsSubscription);
    }

    public void startShortRefreshSubscription() {
        Observable.timer(5, TimeUnit.MINUTES).observeOn(AndroidSchedulers.mainThread()).subscribe(getShortRefreshSubscriber());
    }

    private Subscriber<Long> getShortRefreshSubscriber() {
        shortRefreshSubscriber = new Subscriber<Long>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                Log.e(Constants.TAG, "Error while getting a short refresh subscriber.", e);
            }

            @Override
            public void onNext(Long aLong) {
                if (!mainActivity.userIsInteractingWithDevice) {
                    deviceShouldBeReset = false;
                    mainActivity.refreshDevice();
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
