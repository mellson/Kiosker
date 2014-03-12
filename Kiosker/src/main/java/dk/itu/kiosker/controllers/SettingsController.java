package dk.itu.kiosker.controllers;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.models.LocalSettings;
import dk.itu.kiosker.utils.SettingsExtractor;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class SettingsController {
    // We wait 5 seconds before starting scheduled tasks after a touch event.
    private final Observable<Long> delayedScheduledTasksObservable = Observable.timer(5, TimeUnit.SECONDS).subscribeOn(AndroidSchedulers.mainThread());
    private final MainActivity mainActivity;
    private final SoundController soundController;
    private final WebController webController;
    private final StandbyController standbyController;
    private final HardwareController hardwareController;
    private final RefreshController refreshController;

    // List of the scheduled settings
    private final ArrayList<Subscriber> subscribers;
    private Subscriber<Long> delayedScheduledTasksSubscription;

    public SettingsController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        subscribers = new ArrayList<>();
        soundController = new SoundController(mainActivity, subscribers);
        webController = new WebController(mainActivity, subscribers);
        standbyController = new StandbyController(mainActivity, subscribers);
        hardwareController = new HardwareController(mainActivity);
        refreshController = new RefreshController(mainActivity);
    }

    public void handleSettings(LinkedHashMap settings) {
        // Stop all the scheduled tasks before starting new ones.
        for (Subscriber s : subscribers)
            s.unsubscribe();

        Constants.setPasswordHash(mainActivity, SettingsExtractor.getString(settings, "passwordHash"));
        Constants.setMasterPasswordHash(mainActivity, SettingsExtractor.getString(settings, "masterPasswordHash"));

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

    private Subscriber<Long> getDelayedScheduledTasksSubscription() {
        delayedScheduledTasksSubscription = new Subscriber<Long>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.e(Constants.TAG, "Error while starting delayed tasks.", e);
            }

            @Override
            public void onNext(Long aLong) {
                Log.d(Constants.TAG, "Restarting scheduled tasks.");
                mainActivity.userIsInteractingWithDevice = false;
                webController.startScreenSaverSubscription();
                webController.startCycleSecondarySubscription();
                standbyController.startDimSubscription();
                if (refreshController.deviceShouldBeReset)
                    refreshController.startShortRefreshSubscription();
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
        webController.stopCycleSecondarySubscription();
        standbyController.stopDimSubscription();
        refreshController.stopShortRefreshSubscription();
    }

    public void unsubscribeScheduledTasks() {
        if (mainActivity.currentlyInStandbyPeriod)
            subscribers.remove(mainActivity.wakeSubscriber);
        for (Subscriber s : subscribers)
            s.unsubscribe();
        subscribers.clear();
    }

    public void reloadWebViews() {
        webController.reloadWebViews();
    }

    public void clearWebViews() {
        webController.clearWebViews();
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

    public void startLongRefreshSubscription() {
        refreshController.startLongRefreshSubscription();
    }
}