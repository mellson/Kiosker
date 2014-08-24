package dk.itu.kiosker.web;

import android.webkit.JavascriptInterface;

import org.json.JSONArray;

import java.lang.reflect.Array;
import java.util.ArrayList;

import dk.itu.kiosker.utils.BluetoothDeviceTuple;

/**
 * This class is encapsulating the bridge between our applications sensors and javascript.
 */
public class JSSensorBridge {
    public ArrayList<BluetoothDeviceTuple> bluetoothDevices = new ArrayList<>();
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
    public String getPairedDevices() {
        ArrayList<String> res = new ArrayList<>();
        for (BluetoothDeviceTuple bluetoothDevice : kioskerWebViewClient.getPairedDevices())
            res.add(bluetoothDevice.toString());
        return new JSONArray(res).toString();
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