package dk.itu.kiosker.controllers;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.utils.SettingsExtractor;
import dk.itu.kiosker.utils.Time;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

class SoundController {
    private final Context context;
    private final ArrayList<Subscriber> subscribers;

    public SoundController(Context context, ArrayList<Subscriber> subscribers) {
        this.context = context;
        this.subscribers = subscribers;
    }

    void handleSoundSettings(LinkedHashMap settings) {
        boolean mute = SettingsExtractor.getBoolean(settings, "mute");
        if (mute) {
            setVolume(0);
            return;
        }
        int volume = SettingsExtractor.getInteger(settings, "volume");
        int standardVolume = volume > 0 ? volume : 50;
        setVolume(standardVolume);
        Double quietHoursStartTime = SettingsExtractor.getDouble(settings, "quietHoursStartTime");
        Double quietHoursStopTime = SettingsExtractor.getDouble(settings, "quietHoursStopTime");
        if (quietHoursStartTime >= 0 && quietHoursStopTime >= 0) {
            Time startTime = new Time(quietHoursStartTime);

            // Creating a simple observable we can define a task on.
            Observable<Integer> startObservable = Observable.from(1);

            // Create a subscriber that will set the volume to 0.
            Subscriber<Integer> quietHoursStartTimeSubscriber = getVolumeSubscriber(0);

            // Add the subscriber to our list subscribers.
            subscribers.add(quietHoursStartTimeSubscriber);

            // Start the task at the defined start time.
            startObservable
                    .delaySubscription(startTime.secondsUntil(), TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(quietHoursStartTimeSubscriber);

            // Repeat the above tasks for the stop time.
            Time stopTime = new Time(quietHoursStopTime);
            Observable<Integer> stopObservable = Observable.from(1);
            Subscriber<Integer> quietHoursStopTimeSubscriber = getVolumeSubscriber(standardVolume);
            subscribers.add(quietHoursStopTimeSubscriber);
            stopObservable
                    .delaySubscription(stopTime.secondsUntil(), TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(quietHoursStopTimeSubscriber);

            // If we are already in the quiet hours turn down the volume
            if (Time.isNowBetweenTheseTimes(startTime, stopTime)) {
                quietHoursStartTimeSubscriber.onNext(0);
            }
        }
    }

    private Subscriber<Integer> getVolumeSubscriber(final int volume) {
        return new Subscriber<Integer>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.e(Constants.TAG, "Error while setting volume.", e);
            }

            @Override
            public void onNext(Integer integer) {
                Log.d(Constants.TAG, String.format("Setting volume to %d%%.", volume));
                setVolume(volume);
            }
        };
    }

    private void setVolume(int volume) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                (int) (0.15 * volume),
                0);
    }
}
