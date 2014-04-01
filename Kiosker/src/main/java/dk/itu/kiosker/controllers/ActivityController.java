package dk.itu.kiosker.controllers;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.activities.SettingsActivity;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.utils.CustomerErrorLogger;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class ActivityController {
    /**
     * When the KioskerActivity goes into the background this method will decide if that change was ok.
     * If it was it does nothing.
     * If the change was unwanted we go back to this activity after a given period of time.
     */
    public static void handleMainActivityGoingAway(final KioskerActivity kioskerActivity) {
        if (!kioskerActivity.currentlyInStandbyPeriod || !showingAllowedActivity(kioskerActivity))
            getCountdownString().delay(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<String>() {
                @Override
                public void onCompleted() {
                    if (!showingAllowedActivity(kioskerActivity))
                        backToMainActivity(kioskerActivity);
                }

                @Override
                public void onError(Throwable e) {
                    CustomerErrorLogger.log("Error while trying to go create countdown toasts.", e, kioskerActivity);
                }

                @Override
                public void onNext(String msg) {
                    if (!showingAllowedActivity(kioskerActivity))
                        Toast.makeText(kioskerActivity, msg, Toast.LENGTH_SHORT).show();
                    else
                        unsubscribe();
                }
            });
    }

    /**
     * Returns a series of strings indicating when the application will return to the KioskerActivity.
     */
    private static Observable<String> getCountdownString() {
        final int totalCountdown = 30;
        final int secondsBetweenStrings = 10;

        return Observable.create(new Observable.OnSubscribe<String>() {
            int timeLeft = totalCountdown;

            @Override
            public void call(Subscriber<? super String> subscriber) {
                if (subscriber.isUnsubscribed()) {
                    return;
                }
                timeLeft -= secondsBetweenStrings;
                if (timeLeft > 0)
                    subscriber.onNext("Going back in " + timeLeft + " seconds.");
                else
                    subscriber.onNext("Going back.");
                subscriber.onCompleted();
            }
        }).delay(secondsBetweenStrings, TimeUnit.SECONDS)
                .repeat(totalCountdown / secondsBetweenStrings)
                .startWith("You have " + totalCountdown + " seconds before we go back again.")
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Returns a boolean indicating if an activity is on screen and we actually want it there.
     *
     * @return true if we are showing an activity we want.
     */
    private static Boolean showingAllowedActivity(KioskerActivity kioskerActivity) {
        ActivityManager am = (ActivityManager) kioskerActivity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        Boolean showingMainActivity = taskInfo.get(0).topActivity.getClassName().contains(KioskerActivity.class.getName());
        Boolean showingSettingsActivity = taskInfo.get(0).topActivity.getClassName().contains(SettingsActivity.class.getName());
        return showingMainActivity || showingSettingsActivity || Constants.getAllowHome(kioskerActivity);
    }

    /**
     * Return to this activity by telling the ActivityManager to switch to this task.
     */
    public static void backToMainActivity(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(100);
        assert taskInfo != null;
        for (ActivityManager.RunningTaskInfo taskInfo1 : taskInfo) {
            assert taskInfo1.topActivity != null;
            String className = taskInfo1.topActivity.getClassName();
            if (className != null && !className.isEmpty()) {
                Log.d(Constants.TAG, taskInfo1.topActivity.getClassName());
                if (className.contains(KioskerActivity.class.getName()))
                    am.moveTaskToFront(taskInfo1.id, ActivityManager.MOVE_TASK_NO_USER_ACTION);
            }
        }
    }
}
