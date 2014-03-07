package dk.itu.kiosker.controllers;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.models.LocalSettings;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class SettingsController {
    private MainActivity mainActivity;
    private SoundController soundController;
    private WebController webController;
    private StandbyController standbyController;
    private HardwareController hardwareController;

    // List of the scheduled settings
    private ArrayList<Subscriber> subscribers;

    public SettingsController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        subscribers = new ArrayList<>();
        soundController = new SoundController(mainActivity, subscribers);
        webController = new WebController(mainActivity, subscribers);
        standbyController = new StandbyController(mainActivity, subscribers);
        hardwareController = new HardwareController(mainActivity);
    }

    public void handleSettings(LinkedHashMap settings) {
        // Stop all the scheduled tasks before starting new ones.
        for (Subscriber s : subscribers)
            s.unsubscribe();

        soundController.handleSoundSettings(settings);
        webController.handleWebSettings(settings);
        standbyController.handleDimSettings(settings);
        hardwareController.handleHardwareSettings(settings);

        // Save these settings as the safe defaults.
        if (!settings.isEmpty()) {
            LocalSettings.setSafeJson(mainActivity, settings);

            // Also set that this is no longer the initial run of the application
            Constants.setInitialRun(mainActivity, false);
        }

        // Show our settings in the settings activity.
        Constants.settingsText = settings.toString();

        // If the settings are empty we have failed to get any settings.
        if (settings.isEmpty()) {
            mainActivity.updateMainStatus(":(");
            mainActivity.updateSubStatus("Sorry - error while downloading. Wrong base url?");
            mainActivity.createSecretMenuButton();
        } else
            mainActivity.removeStatusTextViews();

        // When all the settings have been parsed check to see if we should hide the ui.
        hardwareController.hideNavigationUI();
    }

    /**
     * Load and handle safe settings from local storage.
     * These settings are stored every time the app
     * successfully downloads and parses the online settings.
     */
    public void loadSafeSettings() {
        Log.d(Constants.TAG, "Loading default settings.");
        handleSettings(LocalSettings.getSafeJson(mainActivity));
    }

    // We wait 5 seconds before starting scheduled tasks after a touch event.
    Observable<Long> delayedScheduledTasksObservable = Observable.timer(5, TimeUnit.SECONDS).subscribeOn(AndroidSchedulers.mainThread());

    Subscriber<Long> delayedScheduledTasksSubscription;
    private Subscriber getDelayedScheduledTasksSubscription() {
        delayedScheduledTasksSubscription = new Subscriber<Long>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Long aLong) {
                Log.d(Constants.TAG, "Starting scheduled tasks.");
                webController.startScreenSaverSubscription();
                standbyController.startDimSubscription();
            }
        };
        return delayedScheduledTasksSubscription;
    }

    public void startScheduledTasks() {
        if (delayedScheduledTasksSubscription != null && delayedScheduledTasksSubscription.isUnsubscribed())
            delayedScheduledTasksObservable.subscribe(getDelayedScheduledTasksSubscription());
        else {
            if (delayedScheduledTasksSubscription != null)
                delayedScheduledTasksSubscription.unsubscribe();
            delayedScheduledTasksObservable.subscribe(getDelayedScheduledTasksSubscription());
        }
    }

    public void stopScheduledTasks() {
        webController.stopScreenSaverSubscription();
        standbyController.stopDimSubscription();
    }

    public void reloadWebViews() {
        webController.reloadWebViews();
    }

    public void clearWebViews() {
        webController.clearWebViews();
    }

    public void unsubscribeScheduledTasks() {
        if (subscribers != null)
            for (Subscriber s : subscribers)
                s.unsubscribe();
    }

    public void handleNavigationUI() {
        hardwareController.handleNavigationUI();
    }

    public void showNavigationUI() {
        hardwareController.showNavigationUI();
    }

    public void hideNavigationUI() {
        hardwareController.hideNavigationUI();
    }

    public void keepScreenOn() {
        standbyController.keepScreenOn();
    }

    public void handleOnResume() {
        standbyController.handleOnResume();
    }

    public void handleOnPause() {
        standbyController.handleOnPause();
    }
}