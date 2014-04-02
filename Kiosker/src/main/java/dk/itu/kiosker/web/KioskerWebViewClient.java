package dk.itu.kiosker.web;

import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class KioskerWebViewClient extends WebViewClient {
    private final long errorReloadMins;
    private final KioskerActivity kioskerActivity;
    private boolean errorReloaderStarted;
    private boolean firstPageLoad;

    public KioskerWebViewClient(long errorReloadMins, KioskerActivity kioskerActivity) {
        this.errorReloadMins = errorReloadMins;
        this.kioskerActivity = kioskerActivity;
    }

    // you tell the webclient you want to catch when a url is about to load
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return false;
    }

    // here you execute an action when the URL you want is about to load
    @Override
    public void onLoadResource(WebView view, String url) {
        if (url.startsWith("http")) {
        } else {
            Log.d(Constants.TAG, "URL ERROR" + url);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (!firstPageLoad) {
            Constants.setHomeUrl(kioskerActivity, url);
            firstPageLoad = true;
        }
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
                    }
                    else if (Constants.isNetworkAvailable(kioskerActivity)) {
                        Log.d(Constants.TAG, "Reloading after web error: " + description);
                        view.reload();
                    }
                }
            });
        }
    }
}
