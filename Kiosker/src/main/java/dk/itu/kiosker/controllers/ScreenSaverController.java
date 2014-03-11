package dk.itu.kiosker.controllers;

import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.utils.SettingsExtractor;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class ScreenSaverController {
    private final MainActivity mainActivity;
    private final ArrayList<Subscriber> subscribers;
    private int screenSaveLengthMins;
    private ArrayList<String> screenSaverWebPages;
    private Observable<Long> screenSaverObservable;
    private Subscriber<Long> screenSaverSubscriber;
    private WebController webController;

    public ScreenSaverController(MainActivity mainActivity, ArrayList<Subscriber> subscribers, WebController webController) {
        this.mainActivity = mainActivity;
        this.subscribers = subscribers;
        this.webController = webController;
    }

    protected void handleScreenSaving(LinkedHashMap settings) {
        int screenSavePeriodMins = SettingsExtractor.getInteger(settings, "screenSavePeriodMins");
        if (screenSavePeriodMins > 0) {
            screenSaveLengthMins = SettingsExtractor.getInteger(settings, "screenSaveLengthMins");
            screenSaverWebPages = SettingsExtractor.getStringArrayList(settings, "screensavers");
            if (screenSaveLengthMins > 0) {
                screenSaverObservable = Observable.timer(screenSavePeriodMins, TimeUnit.MINUTES).observeOn(AndroidSchedulers.mainThread());
                screenSaverObservable.subscribe(getScreenSaverSubscriber());
            }
        }
    }

    public void startScreenSaverSubscription() {
        // Restart the idle time out if we are not in the standby period.
        if (screenSaverObservable != null && !mainActivity.currentlyInStandbyPeriod)
            screenSaverObservable.subscribe(getScreenSaverSubscriber());
    }

    public void stopScreenSaverSubscription() {
        screenSaverSubscriber.unsubscribe();
        if (mainActivity.currentlyScreenSaving) {
            mainActivity.currentlyScreenSaving = false;
            mainActivity.refreshDevice();
        }
    }

    Subscriber<Long> getScreenSaverSubscriber() {
        screenSaverSubscriber = new Subscriber<Long>() {
            @Override
            public void onCompleted() {
                Observable.timer(screenSaveLengthMins, TimeUnit.MINUTES)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Action1<Long>() {
                            @Override
                            public void call(Long aLong) {
                                Log.d(Constants.TAG, "Stopping screensaver.");
                                // Here we are finished screen saving and we return to the normal layout.
                                mainActivity.currentlyScreenSaving = false;

                                // Return to the previous brightness level
                                StandbyController.dimDevice(mainActivity);

                                mainActivity.refreshDevice();
                            }
                        });
            }

            @Override
            public void onError(Throwable e) {
                Log.e(Constants.TAG, "Error while screen saving.", e);
            }

            @Override
            public void onNext(Long l) {
                if (!screenSaverWebPages.isEmpty()) {
                    mainActivity.currentlyScreenSaving = true;

                    Random rnd = new Random();
                    int randomIndex = rnd.nextInt(screenSaverWebPages.size() / 2) * 2;

                    // Clean current view .
                    mainActivity.cleanUpMainView();

                    // Make a new full screen web view with a random url from the screen saver urls.
                    webController.setupWebView(false, screenSaverWebPages.get(randomIndex), 1.0f, false);

                    // Run the screen saver at max brightness
                    StandbyController.unDimDevice(mainActivity);

                    Log.d(Constants.TAG, String.format("Starting screensaver %s.", screenSaverWebPages.get(randomIndex + 1)));
                } else
                    unsubscribe();
            }
        };

        // Add to subscribers so we can cancel this later
        subscribers.add(screenSaverSubscriber);
        return screenSaverSubscriber;
    }
}
