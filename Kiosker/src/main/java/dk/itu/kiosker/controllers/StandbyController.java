package dk.itu.kiosker.controllers;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.utils.SettingsExtractor;
import dk.itu.kiosker.utils.Time;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

class StandbyController {
    private final KioskerActivity kioskerActivity;
    private final ArrayList<Subscriber> subscribers;
    private Subscriber<Long> idleDimSubscriber;
    private Observable<Long> idleDimObservable;
    //region Device sleep methods.
    private PowerManager.WakeLock fullWakeLock;
    private PowerManager.WakeLock partialWakeLock;
    private Subscriber<Long> standbyStartTimeSubscriber;
    private Subscriber<Long> standbyStopTimeSubscriber;

    public StandbyController(KioskerActivity kioskerActivity, ArrayList<Subscriber> subscribers) {
        this.kioskerActivity = kioskerActivity;
        this.subscribers = subscribers;
    }

    public void handleDimSettings(LinkedHashMap settings) {
        int idlePeriodMins = SettingsExtractor.getInteger(settings, "idlePeriodMins");
        if (idlePeriodMins > 0) {
            idleDimObservable = Observable.timer(idlePeriodMins, TimeUnit.MINUTES).observeOn(AndroidSchedulers.mainThread());
            idleDimObservable.subscribe(getIdleDimSubscriber());
        }

        String standbyStartTimeStr = SettingsExtractor.getString(settings, "standbyStartTime");
        String standbyStopTimeStr = SettingsExtractor.getString(settings, "standbyStopTime");
        if (!standbyStartTimeStr.isEmpty() && !standbyStopTimeStr.isEmpty()) {
            Time standbyStartTime = new Time(standbyStartTimeStr);

            // Creating a simple idleDimObservable we can define a task on.
            Observable<Long> startObservable = Observable.from(1L);

            if (standbyStartTimeSubscriber != null) {
                standbyStartTimeSubscriber.unsubscribe();
                subscribers.remove(standbyStartTimeSubscriber);
            }

            // Create a subscriber that will set start the standby period
            standbyStartTimeSubscriber = getStandbySubscriber(true);

            // Add the subscriber to our list subscribers.
            subscribers.add(standbyStartTimeSubscriber);

            // Start the task at the defined start time.
            startObservable
                    .delaySubscription(standbyStartTime.secondsUntil(), TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(standbyStartTimeSubscriber);

            Time standbyStopTime = new Time(standbyStopTimeStr);
            // Creating a simple idleDimObservable we can define a task on.
            Observable<Long> stopObservable = Observable.from(1L);

            if (standbyStopTimeSubscriber != null) {
                standbyStopTimeSubscriber.unsubscribe();
                subscribers.remove(standbyStopTimeSubscriber);
            }

            // Create a subscriber that will end the standby period
            standbyStopTimeSubscriber = getStandbySubscriber(false);

            // Add the subscriber to our list subscribers.
            subscribers.add(standbyStopTimeSubscriber);

            // Start the task at the defined start time.
            stopObservable
                    .delaySubscription(standbyStopTime.secondsUntil(), TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(standbyStopTimeSubscriber);


            if (Time.isNowBetweenTheseTimes(standbyStartTime, standbyStopTime)) {
                getStandbySubscriber(true).onNext(null);
            }
        }
    }

    public static void unDimDevice(KioskerActivity kioskerActivity) {
        WindowManager.LayoutParams params = kioskerActivity.getWindow().getAttributes();
        params.screenBrightness = -1;
        kioskerActivity.getWindow().setAttributes(params);
    }

    public static void dimDevice(KioskerActivity kioskerActivity) {
        if (!kioskerActivity.currentlyScreenSaving) {
            WindowManager.LayoutParams params = kioskerActivity.getWindow().getAttributes();
            params.screenBrightness = 0;
            kioskerActivity.getWindow().setAttributes(params);
        }
    }

    private Subscriber<Long> getStandbySubscriber(final Boolean startStandby) {
        Subscriber<Long> subscriber = new Subscriber<Long>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.e(Constants.TAG, "Error while trying to start standby subscriber.", e);
            }

            @Override
            public void onNext(Long aLong) {
                if (startStandby) {
                    Log.d(Constants.TAG, "Starting standby.");
                    kioskerActivity.currentlyInStandbyPeriod = true;
                    kioskerActivity.stopScheduledTasks();
                    removeKeepScreenOn();
                } else {
                    Log.d(Constants.TAG, "Ending standby.");
                    kioskerActivity.currentlyInStandbyPeriod = false;
                    kioskerActivity.startScheduledTasks();
                    wakeDevice();
                    unDimDevice(kioskerActivity);
                }
            }
        };
        // If it is the wake subscriber add it to the main activity, so we can keep it alive if we reach standby mode.
        if (!startStandby)
            kioskerActivity.wakeSubscriber = subscriber;
        return subscriber;
    }

    private Subscriber<Long> getIdleDimSubscriber() {
        if (idleDimSubscriber != null && !idleDimSubscriber.isUnsubscribed()) {
            idleDimSubscriber.unsubscribe();
            subscribers.remove(idleDimSubscriber);
        }
        idleDimSubscriber = new Subscriber<Long>() {
            @Override
            public void onCompleted() {
                subscribers.remove(idleDimSubscriber);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(Constants.TAG, "Error while dimming device.", e);
            }

            @Override
            public void onNext(Long aLong) {
                Log.d(Constants.TAG, "Idle time started.");
                dimDevice(kioskerActivity);
                if (!kioskerActivity.currentlyInStandbyPeriod && !kioskerActivity.currentlyScreenSaving)
                    kioskerActivity.backToMainActivity();
                if (kioskerActivity.currentlyInStandbyPeriod)
                    removeKeepScreenOn();
            }
        };
        subscribers.add(idleDimSubscriber);
        return idleDimSubscriber;
    }

    public void stopDimSubscription() {
        unDimDevice(kioskerActivity);
        if (idleDimSubscriber != null && !idleDimSubscriber.isUnsubscribed()) {
            idleDimSubscriber.unsubscribe();
            subscribers.remove(idleDimSubscriber);
        }
    }

    public void startDimSubscription() {
        // Restart the idle time out if we are not in the standby period.
        if (idleDimObservable != null && !kioskerActivity.currentlyInStandbyPeriod)
            idleDimObservable.subscribe(getIdleDimSubscriber());
            // If we are in the standby period dim the screen again after 30 secs.
        else
            Observable.timer(30, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(getIdleDimSubscriber());
    }

    /**
     * This method removes the request to keep the screen on.
     * This will make the device go to sleep after the normal screen timeout setting on the device.
     */
    void removeKeepScreenOn() {
        WindowManager.LayoutParams params = kioskerActivity.getWindow().getAttributes();
        params.flags -= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        kioskerActivity.getWindow().setAttributes(params);
    }

    void keepScreenOn() {
        WindowManager.LayoutParams params = kioskerActivity.getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        kioskerActivity.getWindow().setAttributes(params);
        createWakeLocks();
    }

    /**
     * This method creates the wake locks we need for later waking of the device.
     */
    void createWakeLocks() {
        PowerManager powerManager = (PowerManager) kioskerActivity.getSystemService(Context.POWER_SERVICE);
        fullWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), Constants.TAG + "FULL WAKE LOCK");
        partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.TAG + " PARTIAL WAKE LOCK");
    }

    /**
     * Wakes the device from sleep and sets the screen to never timeout.
     */
    void wakeDevice() {
        fullWakeLock.acquire();
        KeyguardManager keyguardManager = (KeyguardManager) kioskerActivity.getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();
        keepScreenOn();
    }

    public void handleOnResume() {
        if (fullWakeLock.isHeld()) {
            fullWakeLock.release();
        }
        if (partialWakeLock.isHeld()) {
            partialWakeLock.release();
        }
    }

    public void handleOnPause() {
        partialWakeLock.acquire();
    }
    //endregion
}
