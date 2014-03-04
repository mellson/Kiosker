package dk.itu.kiosker.controllers;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.models.Constants;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class ActivityController {
    /**
     * When the MainActivity goes into the background this method will decide if that change was ok.
     * If it was it does nothing.
     * If the change was unwanted we go back to this activity after a given period of time.
     */
    public static void handleMainActivityGoingAway(final MainActivity context) {
        if (!showingAllowedActivity(context))
            getCountdownString().subscribe(new Subscriber<String>() {
                @Override
                public void onCompleted() {
                    if (!showingAllowedActivity(context))
                        backToMainActivity(context);
                }

                @Override
                public void onError(Throwable e) {
                    Log.e(Constants.TAG, "Error while trying to go create countdown toasts.", e);
                }

                @Override
                public void onNext(String msg) {
                    if (!showingAllowedActivity(context))
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    else
                        unsubscribe();
                }
            });
    }

    /**
     * Returns a series of strings indicating when the application will return to the MainActivity.
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
    public static Boolean showingAllowedActivity(MainActivity mainActivity) {
        ActivityManager am = (ActivityManager) mainActivity.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        Boolean showingMainActivity = taskInfo.get(0).topActivity.getClassName().contains(MainActivity.class.getName());
        return showingMainActivity || mainActivity.showingSettings || Constants.getAllowHome(mainActivity);
    }

    /**
     * Return to this activity by telling the ActivityManager to switch to this task.
     */
    public static void backToMainActivity(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(100);
        for (int i = 0; i < taskInfo.size(); i++) {
            ActivityManager.RunningTaskInfo taskInfo1 = taskInfo.get(i);
            Log.d(Constants.TAG, taskInfo1.topActivity.getClassName());
            if (taskInfo1.topActivity.getClassName().contains(MainActivity.class.getName()))
                am.moveTaskToFront(taskInfo1.id, ActivityManager.MOVE_TASK_NO_USER_ACTION);
        }
    }
}
