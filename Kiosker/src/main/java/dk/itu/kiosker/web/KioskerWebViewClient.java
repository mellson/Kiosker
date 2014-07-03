package dk.itu.kiosker.web;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.utils.KioskerSubscriber;
import dk.itu.kiosker.utils.SettingsExtractor;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class KioskerWebViewClient extends WebViewClient implements SensorEventListener {
    private final long errorReloadMins;
    private final KioskerActivity kioskerActivity;
    private final int lightSensorDelaySpeed;
    private final WebPage webPage;
    private final ArrayList<String> lightSensorReceivers;
    private KioskerSubscriber bluetoothUpdateSubscriber;
    private int bluetoothDiscoveryInterval; // Number of seconds between each bluetooth discovery
    private BluetoothAdapter mBluetoothAdapter;
    private BroadcastReceiver bluetoothUpdateReceiver;
    private final ArrayList<String> bluetoothSensorReceivers;
    private ArrayList<String> bluetoothDevices = new ArrayList<>();
    private boolean errorReloaderStarted;
    private boolean firstPageLoad;
    private String deviceId = "";
    private SensorManager sensorManager;
    private WebView view;

    public KioskerWebViewClient(LinkedHashMap settings, KioskerActivity kioskerActivity, WebPage webPage) {
        this.errorReloadMins = SettingsExtractor.getInteger(settings, "errorReloadMins");
        this.kioskerActivity = kioskerActivity;
        this.webPage = webPage;
        this.lightSensorReceivers = SettingsExtractor.getStringArrayList(settings, "lightSensorReceivers");
        this.lightSensorDelaySpeed = SettingsExtractor.getInteger(settings, "lightSensorDelay");
        this.bluetoothSensorReceivers = SettingsExtractor.getStringArrayList(settings, "bluetoothSensorReceivers");
        this.bluetoothDiscoveryInterval = SettingsExtractor.getInteger(settings, "bluetoothDiscoveryInterval");
        if (bluetoothDiscoveryInterval == -1)
            bluetoothDiscoveryInterval = 60;
        this.sensorManager = (SensorManager) kioskerActivity.getSystemService(Context.SENSOR_SERVICE); // 0 - fastest, 1, 2, 3 - slowest (normal)
        this.deviceId = "\"" + Constants.getString(kioskerActivity, Constants.KIOSKER_DEVICE_ID) + "\"";
        if (bluetoothSensorReceivers.contains(webPage.title))
            this.bluetoothUpdateReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    // When discovery finds a device
                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        // Get the BluetoothDevice object from the Intent
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        // Add the name and address to an array adapter to show in a ListView
                        String deviceInfo = device.getName() + " - " + device.getAddress();
                        if (!bluetoothDevices.contains(deviceInfo))
                            bluetoothDevices.add(deviceInfo);
                        view.loadUrl("javascript:bluetoothSensorUpdate(" + bluetoothJSArray() + "," + deviceId + ")");
                        if (!mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.startDiscovery();
                    }
                }
            };
    }

    // Convert the bluetooth devices arraylist to a format that javascript can use
    private String bluetoothJSArray() {
        String s = "[";
        for (String bluetoothDevice : bluetoothDevices) {
            s += "\"" + bluetoothDevice + "\",";
        }
        s = s.substring(0, s.length() - 1) + "]";
        return s;
    }

    // you tell the web client you want to catch when a url is about to load
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return false;
    }

    // here you execute an action when the URL you want is about to load
    @Override
    public void onLoadResource(WebView view, String url) {
        if (!url.startsWith("http")) {
            Log.d(Constants.TAG, "URL ERROR" + url);
        }
    }

    @Override
    public void onPageFinished(final WebView view, String url) {
        super.onPageFinished(view, url);
        if (!firstPageLoad) {
            Constants.setString(kioskerActivity, url, Constants.KIOSKER_HOME_URL_ID);
            firstPageLoad = true;
        }
        // Only add the light sensor if this web page is one of the receivers.
        if (lightSensorReceivers.contains(webPage.title)) {
            int sensorDelay;
            switch (lightSensorDelaySpeed) {
                case 0:
                    sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
                    break;
                case 1:
                    sensorDelay = SensorManager.SENSOR_DELAY_GAME;
                    break;
                case 2:
                    sensorDelay = SensorManager.SENSOR_DELAY_UI;
                    break;
                case 3:
                    sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
                    break;
                default:
                    sensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
            }
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
                    sensorDelay);

        }

        if (bluetoothSensorReceivers.contains(webPage.title)) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                Log.d(Constants.TAG, "No Bluetooth Radio");
            } else {
                if (!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                kioskerActivity.registerReceiver(bluetoothUpdateReceiver, filter);
                if (bluetoothUpdateSubscriber != null && !bluetoothUpdateSubscriber.isUnsubscribed())
                    bluetoothUpdateSubscriber.unsubscribe();
                bluetoothUpdateSubscriber = new KioskerSubscriber("Error while discovering bluetooth devices.", kioskerActivity) {
                    @Override
                    public void onNext(Long aLong) {
                        if (!mBluetoothAdapter.isDiscovering()) {
                            mBluetoothAdapter.startDiscovery();
                            bluetoothDevices.clear();
                        }
                    }
                };
                Observable.interval(bluetoothDiscoveryInterval, TimeUnit.SECONDS).subscribe(bluetoothUpdateSubscriber);
            }
        }
        this.view = view;
    }

    @Override
    public void onReceivedError(final WebView view, int errorCode, final String description, String failingUrl) {
        final int webError = errorCode;
        if (errorReloadMins > 0 && !errorReloaderStarted) {
            errorReloaderStarted = true;
            Observable.timer(errorReloadMins, TimeUnit.MINUTES).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
                @Override
                public void call(Long aLong) {
                    errorReloaderStarted = false;
                    if (webError == ERROR_HOST_LOOKUP) {
                        Log.d(Constants.TAG, "Refreshing after error web error: " + description);
                        kioskerActivity.refreshDevice();
                    } else if (Constants.isNetworkAvailable(kioskerActivity)) {
                        Log.d(Constants.TAG, "Reloading after web error: " + description);
                        view.reload();
                    }
                }
            });
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0]; // SI lux units (m/s^2), range 0.0 -> 32768.0
            view.loadUrl("javascript:lightSensorUpdate(" + lux + "," + deviceId + ")");
        }
    }

    public void stopSensors() {
        // If this is a light sensor receiver, then unregister it before a page load
        if (lightSensorReceivers != null && lightSensorReceivers.contains(webPage.title) && sensorManager != null)
            sensorManager.unregisterListener(this);
        if (bluetoothSensorReceivers.contains(webPage.title))
            kioskerActivity.unregisterReceiver(bluetoothUpdateReceiver);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}