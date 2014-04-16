package dk.itu.kiosker.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.crashlytics.android.Crashlytics;

import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.R;
import dk.itu.kiosker.controllers.ActivityController;
import dk.itu.kiosker.controllers.HardwareController;
import dk.itu.kiosker.controllers.SettingsController;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.models.LocalSettings;
import dk.itu.kiosker.models.OnlineSettings;
import dk.itu.kiosker.utils.CustomerErrorLogger;
import dk.itu.kiosker.utils.IntentHelper;
import dk.itu.kiosker.utils.Pinger;
import dk.itu.kiosker.utils.WifiController;
import dk.itu.kiosker.web.WebViewCacheDeleter;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class KioskerActivity extends Activity {
    public boolean currentlyInStandbyPeriod;
    public boolean currentlyScreenSaving;
    public boolean userIsInteractingWithDevice;
    public Subscriber<Long> wakeSubscriber;
    public LinearLayout mainLayout;
    public SettingsController settingsController;
    private StatusUpdater statusUpdater;
    private Subscriber<Long> noInternetSubscriber;

    //region Create methods.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Constants.TAG, "onCreate() called");


        // Check if we should kill the app.
        if (getIntent().getBooleanExtra(Constants.KIOSKER_KILL_APP_ID, false)) {
            HardwareController.showNavigationUI();
            finish();
            return;
        }

        WebViewCacheDeleter.deleteWebViewCache(this);

        Crashlytics.start(this);
        Crashlytics.setUserIdentifier(Constants.getDeviceId(this));
        Pinger.start(this);

        setContentView(R.layout.activity_main);
        setupApplication();
    }

    /**
     * This method takes care of initializing the objects needed by this activity.
     * It also decides if we do an initial setup or a refresh of settings.
     */
    private void setupApplication() {
        mainLayout = (LinearLayout) findViewById(R.id.mainView);
        settingsController = new SettingsController(this);
        statusUpdater = new StatusUpdater(this);
        settingsController.keepScreenOn();
        if (Constants.getInitialRun(this))
            InitialSetup.start(this);
        else
            settingsController.startLongRefreshSubscription();
    }

    /**
     * Clears the current view and downloads settings again.
     * If there is no internet it will retry after 5 seconds.
     */
    public void refreshDevice() {
        Log.d(Constants.TAG, "Refreshing device.");
        cleanUpMainView();

        if (Constants.getInitialRun(this)) {
            InitialSetup.start(this);
            return;
        }

        // Check if we have started a subscriber for auto retrying internet connectivity.
        if (noInternetSubscriber != null && !noInternetSubscriber.isUnsubscribed())
            noInternetSubscriber.unsubscribe();
        settingsController.unsubscribeScheduledTasks();

        if (!Constants.isNetworkAvailable(this)) {
            statusUpdater.updateMainStatus("No internet");
            statusUpdater.updateSubStatus("Please refresh from settings. Auto retry in 30 seconds.");
            noInternetSubscriber = new Subscriber<Long>() {
                @Override
                public void onCompleted() {
                }

                @Override
                public void onError(Throwable e) {
                    CustomerErrorLogger.log("Error while retrying internet connection.", e, KioskerActivity.this);
                }

                @Override
                public void onNext(Long aLong) {
                    refreshDevice();
                }
            };
            String ssid = Constants.getSSID(this);
            if (!ssid.isEmpty()) {
                WifiController wifiController = new WifiController(this);
                wifiController.connectToWifi(ssid);
            }
            Observable.timer(30, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(noInternetSubscriber);
            createSecretMenuButton();
        } else {
            statusUpdater.updateMainStatus("Downloading settings");
            statusUpdater.updateSubStatus("Starting download.");
            OnlineSettings.getSettings(this);
        }
    }

    /**
     * Removes all views added to the main layout and resets all web controllers.
     */
    public void cleanUpMainView() {
        if (mainLayout != null)
            mainLayout.removeAllViews();
        if (settingsController != null) {
            settingsController.clearWebViews();
            settingsController.unsubscribeScheduledTasks();
        }
    }
    //endregion

    //region Callback method.

    /**
     * This gets called whenever we return from our secret settings activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(Constants.TAG, "onActivity result");

        // If the user entered a wrong or no password at all, just return.
        boolean wrongOrNoPassword = data.getBooleanExtra(Constants.KIOSKER_WRONG_OR_NO_PASSWORD_ID, false);
        if (wrongOrNoPassword) return;

        String deviceId = data.getStringExtra(Constants.KIOSKER_DEVICE_ID);
        Constants.setDeviceId(this, deviceId);

        String baseUrl = data.getStringExtra(Constants.JSON_BASE_URL_ID);
        Constants.setJsonBaseUrl(this, baseUrl);

        Boolean resetDevice = data.getBooleanExtra(Constants.KIOSKER_RESET_DEVICE_ID, false);
        if (resetDevice) {
            LocalSettings.removeSafeSettings(this);
            Constants.setDeviceId(this, "");
            Constants.setJsonBaseUrl(this, "");
            Constants.setInitialRun(this, true);
            Constants.setLatestError("", this);
            cleanUpMainView();
            InitialSetup.start(this);
        }
    }
    //endregion

    //region Life cycle methods.

    @Override
    public void onPause() {
        super.onPause();
        settingsController.handleOnPause();
        Log.d(Constants.TAG, "onPause() called");
    }

    /**
     * This method gets called whenever our activity enters the background.
     * We use it to call our handler for this scenario handleMainActivityGoingAway.
     */
    @Override
    public void onStop() {
        super.onStop();
        Log.d(Constants.TAG, "onStop() called");
        if (!currentlyInStandbyPeriod)
            ActivityController.handleMainActivityGoingAway(this);
        if (currentlyInStandbyPeriod) {
            settingsController.unsubscribeScheduledTasks();
            settingsController.stopScheduledTasks();
        }
        cleanUpMainView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(Constants.TAG, "onResume() called");
        settingsController.handleOnResume();
        handleNavigationUI();
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.d(Constants.TAG, "onStart() called");
        setFullScreenImmersiveMode();
        refreshDevice();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "onDestroy() called");
    }
    //endregion

    //region Helper methods.

    /**
     * Enable the full screen immersive mode introduced in kit kat.
     */
    private void setFullScreenImmersiveMode() {
        if (mainLayout != null)
            mainLayout.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
    }

    public void createSecretMenuButton() {
        Button secretMenuButton = new Button(this);
        secretMenuButton.setText("Secret Settings");
        mainLayout.addView(secretMenuButton);
        secretMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(KioskerActivity.this, SettingsActivity.class);
                IntentHelper.addDataToSettingsIntent(i, KioskerActivity.this);
                startActivityForResult(i, 0);
            }
        });
    }

    public void stopScheduledTasks() {
        settingsController.stopScheduledTasks();
    }

    public void startScheduledTasks() {
        settingsController.startScheduledTasks();
    }

    public void backToMainActivity() {
        ActivityController.backToMainActivity(this);
        settingsController.reloadWebViews();
    }

    public void handleSettings(LinkedHashMap currentSettings, boolean baseSettings) {
        settingsController.handleSettings(currentSettings, baseSettings);
    }

    public void loadSafeSettings() {
        settingsController.loadSafeSettings();
    }

    public void addView(View view, float weight) {
        mainLayout.addView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
    }

    private void handleNavigationUI() {
        settingsController.handleNavigationUI();
    }

    public void removeStatusTextViews() {
        statusUpdater.removeStatusTextViews();
    }

    public void updateMainStatus(String status) {
        statusUpdater.updateMainStatus(status);
    }

    public void updateSubStatus(String status) {
        statusUpdater.updateSubStatus(status);
    }
    //endregion
}