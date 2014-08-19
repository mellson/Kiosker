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

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.utils.SettingsExtractor;
import dk.itu.kiosker.utils.Tuple;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class KioskerWebViewClient extends WebViewClient implements SensorEventListener {
    private final long errorReloadMins;
    private final KioskerActivity kioskerActivity;
    private final boolean addSensorBridge;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean errorReloaderStarted;
    private boolean firstPageLoad;
    private String deviceId = "";
    private int lightSensorDelaySpeed = 3;
    private SensorManager sensorManager;
    private JSSensorBridge jsSensorBridge;
    private WebView view;

    public KioskerWebViewClient(LinkedHashMap settings, KioskerActivity kioskerActivity) {
        this.errorReloadMins = SettingsExtractor.getInteger(settings, "errorReloadMins");
        this.kioskerActivity = kioskerActivity;
        this.addSensorBridge = SettingsExtractor.getBoolean(settings, "addSensorBridge");
        this.sensorManager = (SensorManager) kioskerActivity.getSystemService(Context.SENSOR_SERVICE);
        this.deviceId = "\"" + Constants.getString(kioskerActivity, Constants.KIOSKER_DEVICE_ID) + "\"";

        // Init the bridge object if we need it
        if (addSensorBridge) jsSensorBridge = new JSSensorBridge(this);
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
            if (addSensorBridge) {
                // Injecting the Java object to JavaScript
                view.addJavascriptInterface(jsSensorBridge, "sensorBridge");
                view.reload();
                this.view = view;
            }
        }
    }

    public void startBluetoothDiscovery() {
        BroadcastReceiver bluetoothUpdateReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    Tuple<String, String> deviceInfo = new Tuple<>(device.getName(), device.getAddress());
                    view.loadUrl("javascript:bluetoothSensorUpdate(" + deviceInfo + ")"); // TODO færdiggør den her
                }
            }
        };
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) Log.d(Constants.TAG, "No Bluetooth Radio");
        else {
            if (!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            kioskerActivity.registerReceiver(bluetoothUpdateReceiver, filter);
            if (!mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.startDiscovery();
                jsSensorBridge.bluetoothDevices.clear();
            }
        }
    }

    public Set<BluetoothDevice> getPairedDevices() {
        if (mBluetoothAdapter == null) Log.d(Constants.TAG, "No Bluetooth Radio");
        else if (!mBluetoothAdapter.isEnabled()) mBluetoothAdapter.enable();
        return mBluetoothAdapter.getBondedDevices();
    }

    public void setLightSensorDelaySpeed(int lightSensorDelaySpeed) {
        this.lightSensorDelaySpeed = lightSensorDelaySpeed;
    }

    public void startLightSensor() {
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

    @Override // This is the method called whenever the light sensor gets an update from the sensor.
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            float lux = event.values[0]; // SI lux units (m/s^2), range 0.0 -> 32768.0
            view.loadUrl("javascript:lightSensorUpdate(" + lux + "," + deviceId + ")");
        }
    }

    public void stopLightSensor() {
        if (addSensorBridge) sensorManager.unregisterListener(this);
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
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}