package dk.itu.kiosker.controllers;

import android.util.Log;

import java.util.LinkedHashMap;

import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.models.Constants;

public class HardwareController {
    private final MainActivity mainActivity;

    // Is used to indicate whether or not the navigation ui should be hidden or not.
    private Boolean allowHome() {
        return Constants.getAllowHome(mainActivity);
    }

    private Boolean settingsAreParsed = false;

    public HardwareController(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    /**
     * This method will try to hide the navigation ui of the system.
     * That is the status bar and the soft navigation keys.
     * It requires root to work.
     */
    public void hideNavigationUI() {
        if (!allowHome() && settingsAreParsed && Constants.isDeviceRooted()) {
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

    public void handleHardwareSettings(LinkedHashMap settings) {
        Object allowHome = settings.get("allowHome");
        if (allowHome != null)
            Constants.setAllowHome(mainActivity, (Boolean) allowHome);
        settingsAreParsed = true;
    }

    public void handleNavigationUI() {
        if (allowHome())
            showNavigationUI();
        else
            hideNavigationUI();
    }
}
