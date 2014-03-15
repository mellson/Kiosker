package dk.itu.kiosker.controllers;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.utils.SettingsExtractor;
import dk.itu.kiosker.utils.Time;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

class StandbyController {
    private final MainActivity mainActivity;
    private final ArrayList<Subscriber> subscribers;
    private Subscriber<Long> idleDimSubscriber;
    private Observable<Long> idleDimObservable;
    //region Device sleep methods.
    private PowerManager.WakeLock fullWakeLock;
    private PowerManager.WakeLock partialWakeLock;

    public StandbyController(MainActivity mainActivity, ArrayList<Subscriber> subscribers) {
        this.mainActivity = mainActivity;
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

            // Create a subscriber that will set the volume to 0.
            Subscriber<Long> standbyStartTimeSubscriber = getStandbySubscriber(true);

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

            // Create a subscriber that will set the volume to 0.
            Subscriber<Long> standbyStopTimeSubscriber = getStandbySubscriber(false);

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

    public static void unDimDevice(MainActivity mainActivity) {
        WindowManager.LayoutParams params = mainActivity.getWindow().getAttributes();
        params.screenBrightness = -1;
        mainActivity.getWindow().setAttributes(params);
    }

    public static void dimDevice(MainActivity mainActivity) {
        if (!mainActivity.currentlyScreenSaving) {
            WindowManager.LayoutParams params = mainActivity.getWindow().getAttributes();
            params.screenBrightness = 0;
            mainActivity.getWindow().setAttributes(params);
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
                    mainActivity.currentlyInStandbyPeriod = true;
                    removeKeepScreenOn();
                } else {
                    Log.d(Constants.TAG, "Ending standby.");
                    mainActivity.currentlyInStandbyPeriod = false;
                    wakeDevice();
                    unDimDevice(mainActivity);
                }
            }
        };
        // If it is the wake subscriber add it to the main activity, so we can keep it alive if we reach standby mode.
        if (!startStandby)
            mainActivity.wakeSubscriber = subscriber;
        return subscriber;
    }

    private Subscriber<Long> getIdleDimSubscriber() {
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
                dimDevice(mainActivity);
                if (!mainActivity.currentlyInStandbyPeriod && !mainActivity.currentlyScreenSaving)
                    mainActivity.backToMainActivity();
                if (mainActivity.currentlyInStandbyPeriod)
                    removeKeepScreenOn();
            }
        };
        subscribers.add(idleDimSubscriber);
        return idleDimSubscriber;
    }

    public void stopDimSubscription() {
        unDimDevice(mainActivity);
        if (idleDimSubscriber != null && !idleDimSubscriber.isUnsubscribed())
            idleDimSubscriber.unsubscribe();
    }

    public void startDimSubscription() {
        // Restart the idle time out if we are not in the standby period.
        if (idleDimObservable != null && !mainActivity.currentlyInStandbyPeriod)
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
        WindowManager.LayoutParams params = mainActivity.getWindow().getAttributes();
        params.flags -= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        mainActivity.getWindow().setAttributes(params);
    }

    void keepScreenOn() {
        WindowManager.LayoutParams params = mainActivity.getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        mainActivity.getWindow().setAttributes(params);
        createWakeLocks();
    }

    /**
     * This method creates the wake locks we need for later waking of the device.
     */
    void createWakeLocks() {
        PowerManager powerManager = (PowerManager) mainActivity.getSystemService(Context.POWER_SERVICE);
        fullWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), Constants.TAG + "FULL WAKE LOCK");
        partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.TAG + " PARTIAL WAKE LOCK");
    }

    /**
     * Wakes the device from sleep and sets the screen to never timeout.
     */
    void wakeDevice() {
        fullWakeLock.acquire();
        KeyguardManager keyguardManager = (KeyguardManager) mainActivity.getSystemService(Context.KEYGUARD_SERVICE);
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
