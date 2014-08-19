package dk.itu.kiosker.web;

import android.bluetooth.BluetoothDevice;
import android.webkit.JavascriptInterface;

import java.util.ArrayList;
import java.util.Set;

import dk.itu.kiosker.utils.Tuple;

/**
 * This class is encapsulating the bridge between our applications sensors and javascript.
 */
public class JSSensorBridge {
    public ArrayList<Tuple<String, String>> bluetoothDevices = new ArrayList<>();
    public KioskerWebViewClient kioskerWebViewClient;

    public JSSensorBridge(KioskerWebViewClient kioskerWebViewClient) {
        this.kioskerWebViewClient = kioskerWebViewClient;
    }

    @JavascriptInterface
    public String toString() {
        return "JSSensorBridge Tester";
    }

    @JavascriptInterface
    public void setLightSensorDelaySpeed(int speed) {
        kioskerWebViewClient.setLightSensorDelaySpeed(speed);
    }

    @JavascriptInterface
    public Set<BluetoothDevice> getPairedDevices() {
        return kioskerWebViewClient.getPairedDevices();
    }

    @JavascriptInterface
    public void startBluetoothDiscovery() {
        kioskerWebViewClient.startBluetoothDiscovery();
    }

    @JavascriptInterface
    public void startLightSensor() {
        kioskerWebViewClient.startLightSensor();
    }

    @JavascriptInterface
    public void stopLightSensor() {
        kioskerWebViewClient.stopLightSensor();
    }
}