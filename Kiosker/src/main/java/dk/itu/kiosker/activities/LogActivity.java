package dk.itu.kiosker.activities;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.R;
import dk.itu.kiosker.controllers.HardwareController;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.utils.CustomerErrorLogger;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class LogActivity extends Activity {
    private TextView tv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        keepScreenOn();
    }

    protected void keepScreenOn() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        getWindow().setAttributes(params);
    }

    private void updateLog() {
        try {
            Process process = Runtime.getRuntime().exec("logcat -d -v time");
            final BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            tv = (TextView) findViewById(R.id.logTextView);
            tv.setText("Getting log, please be patient.");

            final Subscriber<Long> updateSubscriber = new Subscriber<Long>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    CustomerErrorLogger.log("Error while updating log update text.", e, LogActivity.this);
                }

                @Override
                public void onNext(Long aLong) {
                    tv.setText(tv.getText() + ".");
                }
            };
            Observable.interval(100, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(updateSubscriber);

            Observable.timer(500, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Long>() {
                        @Override
                        public void call(Long aLong) {
                            StringBuilder log = new StringBuilder();
                            String line;
                            try {
                                while ((line = bufferedReader.readLine()) != null) {
                                    if (!line.contains("GC_FOR_ALLOC")) { // Remove all garbage collection messages.
                                        log.insert(0, line);
                                        log.insert(line.length(), "\n");
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            updateSubscriber.unsubscribe();
                            String latestException = Constants.getLatestError(LogActivity.this);
                            if (!latestException.isEmpty()) {
                                TextView tv2 = (TextView) findViewById(R.id.exceptionsTextView);
                                tv2.setTextColor(Color.RED);
                                tv2.setText(latestException);
                            }
                            tv.setText(log);
                        }
                    });
        } catch (IOException e) {
            Log.e(Constants.TAG, "Error while updating log.", e);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        updateLog();
        HardwareController.showNavigationUI();
//        FlurryAgent.setUserId(Constants.getDeviceId(this));
//        FlurryAgent.onStartSession(this, FlurryCredentials.API_KEY);
//        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        HardwareController.handleNavigationUI();
//        FlurryAgent.onEndSession(this);
//        EasyTracker.getInstance(this).activityStop(this);
    }
}