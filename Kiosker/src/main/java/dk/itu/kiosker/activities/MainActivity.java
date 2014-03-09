package dk.itu.kiosker.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.LinkedHashMap;

import dk.itu.kiosker.R;
import dk.itu.kiosker.controllers.ActivityController;
import dk.itu.kiosker.controllers.SettingsController;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.models.LocalSettings;
import dk.itu.kiosker.models.OnlineSettings;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class MainActivity extends Activity {
    public Boolean showingSettings = false;
    public boolean currentlyInStandbyPeriod;
    public boolean currentlyScreenSaving;
    public boolean userIsInteractingWithDevice;
    public LinearLayout mainLayout;
    private SettingsController settingsController;
    private StatusUpdater statusUpdater;

    //region Create methods.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupApplication();
    }

    /**
     * This method takes care of initializing the objects needed by this activity.
     * It also decides if we do an initial setup or a refresh of settings.
     */
    private void setupApplication() {
        settingsController = new SettingsController(this);
        mainLayout = (LinearLayout) findViewById(R.id.mainView);
        statusUpdater = new StatusUpdater(this);
        settingsController.keepScreenOn();
        if (Constants.getInitialRun(this))
            InitialSetup.start(this);
        else
            settingsController.startLongRefreshSubscription();
    }

    public void startLongRefreshSubscription() {
        settingsController.startLongRefreshSubscription();
    }

    /**
     * Clears the current view and downloads settings again.
     */
    public void refreshDevice() {
        Log.d(Constants.TAG, "Refreshing device.");
        cleanUpMainView();
        settingsController.unsubscribeScheduledTasks();
        statusUpdater.updateMainStatus("Downloading settings");
        statusUpdater.updateSubStatus("Starting download.");
        OnlineSettings.getSettings(this);
    }
    //endregion

    //region Callback method.
    /**
     * This gets called whenever we return from our secret settings activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String deviceId = data.getStringExtra(Constants.KIOSKER_DEVICE_ID);
        Constants.setDeviceId(this, deviceId);

        String baseUrl = data.getStringExtra(Constants.JSON_BASE_URL_ID);
        Constants.setJsonBaseUrl(this, baseUrl);

        Log.d(Constants.TAG, "onActivity result");

        Boolean resetDevice = data.getBooleanExtra(Constants.KIOSKER_RESET_DEVICE_ID, false);
        if (resetDevice) {
            Constants.setInitialRun(this, true);
            LocalSettings.removeSafeSettings(this);
            cleanUpMainView();
            InitialSetup.start(this);
        }

        Boolean refreshDevice = data.getBooleanExtra(Constants.KIOSKER_REFRESH_SETTINGS_ID, false);
        if (refreshDevice)
            Observable.from(1).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Integer>() {
                @Override
                public void call(Integer integer) {
                    refreshDevice();
                }
            });

        Boolean allowHome = data.getBooleanExtra(Constants.KIOSKER_ALLOW_HOME_ID, false);
        if (allowHome != Constants.getAllowHome(this)) {
            Constants.setAllowHome(this, allowHome);
            if (!allowHome)
                hideNavigationUI();
            else
                showNavigationUI();
        }

        showingSettings = false;
    }
    //endregion

    //region Life cycle methods.
    /**
     * This method gets called whenever our activity enters the background.
     * We use it to call our handler for this scenario handleMainActivityGoingAway.
     */
    @Override
    public void onStop() {
        super.onStop();
        Log.d(Constants.TAG, "onStop() called");
        showNavigationUI();
        ActivityController.handleMainActivityGoingAway(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        settingsController.handleOnResume();
        handleNavigationUI();
        Log.d(Constants.TAG, "onResume() called");
    }

    @Override
    public void onPause() {
        super.onPause();
        settingsController.handleOnPause();
        Log.d(Constants.TAG, "onPause() called");
    }


    @Override
    public void onStart() {
        super.onStart();
        if (settingsController != null)
            handleNavigationUI();
        else
            setupApplication();
        setFullScreenImmersiveMode();
        Log.d(Constants.TAG, "onStart() called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "onDestroy() called");
        showNavigationUI();
    }
    //endregion

    //region Helper methods.
    /**
     * Removes and invalidates all the views we have added to our main layout programmatically.
     */
    public void cleanUpMainView() {
        mainLayout.removeAllViews();
        settingsController.clearWebViews();
    }

    /**
     * Enable the full screen immersive mode introduced in kit kat.
     */
    private void setFullScreenImmersiveMode() {
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
                Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                startActivityForResult(i, 0);
            }
        });
    }

    public void stopScheduledTasks() {
        userIsInteractingWithDevice = true;
        settingsController.stopScheduledTasks();
    }

    public void startScheduledTasks() {
        settingsController.startScheduledTasks();
    }

    public void backToMainActivity() {
        ActivityController.backToMainActivity(this);
        settingsController.reloadWebViews();
    }

    public void handleSettings(LinkedHashMap currentSettings) {
        settingsController.handleSettings(currentSettings);
    }

    public void loadSafeSettings() {
        settingsController.loadSafeSettings();
    }

    public void addView(View view) {
        mainLayout.addView(view);
    }

    private void handleNavigationUI() {
        settingsController.handleNavigationUI();
    }

    private void showNavigationUI() {
        settingsController.showNavigationUI();
    }

    private void hideNavigationUI() {
        settingsController.hideNavigationUI();
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