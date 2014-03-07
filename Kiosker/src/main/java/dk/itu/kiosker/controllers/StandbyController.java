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
import dk.itu.kiosker.utils.Time;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class StandbyController {
    private MainActivity mainActivity;
    private Subscriber idleDimSubscriber;
    private Observable idleDimObservable;
    private ArrayList<Subscriber> subscribers;

    public StandbyController(MainActivity mainActivity, ArrayList<Subscriber> subscribers) {
        this.mainActivity = mainActivity;
        this.subscribers = subscribers;
    }

    public void handleDimSettings(LinkedHashMap settings) {
        if (settings.containsKey("idlePeriodMins")) {
            int minutesUntilDim = (int) settings.get("idlePeriodMins");
            idleDimObservable = Observable.timer(minutesUntilDim, TimeUnit.MINUTES).observeOn(AndroidSchedulers.mainThread());
            idleDimObservable.subscribe(getIdleDimSubscriber());
        }
        if (settings.containsKey("standbyStartTime") && settings.containsKey("standbyStopTime")) {
            Time standbyStartTime = new Time(settings.get("standbyStartTime"));

            // Creating a simple idleDimObservable we can define a task on.
            Observable<Integer> startObservable = Observable.from(1);

            // Create a subscriber that will set the volume to 0.
            Subscriber<Integer> standbyStartTimeSubscriber = getStandbySubscriber(true);

            // Add the subscriber to our list subscribers.
            subscribers.add(standbyStartTimeSubscriber);

            // Start the task at the defined start time.
            startObservable
                    .delaySubscription(standbyStartTime.secondsUntil(), TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(standbyStartTimeSubscriber);

            Time standbyStopTime = new Time(settings.get("standbyStopTime"));
            // Creating a simple idleDimObservable we can define a task on.
            Observable<Integer> stopObservable = Observable.from(1);

            // Create a subscriber that will set the volume to 0.
            Subscriber<Integer> standbyStopTimeSubscriber = getStandbySubscriber(false);

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

    private Subscriber getStandbySubscriber(final Boolean dimScreen) {
        return new Subscriber() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.e(Constants.TAG, "Error while trying to start standby subscriber.", e);
            }

            @Override
            public void onNext(Object o) {
                if (dimScreen) {
                    mainActivity.currentlyInStandbyPeriod = true;
                    removeKeepScreenOn();
                } else {
                    mainActivity.currentlyInStandbyPeriod = false;
                    wakeDevice();
                    unDimDevice();
                }
            }
        };
    }

    /**
     * This method removes the request to keep the screen on.
     * This will make the device go to sleep after the normal screen timeout setting on the device.
     */
    public void removeKeepScreenOn() {
        WindowManager.LayoutParams params = mainActivity.getWindow().getAttributes();
        params.flags -= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        mainActivity.getWindow().setAttributes(params);
    }

    private Subscriber getIdleDimSubscriber() {
        idleDimSubscriber = new Subscriber() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.e(Constants.TAG, "Error while dimming device.", e);
            }

            @Override
            public void onNext(Object o) {
                dimDevice();
                if (!mainActivity.currentlyInStandbyPeriod && !mainActivity.currentlyScreenSaving)
                    mainActivity.backToMainActivity();
            }
        };
        return idleDimSubscriber;
    }

    private void unDimDevice() {
        WindowManager.LayoutParams params = mainActivity.getWindow().getAttributes();
        params.alpha = 1.0f;
        params.screenBrightness = -1;
        mainActivity.getWindow().setAttributes(params);
    }

    private void dimDevice() {
        WindowManager.LayoutParams params = mainActivity.getWindow().getAttributes();
        params.screenBrightness = 0;
        mainActivity.getWindow().setAttributes(params);
    }

    public void stopDimSubscription() {
        unDimDevice();
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

    protected void keepScreenOn() {
        WindowManager.LayoutParams params = mainActivity.getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        mainActivity.getWindow().setAttributes(params);

        createWakeLocks();
    }

    //region Device sleep methods.
    private PowerManager.WakeLock fullWakeLock;
    private PowerManager.WakeLock partialWakeLock;

    /**
     * This method creates the wake locks we need for later waking of the device.
     */
    protected void createWakeLocks() {
        PowerManager powerManager = (PowerManager) mainActivity.getSystemService(Context.POWER_SERVICE);
        fullWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), Constants.TAG + "FULL WAKE LOCK");
        partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, Constants.TAG + " PARTIAL WAKE LOCK");
    }

    /**
     * Wakes the device from sleep and sets the screen to never timeout.
     */
    public void wakeDevice() {
        fullWakeLock.acquire();
        KeyguardManager keyguardManager = (KeyguardManager) mainActivity.getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();
        keepScreenOn();
        mainActivity.startScheduledTasks();
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
