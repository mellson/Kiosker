package dk.itu.kiosker.models;

import android.util.Log;
import android.widget.Toast;

import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.InitialSetup;
import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.utils.JsonFetcher;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class OnlineSettings {
    private static LinkedHashMap currentSettings;

    public static void getSettings(MainActivity mainActivity) {
        Constants.JSON_BASE_URL = Constants.getJsonBaseUrl(mainActivity);

        if (!Constants.JSON_BASE_URL.isEmpty()) {
            JsonFetcher.getObservableMap(Constants.BASE_SETTINGS + Constants.FILE_ENDING)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(baseSettingsObserver(mainActivity));
        } else {
            LinkedHashMap emptyMap = new LinkedHashMap();
            Observable.from(emptyMap)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(baseSettingsObserver(mainActivity));
        }
    }

    // Observer we use to consume the base json settings.
    private static Observer<LinkedHashMap> baseSettingsObserver(final MainActivity mainActivity) {
        return new Observer<LinkedHashMap>() {
            @Override
            public void onCompleted() {
                Log.d(Constants.TAG, "Finished getting base json settings.");
                mainActivity.updateSubStatus("Finished downloading base settings.");
                String device_id = Constants.getDeviceId(mainActivity);
                if (!device_id.isEmpty())
                    JsonFetcher.getObservableMap(Constants.BASE_SETTINGS + "_" + device_id + Constants.FILE_ENDING)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(deviceSpecificSettingsObserver(mainActivity));
                else
                    mainActivity.handleSettings(currentSettings);
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(Constants.TAG, "Error while getting base json settings.", throwable);

                mainActivity.updateMainStatus("Error");
                if (Constants.hasSafeSettings(mainActivity)) {
                    mainActivity.updateSubStatus("while getting base json settings, trying safe settings.");

                    // Of there was an error getting the json we can load or last successful json
                    Observable.timer(3, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
                        @Override
                        public void call(Long aLong) {
                            mainActivity.loadSafeSettings();
                        }
                    });
                } else {
                    mainActivity.updateSubStatus("while getting base json settings, please retry.");
                    Observable.timer(3, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
                        @Override
                        public void call(Long aLong) {
                            InitialSetup.start(mainActivity);
                        }
                    });
                }
            }

            @Override
            public void onNext(LinkedHashMap settings) {
                // Set the current settings to be the downloaded base settings.
                currentSettings = settings;
            }
        };
    }

    // Observer we use to consume the device specific json settings.
    private static Observer<LinkedHashMap> deviceSpecificSettingsObserver(final MainActivity mainActivity) {
        return new Observer<LinkedHashMap>() {
            @Override
            public void onCompleted() {
                Log.d(Constants.TAG, "Finished getting device specific json settings.");
                mainActivity.updateSubStatus("Finished downloading device specific settings.");
                mainActivity.handleSettings(currentSettings);
            }

            @Override
            public void onError(Throwable throwable) {
                Log.e(Constants.TAG, "Error while getting device specific json settings.", throwable);
                Toast.makeText(mainActivity, "Error while getting device specific json settings.", Toast.LENGTH_LONG).show();
                mainActivity.handleSettings(currentSettings);
            }

            @Override
            public void onNext(LinkedHashMap settings) {
                // Combine the base settings with the user specific settings.
                currentSettings.putAll(settings);
            }
        };
    }
}
