package dk.itu.kiosker.controllers;

import android.util.Log;

import java.util.LinkedHashMap;

import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.utils.SettingsExtractor;

class HardwareController {
    private final MainActivity mainActivity;
    private Boolean hardwareSettingsParsed = false;

    public HardwareController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void handleHardwareSettings(LinkedHashMap settings) {
        Constants.setAllowHome(mainActivity, SettingsExtractor.getBoolean(settings, "allowHome"));
        hardwareSettingsParsed = true;
    }

    // Is used to indicate whether or not the navigation ui should be hidden or not.
    private Boolean allowHome() {
        return Constants.getAllowHome(mainActivity);
    }

    /**
     * This method will try to hide the navigation ui of the system.
     * That is the status bar and the soft navigation keys.
     * It requires root to work.
     */
    public void hideNavigationUI() {
        if (!allowHome() && hardwareSettingsParsed && Constants.isDeviceRooted()) {
            try {
                String command = "LD_LIBRARY_PATH=/vendor/lib:/system/lib service call activity 42 s16 com.android.systemui";
                Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
                process.waitFor();
            } catch (Exception e) {
                Log.e(Constants.TAG, "Error while trying to hide navigation ui.", e);
            }
        }
    }

    /**
     * This method will try to show the navigation ui of the system.
     * That is the status bar and the soft navigation keys.
     * It requires root to work.
     */
    public void showNavigationUI() {
        if (Constants.isDeviceRooted()) {
            try {
                String command = "LD_LIBRARY_PATH=/vendor/lib:/system/lib am startservice -n com.android.systemui/.SystemUIService";
                Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
                process.waitFor();
            } catch (Exception e) {
                Log.e(Constants.TAG, "Error while trying to show navigation ui.", e);
            }
        }
    }

    public void handleNavigationUI() {
        if (allowHome())
            showNavigationUI();
        else
            hideNavigationUI();
    }
}
