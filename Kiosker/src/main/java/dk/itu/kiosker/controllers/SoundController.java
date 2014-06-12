package dk.itu.kiosker.controllers;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.utils.KioskerSubscriber;
import dk.itu.kiosker.utils.SettingsExtractor;
import dk.itu.kiosker.utils.Time;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

class SoundController {
    private final Context context;
    private final ArrayList<Subscriber> subscribers;
    private Subscriber<Long> quietHoursStartTimeSubscriber;
    private Subscriber<Long> quietHoursStopTimeSubscriber;
    private Activity kioskerActivity;

    public SoundController(Context context, ArrayList<Subscriber> subscribers, KioskerActivity kioskerActivity) {
        this.context = context;
        this.subscribers = subscribers;
        this.kioskerActivity = kioskerActivity;
    }

    void handleSoundSettings(LinkedHashMap settings) {
        boolean mute = SettingsExtractor.getBoolean(settings, "mute");
        if (mute) {
            setVolume(0);
            return;
        }
        int volume = SettingsExtractor.getInteger(settings, "volume");
        final int standardVolume = volume > 0 ? volume : 50;
        setVolume(standardVolume);
        String quietHoursStartTime = SettingsExtractor.getString(settings, "quietHoursStartTime");
        String quietHoursStopTime = SettingsExtractor.getString(settings, "quietHoursStopTime");
        if (!quietHoursStartTime.isEmpty() && !quietHoursStopTime.isEmpty()) {
            Time startTime = new Time(quietHoursStartTime);

            // Creating a simple observable we can define a task on.
            Observable<Long> startObservable = Observable.from(1L);

            if (quietHoursStartTimeSubscriber != null) {
                quietHoursStartTimeSubscriber.unsubscribe();
                subscribers.remove(quietHoursStartTimeSubscriber);
            }

            // Create a subscriber that will set the volume to 0.
            quietHoursStartTimeSubscriber = new KioskerSubscriber("Error while setting volume.", kioskerActivity) {
                @Override
                public void onNext(Long aLong) {
                    Log.d(Constants.TAG, String.format("Setting volume to %d%%.", 0));
                    setVolume(0);
                }
            };

            // Add the subscriber to our list subscribers.
            subscribers.add(quietHoursStartTimeSubscriber);

            // Start the task at the defined start time.
            startObservable
                    .delaySubscription(startTime.secondsUntil(), TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(quietHoursStartTimeSubscriber);

            // Repeat the above tasks for the stop time.
            Time stopTime = new Time(quietHoursStopTime);
            Observable<Long> stopObservable = Observable.from(1L);
            if (quietHoursStopTimeSubscriber != null) {
                quietHoursStopTimeSubscriber.unsubscribe();
                subscribers.remove(quietHoursStopTimeSubscriber);
            }
            quietHoursStopTimeSubscriber = new KioskerSubscriber("Error while setting volume.", kioskerActivity) {
                @Override
                public void onNext(Long aLong) {
                    Log.d(Constants.TAG, String.format("Setting volume to %d%%.", standardVolume));
                    setVolume(0);
                }
            };
            subscribers.add(quietHoursStopTimeSubscriber);
            stopObservable
                    .delaySubscription(stopTime.secondsUntil(), TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(quietHoursStopTimeSubscriber);

            // If we are already in the quiet hours turn down the volume
            if (Time.isNowBetweenTheseTimes(startTime, stopTime)) {
                quietHoursStartTimeSubscriber.onNext(0L);
            }
        }
    }

    private void setVolume(int volume) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                (int) (0.15 * volume),
                0);
    }
}