package dk.itu.kiosker.utils;

import android.app.Activity;

public class ClearWifiFiles {
    /**
     * This call requires root
     *
     * @param activity
     */
    public static void RemoveSystemLevelWifiFiles(Activity activity) {
        try {
            // Clear wifi log files in both system and data directories
            for (int i = 0; i < 2; i++) {
                String path = i == 0 ? "/data/misc/wifi/" : "/system/etc/wifi/";
                String command = "cd " + path + "; sed -n '/network/q;p' wpa_supplicant.conf > temp.txt; cat temp.txt > wpa_supplicant.conf; rm temp.txt";
                Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
                process.waitFor();
                process.destroy();
            }
        } catch (Exception e) {
            CustomerErrorLogger.log("Error while trying to delete wifi configuration files.", e, activity);
        }
    }

    public static void RebootDevice(Activity activity) {
        try {
            Runtime.getRuntime().exec(new String[]{"su","-c","reboot now"});
        } catch (Exception e) {
            CustomerErrorLogger.log("Error while trying to delete wifi configuration files.", e, activity);
        }
    }
}
