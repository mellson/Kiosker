package dk.itu.kiosker.controllers;

import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.activities.SettingsActivity;
import dk.itu.kiosker.models.Constants;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class WebController {
    private MainActivity mainActivity;
    private Date lastTap;
    private int tapsToOpenSettings = 5;
    private int taps = tapsToOpenSettings;
    private ArrayList<WebView> webViews;
    private ArrayList<String> webPages;
    private ArrayList<Subscriber> subscribers;

    public WebController(MainActivity mainActivity, ArrayList<Subscriber> subscribers) {
        this.mainActivity = mainActivity;
        this.subscribers = subscribers;
        // Start our tap detection with a value
        lastTap = new Date();
        webViews = new ArrayList<>();
        webPages = new ArrayList<>();
    }

    public void handleWebSettings(LinkedHashMap settings) {
        ArrayList<String> tempWebPages = (ArrayList<String>) settings.get("home");
        if (tempWebPages == null)
            tempWebPages = new ArrayList<>();

        // Only pick the urls from our settings.
        for (int i = 0; i < tempWebPages.size(); i += 2)
            webPages.add(tempWebPages.get(i));

        int reloadPeriodMins = 0;
        Object reloadPeriodMinsObject = settings.get("reloadPeriodMins");
        if (reloadPeriodMinsObject != null)
            reloadPeriodMins = (int) reloadPeriodMinsObject;

        int errorReloadMins = 0;
        Object errorReloadMinsObject = settings.get("errorReloadMins");
        if (errorReloadMinsObject != null)
            errorReloadMins = (int) errorReloadMinsObject;

        setupWebViews(reloadPeriodMins, errorReloadMins);
    }

    /**
     * Setup the WebViews we need.
     */
    private void setupWebViews(final int reloadPeriodMins, final int errorReloadMins) {
        for (String page : webPages) {
            WebView webView = getWebView(1.00f / webPages.size());
            webViews.add(webView);
            mainActivity.addView(webView);
            webView.loadUrl(page);
            addTapToSettings(webView);
            webView.setWebViewClient(new WebViewClient() {
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
                public void onReceivedError(final WebView view, int errorCode, String description, String failingUrl) {
                    if (errorReloadMins > 0) {
                        Observable.timer(errorReloadMins, TimeUnit.MINUTES).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
                            @Override
                            public void call(Long aLong) {
                                Log.d(Constants.TAG, "Reloading after error");
                                view.reload();
                            }
                        });
                    }
                }
            });
            if (reloadPeriodMins > 0) {
                Subscriber s = reloadSubscriber();
                subscribers.add(s);
                Observable.timer(reloadPeriodMins, TimeUnit.MINUTES).repeat().observeOn(AndroidSchedulers.mainThread()).subscribe(s);
            }
        }
    }

    /**
     * Get subscriber for reloading the webview.
     */
    private Subscriber reloadSubscriber() {
        return new Subscriber() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                Log.e(Constants.TAG, "Error while trying to do scheduled reloading of webpage.", e);
            }

            @Override
            public void onNext(Object o) {
                reloadWebViews();
            }
        };
    }


    /**
     * Returns a WebView with a specified weight.
     *
     * @param weight how much screen estate should this main take?
     *               A total value of 1.0f will be split between the webviews created.
     * @return a WebView with the specified weight.
     */
    private WebView getWebView(float weight) {
        WebView webView = new WebView(mainActivity);
        webView.setWebChromeClient(new WebChromeClient());
        WebSettings webSettings = webView.getSettings();
        webView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0, weight));
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webView.setWebContentsDebuggingEnabled(true);
        return webView;
    }

    /**
     * Add our secret taps for opening settings to a WebView.
     */
    private void addTapToSettings(WebView webView) {
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            // Create a click listener that will open settings on the correct number of taps.
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mainActivity.stopScheduledTasks();

                    Date now = new Date();

                    // If we detect a double tap count down the taps needed to trigger settings.
                    if (now.getTime() - lastTap.getTime() < 300) {
                        if (taps == 2) {
                            taps = tapsToOpenSettings;
                            mainActivity.showingSettings = true;
                            Intent i = new Intent(mainActivity, SettingsActivity.class);
                            i.putExtra(Constants.KIOSKER_DEVICE_ID, Constants.getDeviceId(mainActivity));
                            i.putExtra(Constants.JSON_BASE_URL, Constants.getJsonBaseUrl(mainActivity));
                            mainActivity.startActivityForResult(i, 0);
                        }
                        taps--;
                    }
                    // If it is not a double tap reset the taps counter.
                    else
                        taps = tapsToOpenSettings;

                    lastTap = now;
                } else if (event.getAction() == MotionEvent.ACTION_UP)
                    mainActivity.startScheduledTasks();
                return false;
            }
        });
    }

    public void clearWebViews() {
        webViews.clear();
        webPages.clear();
    }

    public void reloadWebViews() {
        Observable.from(1).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                for (int i = 0; i < webViews.size(); i++)
                    webViews.get(i).loadUrl(webPages.get(i));
            }
        });
    }
}
