package dk.itu.kiosker.controllers;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import dk.itu.kiosker.R;
import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.activities.StatusUpdater;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.models.LocalSettings;
import rx.Subscriber;

public class SettingsController {
    private MainActivity mainActivity;
    private SoundController soundController;
    private WebController webController;
    private SleepController sleepController;
    private HardwareController hardwareController;

    // List of the scheduled settings
    private ArrayList<Subscriber> subscribers;

    public SettingsController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        subscribers = new ArrayList<>();
        soundController = new SoundController(mainActivity, subscribers);
        webController = new WebController(mainActivity, subscribers);
        sleepController = new SleepController(mainActivity, subscribers);
        hardwareController = new HardwareController(mainActivity);
    }

    public void handleSettings(LinkedHashMap settings) {
        // Stop all the scheduled tasks before starting new ones.
        for (Subscriber s : subscribers)
            s.unsubscribe();

        soundController.handleSoundSettings(settings);
        webController.handleWebSettings(settings);
        sleepController.handleDimSettings(settings);
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
            StatusUpdater.updateTextView(mainActivity, R.id.downloadingTextView, ":(");
            StatusUpdater.updateTextView(mainActivity, R.id.statusTextView, "Sorry - error while downloading. Wrong base url?");
            mainActivity.createSecretMenuButton();
        } else
            StatusUpdater.removeStatusTextViews(mainActivity);

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

    public void stopScheduledTasks() {
        sleepController.stopDimSubscription();
    }

    public void startScheduledTasks() {
        sleepController.startDimSubscription();
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
        sleepController.keepScreenOn();
    }

    public void handleOnResume() {
        sleepController.handleOnResume();
    }

    public void handleOnPause() {
        sleepController.handleOnPause();
    }
}