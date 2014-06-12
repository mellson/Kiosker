package dk.itu.kiosker.utils;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;
import rx.Observable;
import rx.Subscriber;

public class WifiController {
    private final KioskerActivity kioskerActivity;
    private Subscriber<Long> wifiConnectSubscriber;

    public WifiController(KioskerActivity kioskerActivity) {
        this.kioskerActivity = kioskerActivity;
    }

    public void handleWifiSettings(LinkedHashMap settings) {
        if (SettingsExtractor.getBoolean(settings, "manualWifi")) {
            Constants.setBoolean(kioskerActivity, true, Constants.KIOSKER_MANUAL_WIFI);
            String SSID = SettingsExtractor.getString(settings, "wifiSSID");
            Constants.setString(kioskerActivity, SSID, Constants.KIOSKER_SSID_ID);
            if (!SSID.isEmpty())
                Observable.timer(5, TimeUnit.MINUTES).repeat().subscribe(getWifiConnectSubscriber(SSID));
        }
    }

    private Subscriber<? super Long> getWifiConnectSubscriber(final String SSID) {
        if (wifiConnectSubscriber != null && !wifiConnectSubscriber.isUnsubscribed())
            wifiConnectSubscriber.unsubscribe();

        wifiConnectSubscriber = new KioskerSubscriber("Error while trying to connect to " + SSID, kioskerActivity) {
            @Override
            public void onNext(Long aLong) {
                if (!Constants.isNetworkAvailable(kioskerActivity))
                    connectToWifi(SSID);
            }
        };
        return wifiConnectSubscriber;
    }

    public void connectToWifi(String ssid) {
        WifiManager wifiManager = (WifiManager) kioskerActivity.getSystemService(Context.WIFI_SERVICE);
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        while (configuredNetworks == null) {
            wifiManager.setWifiEnabled(false);
            wifiManager.setWifiEnabled(true);
            configuredNetworks = wifiManager.getConfiguredNetworks();
        }

        for (WifiConfiguration network : configuredNetworks)
            if (network.SSID.replace("\"","").equals(ssid))
                wifiManager.removeNetwork(network.networkId);

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ssid + "\"";
        config.preSharedKey = "\""+ WifiCredentials.wifiKeys.get(ssid) +"\"";
        int netId = wifiManager.addNetwork(config);
        wifiManager.saveConfiguration();
        wifiManager.enableNetwork(netId, true);
    }

    /**
     * Gets the state of Airplane Mode.
     *
     * @param context
     * @return true if enabled.
     */
    private static boolean isAirplaneModeOn(Context context) {

        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;

    }

    public static void sendMacAddressToCrashlytics(Activity activity) {
        WifiManager wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        String macAdd = wifiManager.getConnectionInfo().getMacAddress();
        String device_id = Constants.getString(activity, Constants.KIOSKER_DEVICE_ID);
        throw new RuntimeException("Device:" + device_id + " - MAC:" + macAdd);
    }
}
