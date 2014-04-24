package dk.itu.kiosker.utils;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import java.util.LinkedHashMap;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;
import rx.Subscriber;

public class WifiController {
    private final KioskerActivity kioskerActivity;
    private Subscriber<Long> wifiConnectSubscriber;

    public WifiController(KioskerActivity kioskerActivity) {
        this.kioskerActivity = kioskerActivity;
    }

    public void handleWifiSettings(LinkedHashMap settings) {
//        TODO Check when this bug gets fixed.
//        Currently disabled because of bug in KitKat that will soft kill the wifi radio when programmatically turning on wifi.
//
//        if (SettingsExtractor.getBoolean(settings, "manualWifi")) {
//            String SSID = SettingsExtractor.getString(settings, "wifiSSID");
//            Constants.setSSID(kioskerActivity, SSID);
//            if (!SSID.isEmpty())
//                Observable.timer(5, TimeUnit.MINUTES).repeat().subscribe(getWifiConnectSubscriber(SSID));
//        }
    }

    private Subscriber<? super Long> getWifiConnectSubscriber(final String SSID) {
        if (wifiConnectSubscriber != null && !wifiConnectSubscriber.isUnsubscribed())
            wifiConnectSubscriber.unsubscribe();

        wifiConnectSubscriber = new Subscriber<Long>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                CustomerErrorLogger.log("Error while trying to connect to " + SSID, e, kioskerActivity);
            }

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
        wifiManager.setWifiEnabled(true);

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "\"" + ssid + "\"";
        config.preSharedKey = "\""+ WifiCredentials.wifiKeys.get(ssid) +"\"";

        int netId = wifiManager.addNetwork(config);
        wifiManager.saveConfiguration();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
    }
}
