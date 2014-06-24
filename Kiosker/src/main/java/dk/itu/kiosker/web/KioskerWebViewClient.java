package dk.itu.kiosker.web;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;
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
    private boolean errorReloaderStarted;
    private boolean firstPageLoad;
    private SensorManager sensorManager;
    private WebView view;

    public KioskerWebViewClient(LinkedHashMap settings, KioskerActivity kioskerActivity, WebPage webPage) {
        this.errorReloadMins = SettingsExtractor.getInteger(settings, "errorReloadMins");
        this.kioskerActivity = kioskerActivity;
        this.webPage = webPage;
        this.lightSensorReceivers = SettingsExtractor.getStringArrayList(settings, "lightSensorReceivers");
        this.lightSensorDelaySpeed = SettingsExtractor.getInteger(settings, "lightSensorDelay");
        this.sensorManager = (SensorManager) kioskerActivity.getSystemService(Context.SENSOR_SERVICE); // 0 - fastest, 1, 2, 3 - slowest (normal)
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
    public void onPageFinished(WebView view, String url) {
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
            view.loadUrl("javascript:changeBackground()");
        }
    }

    public void stopSensors() {
        // If this is a light sensor receiver, then unregister it before a page load
        if (lightSensorReceivers.contains(webPage.title))
            sensorManager.unregisterListener(this);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
