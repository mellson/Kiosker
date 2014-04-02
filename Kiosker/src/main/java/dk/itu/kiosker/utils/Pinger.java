package dk.itu.kiosker.utils;

import android.content.pm.PackageManager;
import android.util.Log;

import com.github.kevinsawicki.http.HttpRequest;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;
import rx.Observable;
import rx.Subscriber;

public class Pinger {
    private static Subscriber<Long> pingSubscriber;

    public static void start(KioskerActivity kioskerActivity) {
        Observable.timer(10, TimeUnit.SECONDS).repeat().subscribe(getPingSubscriber(kioskerActivity));
    }

    private static Subscriber<? super Long> getPingSubscriber(final KioskerActivity kioskerActivity) {
        if (pingSubscriber != null && !pingSubscriber.isUnsubscribed())
            pingSubscriber.unsubscribe();
        pingSubscriber = new Subscriber<Long>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                CustomerErrorLogger.log("Error while trying to ping.", e, kioskerActivity);
            }

            @Override
            public void onNext(Long aLong) {
                Random random = new Random();
                String randomInt = "";
                for (int i = 0; i < 5; i++)
                    randomInt += random.nextInt(9);
                int version = 0;
                try {
                     version = kioskerActivity.getPackageManager().getPackageInfo(kioskerActivity.getPackageName(), 0).versionCode;
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                String deviceId = Constants.getDeviceId(kioskerActivity);
                deviceId = deviceId.isEmpty() ? "UnknownDevice" : deviceId;
                String url = "http://clintio.us/kiosker/ping.php?d=" + deviceId + "&v=" + version +  "&r=" + randomInt;
                HttpRequest request =  HttpRequest.get(url);
                if (!request.ok())
                    Log.d(Constants.TAG, "Could not ping " + url);
            }
        };
        return pingSubscriber;
    }
}
