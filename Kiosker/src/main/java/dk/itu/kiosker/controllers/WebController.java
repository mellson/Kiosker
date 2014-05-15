package dk.itu.kiosker.controllers;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.activities.SettingsActivity;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.utils.CustomerErrorLogger;
import dk.itu.kiosker.utils.IntentHelper;
import dk.itu.kiosker.utils.SettingsExtractor;
import dk.itu.kiosker.utils.WebHelper;
import dk.itu.kiosker.web.KioskerWebChromeClient;
import dk.itu.kiosker.web.KioskerWebViewClient;
import dk.itu.kiosker.web.NavigationLayout;
import dk.itu.kiosker.web.WebPage;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class WebController {
    public static final int tapsToOpenSettings = 5;
    private int taps = tapsToOpenSettings;
    private final KioskerActivity kioskerActivity;
    private final ArrayList<Subscriber> subscribers;
    // Has our main web view (home) at index 0 and the sites at index 1
    private ArrayList<WebView> webViews;
    private ArrayList<NavigationLayout> navigationLayouts;
    private Date lastTap;
    private ArrayList<WebPage> homeWebPages;
    private ArrayList<WebPage> sitesWebPages;
    private int reloadPeriodMins;
    private int errorReloadMins;
    private boolean fullScreenMode;
    private ScreenSaverController screenSaverController;
    private Subscriber<Long> secondaryCycleSubscriber;
    private int secondaryCycleIndex;
    private Observable<Long> secondaryCycleObservable;
    private Subscriber<Long> reloadSubscriber;
    private int resetToHomeMins;
    private int defaultResetToHomeMins = 2;
    private Subscriber<Long> resetToHomeSubscriber;

    public WebController(KioskerActivity kioskerActivity, ArrayList<Subscriber> subscribers) {
        this.kioskerActivity = kioskerActivity;
        this.subscribers = subscribers;
        lastTap = new Date();
        webViews = new ArrayList<>();
        navigationLayouts = new ArrayList<>();
    }

    public void handleWebSettings(LinkedHashMap settings) {
        kioskerActivity.cleanUpMainView(); // Make sure that we don't have multiple webviews

        reloadPeriodMins = SettingsExtractor.getInteger(settings, "reloadPeriodMins");
        errorReloadMins = SettingsExtractor.getInteger(settings, "errorReloadMins");

        resetToHomeMins = SettingsExtractor.getInteger(settings, "resetToHomeMins");
        resetToHomeMins = resetToHomeMins <= 0 ? defaultResetToHomeMins : resetToHomeMins;
        startResetToHomeSubscription();

        // Get the layout from settings, if there is no layout defined fallback to 0 - fullscreen layout.
        int tempLayout = SettingsExtractor.getInteger(settings, "layout");
        int layout = (tempLayout >= 0) ? tempLayout : 0;
        fullScreenMode = layout == 0;

        homeWebPages = SettingsExtractor.getWebPages(settings, "home");
        sitesWebPages = SettingsExtractor.getWebPages(settings, "sites");

        handleWebViewSetup(layout);
        handleAutoCycleSecondary(settings);

        screenSaverController = new ScreenSaverController(kioskerActivity, subscribers, this);
        screenSaverController.handleScreenSaving(settings);

        boolean clearCache = Constants.getBoolean(kioskerActivity, Constants.KIOSKER_RESET_WEBCACHE);
        if (clearCache) {
            for (WebView webView : webViews) webView.clearCache(true);
            Constants.setBoolean(kioskerActivity, false, Constants.KIOSKER_RESET_WEBCACHE);
        }
    }

    private void handleWebViewSetup(int layout) {
        if (!homeWebPages.isEmpty()) {
            float weight = WebHelper.layoutTranslator(layout, true);
            // If the weight to the main view is "below" fullscreen and there are alternative sites set the main view to fullscreen.
            if (weight < 1.0 && sitesWebPages.isEmpty())
                setupWebView(true, homeWebPages.get(0), 1.0f, true);
            if (weight > 0.0)
                setupWebView(true, homeWebPages.get(0), weight, true);
        }

        if (!sitesWebPages.isEmpty() && !fullScreenMode) {
            float weight = WebHelper.layoutTranslator(layout, false);
            if (weight > 0.0)
                setupWebView(false, sitesWebPages.get(0), weight, true);
        }
    }

    private void handleAutoCycleSecondary(LinkedHashMap settings) {
        // Only handle secondary cycling if we are not in fullscreen layout
        if (!fullScreenMode) {
            boolean allowSwitching = SettingsExtractor.getBoolean(settings, "allowSwitching");
            Constants.setBoolean(kioskerActivity, allowSwitching, Constants.KIOSKER_ALLOW_SWITCHING_ID);
            boolean autoCycleSecondary = SettingsExtractor.getBoolean(settings, "autoCycleSecondary");
            if (autoCycleSecondary && sitesWebPages.size() > 2) {
                int autoCycleSecondaryPeriodMins = SettingsExtractor.getInteger(settings, "autoCycleSecondaryPeriodMins");
                if (autoCycleSecondaryPeriodMins > 0) {
                    secondaryCycleObservable = Observable.timer(autoCycleSecondaryPeriodMins, TimeUnit.MINUTES)
                            .repeat()
                            .observeOn(AndroidSchedulers.mainThread());
                    secondaryCycleObservable.subscribe(getCycleSecondarySubscriber());
                }
            }
        }
    }

    private Subscriber<Long> getCycleSecondarySubscriber() {
        if (secondaryCycleSubscriber != null && !secondaryCycleSubscriber.isUnsubscribed()) {
            secondaryCycleSubscriber.unsubscribe();
            subscribers.remove(secondaryCycleSubscriber);
        }

        secondaryCycleSubscriber = new Subscriber<Long>() {
            @Override
            public void onCompleted() {
                subscribers.remove(secondaryCycleSubscriber);
            }

            @Override
            public void onError(Throwable e) {
                CustomerErrorLogger.log("Error while cycling secondary screen.", e, kioskerActivity);
            }

            @Override
            public void onNext(Long l) {
                if (webViews.size() > 1 && !kioskerActivity.currentlyInStandbyPeriod) {
                    Log.d(Constants.TAG, "Cycling secondary screen.");
                    secondaryCycleIndex = (secondaryCycleIndex + 1) % sitesWebPages.size();
                    String url = sitesWebPages.get(secondaryCycleIndex).url;
                    // Get the secondary web view and load the next url in that
                    webViews.get(1).loadUrl(url);
                } else {
                    unsubscribe();
                    subscribers.remove(this);
                }
            }
        };
        // Add our subscriber to subscribers so that we can cancel it later
        subscribers.add(secondaryCycleSubscriber);
        return secondaryCycleSubscriber;
    }

    /**
     * Setup the WebViews we need.
     *
     * @param homeView       is this the main view, if so we don't allow the user to change the url.
     * @param webPage        the main url for this web view.
     * @param weight         how much screen estate should this main take?
     * @param allowReloading should this be reloaded according to the reloadPeriodMins from the settings?
     */
    protected void setupWebView(boolean homeView, WebPage webPage, float weight, boolean allowReloading) {
        WebView webView = getWebView();
        webViews.add(webView);
        webView.loadUrl(webPage.url);
        addTapToSettings(webView);
        if (reloadPeriodMins > 0 && allowReloading) {
            if (reloadSubscriber != null) {
                reloadSubscriber.unsubscribe();
                subscribers.remove(reloadSubscriber);
            }

            reloadSubscriber = reloadSubscriber(webView);
            Observable.timer(reloadPeriodMins, TimeUnit.MINUTES)
                    .repeat()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(reloadSubscriber);
        }

        // A frame layout enables us to overlay the navigation on the web view.
        FrameLayout frameLayout = new FrameLayout(kioskerActivity);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        frameLayout.setLayoutParams(params);

        // Add navigation options to the web view.
        NavigationLayout navigationLayout = new NavigationLayout(homeView, kioskerActivity, webView, sitesWebPages);
        navigationLayout.hideNavigation();
        navigationLayouts.add(navigationLayout);

        // Add the web view and our navigation in a frame layout.
        frameLayout.addView(webView);
        frameLayout.addView(navigationLayout);
        kioskerActivity.addView(frameLayout, weight);
    }

    /**
     * Returns a WebView with a specified weight.
     *
     * @return a WebView with the specified weight.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private WebView getWebView() {
        WebView.setWebContentsDebuggingEnabled(true);
        final WebView webView = new WebView(kioskerActivity);
        webView.setWebViewClient(new KioskerWebViewClient(errorReloadMins, kioskerActivity));
        webView.setWebChromeClient(new KioskerWebChromeClient());

        // Disable hardware acceleration because of rendering bug in Kit Kat. Sometimes it would throw "W/AwContents﹕ nativeOnDraw failed; clearing to background color." errors.
//        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        return webView;
    }

    /**
     * Add our secret taps for opening settings to a WebView.
     * Also adding touch recognition for restarting scheduled tasks
     * and showing navigation ui.
     */
    private void addTapToSettings(WebView webView) {
        webView.setOnTouchListener(new View.OnTouchListener() {
            @SuppressWarnings("SuspiciousMethodCalls")
            @Override
            // Create a click listener that will open settings on the correct number of taps.
            public boolean onTouch(View webView, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    kioskerActivity.userIsInteractingWithDevice = true;
                    kioskerActivity.stopScheduledTasks();

                    Date now = new Date();

                    // If we detect a double tap count down the taps needed to trigger settings.
                    if (now.getTime() - lastTap.getTime() < 300) {
                        if (taps == 2) {
                            taps = tapsToOpenSettings;
                            Intent i = new Intent(kioskerActivity, SettingsActivity.class);
                            IntentHelper.addDataToSettingsIntent(i, kioskerActivity);
                            kioskerActivity.startActivityForResult(i, 0);
                        }
                        taps--;
                    }
                    // If it is not a double tap reset the taps counter.
                    else
                        taps = tapsToOpenSettings;

                    lastTap = now;
                }

                // When the user lifts his finger, restart all scheduled tasks and show our navigation.
                else if (event.getAction() == MotionEvent.ACTION_UP) {
                    kioskerActivity.startScheduledTasks();
                    if (webViews.contains(webView))
                        navigationLayouts.get(webViews.indexOf(webView)).showNavigation();
                }
                return false;
            }
        });
    }

    public void clearWebViews() {
        if (webViews != null) {
            for (WebView webView : webViews) {
                webView.loadUrl("about:blank");
                webView.destroy();
            }
        }
        if (navigationLayouts != null)
            for (NavigationLayout navigationLayout : navigationLayouts)
                navigationLayout.removeAllViews();
        navigationLayouts = resetArray(navigationLayouts);
        webViews = resetArray(webViews);
        homeWebPages = resetArray(homeWebPages);
        sitesWebPages = resetArray(sitesWebPages);
    }

    private ArrayList resetArray(ArrayList array) {
        if (array != null) {
            array.clear();
            array = null;
        }
        return new ArrayList<>();
    }

    /**
     * Reloads all the web views.
     * The main view will load the default page.
     * Other web views will reload their current page.
     */
    public void reloadWebViews() {
        Observable.from(1).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                if (!webViews.isEmpty() && !homeWebPages.isEmpty())
                    for (int i = 0; i < webViews.size(); i++) {
                        if (i == 0)
                            webViews.get(i).loadUrl(homeWebPages.get(0).url);
                        else
                            webViews.get(i).reload();
                    }
            }
        });
    }

    /**
     * Get subscriber for reloading the web view.
     *
     * @param webView the web view you want reloaded.
     */
    private Subscriber<Long> reloadSubscriber(final WebView webView) {
        if (reloadSubscriber != null && !reloadSubscriber.isUnsubscribed()) {
            reloadSubscriber.unsubscribe();
            subscribers.remove(reloadSubscriber);
        }
        reloadSubscriber = new Subscriber<Long>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.e(Constants.TAG, "Error while reloading web view.", e);
            }

            @Override
            public void onNext(Long aLong) {
                String url = webView.getUrl();
                if (url == null || kioskerActivity.currentlyInStandbyPeriod) {
                    unsubscribe();
                    subscribers.remove(this);
                } else {
                    if (Constants.isNetworkAvailable(kioskerActivity)) {
                        Log.d(Constants.TAG, String.format("Reloading web view with url %s.", url));
                        webView.reload();
                        webView.clearCache(true);
                    } else
                        kioskerActivity.refreshDevice();
                }
            }
        };
        subscribers.add(reloadSubscriber);
        return reloadSubscriber;
    }

    public void startScreenSaverSubscription() {
        if (screenSaverController != null)
            screenSaverController.startScreenSaverSubscription();
    }

    public void stopScreenSaverSubscription() {
        if (screenSaverController != null)
            screenSaverController.stopScreenSaverSubscription();
    }

    public void startCycleSecondarySubscription() {
        if (secondaryCycleObservable != null)
            secondaryCycleObservable.subscribe(getCycleSecondarySubscriber());
    }

    public void stopCycleSecondarySubscription() {
        if (secondaryCycleSubscriber != null && !secondaryCycleSubscriber.isUnsubscribed()) {
            secondaryCycleSubscriber.unsubscribe();
            subscribers.remove(secondaryCycleSubscriber);
        }
    }

    public void startResetToHomeSubscription() {
        if (resetToHomeSubscriber != null && !resetToHomeSubscriber.isUnsubscribed()) {
            resetToHomeSubscriber.unsubscribe();
            subscribers.remove(resetToHomeSubscriber);
        }

        resetToHomeSubscriber = new Subscriber<Long>() {
            @Override
            public void onCompleted() {
                subscribers.remove(resetToHomeSubscriber);
            }

            @Override
            public void onError(Throwable e) {
                CustomerErrorLogger.log("Error while resetting to home.", e, kioskerActivity);
            }

            @Override
            public void onNext(Long l) {
                if (Constants.isNetworkAvailable(kioskerActivity) && !webViews.isEmpty() && !showingHomeUrl() && !kioskerActivity.currentlyInStandbyPeriod && !kioskerActivity.currentlyScreenSaving) {
                    Log.d(Constants.TAG, "Resetting to home.");
                    reloadWebViews();
                } else {
                    unsubscribe();
                    subscribers.remove(resetToHomeSubscriber);
                }
            }
        };
        Observable.timer(resetToHomeMins, TimeUnit.MINUTES).observeOn(AndroidSchedulers.mainThread()).subscribe(resetToHomeSubscriber);
        // Add our subscriber to subscribers so that we can cancel it later
        subscribers.add(resetToHomeSubscriber);
    }

    private boolean showingHomeUrl() {
        return webViews.get(0).getUrl().equals(Constants.getString(kioskerActivity, Constants.KIOSKER_HOME_URL_ID));
    }

    public void stopResetToHomeSubscription() {
        if (resetToHomeSubscriber != null && !resetToHomeSubscriber.isUnsubscribed()) {
            resetToHomeSubscriber.unsubscribe();
            subscribers.remove(resetToHomeSubscriber);
        }
    }
}