package dk.itu.kiosker.controllers;

import java.util.LinkedHashMap;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.utils.CustomerErrorLogger;
import dk.itu.kiosker.utils.SettingsExtractor;

public class HardwareController {
    private static KioskerActivity kioskerActivity;
    private static Boolean hardwareSettingsParsed = false;

    public HardwareController(KioskerActivity kioskerActivity) {
        HardwareController.kioskerActivity = kioskerActivity;
    }

    // Is used to indicate whether or not the navigation ui should be hidden or not.
    private static Boolean allowHome() {
        return Constants.getAllowHome(kioskerActivity);
    }

    /**
     * This method will try to hide the navigation ui of the system.
     * That is the status bar and the soft navigation keys.
     * It requires root to work.
     */
    public static void hideNavigationUI() {
        if (!allowHome() && hardwareSettingsParsed && Constants.isDeviceRooted()) {
            try {
                String command = "LD_LIBRARY_PATH=/vendor/lib:/system/lib service call activity 42 s16 com.android.systemui";
                Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
                process.waitFor();
            } catch (Exception e) {
                CustomerErrorLogger.log("Error while trying to hide navigation ui.", e, kioskerActivity);
            }
        }
    }

    /**
     * This method will try to show the navigation ui of the system.
     * That is the status bar and the soft navigation keys.
     * It requires root to work.
     */
    public static void showNavigationUI() {
        if (Constants.isDeviceRooted()) {
            try {
                String command = "LD_LIBRARY_PATH=/vendor/lib:/system/lib am startservice -n com.android.systemui/.SystemUIService";
                Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
                process.waitFor();
            } catch (Exception e) {
                CustomerErrorLogger.log("Error while trying to show navigation ui.", e, kioskerActivity);
            }
        }
    }

    public static void handleNavigationUI() {
        if (allowHome())
            showNavigationUI();
        else
            hideNavigationUI();
    }

    public void handleHardwareSettings(LinkedHashMap settings) {
        Constants.setAllowHome(kioskerActivity, SettingsExtractor.getBoolean(settings, "allowHome"));
        hardwareSettingsParsed = true;
    }
}
