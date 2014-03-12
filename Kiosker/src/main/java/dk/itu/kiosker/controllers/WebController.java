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

import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.activities.SettingsActivity;
import dk.itu.kiosker.models.Constants;
import dk.itu.kiosker.utils.SettingsExtractor;
import dk.itu.kiosker.utils.WebHelper;
import dk.itu.kiosker.web.KioskerWebChromeClient;
import dk.itu.kiosker.web.KioskerWebViewClient;
import dk.itu.kiosker.web.NavigationLayout;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

class WebController {
    private final MainActivity mainActivity;
    private final int tapsToOpenSettings = 5;
    private int taps = tapsToOpenSettings;
    // Has our main web view (home) at index 0 and the sites at index 1
    private final ArrayList<WebView> webViews;
    private final ArrayList<Subscriber> subscribers;
    private final ArrayList<NavigationLayout> navigationLayouts;
    private Date lastTap;
    private ArrayList<String> homeWebPages;
    private ArrayList<String> sitesWebPages;
    private int reloadPeriodMins;
    private int errorReloadMins;
    private boolean fullScreenMode;
    private ScreenSaverController screenSaverController;
    private Subscriber<Long> secondaryCycleSubscriber;
    private int secondaryCycleIndex;
    private Observable<Long> secondaryCycleObservable;

    public WebController(MainActivity mainActivity, ArrayList<Subscriber> subscribers) {
        this.mainActivity = mainActivity;
        this.subscribers = subscribers;
        lastTap = new Date();
        webViews = new ArrayList<>();
        navigationLayouts = new ArrayList<>();
    }

    public void handleWebSettings(LinkedHashMap settings) {
        reloadPeriodMins = SettingsExtractor.getInteger(settings, "reloadPeriodMins");
        errorReloadMins = SettingsExtractor.getInteger(settings, "errorReloadMins");

        // Get the layout from settings, if there is no layout defined fallback to 0 - fullscreen layout.
        int tempLayout = SettingsExtractor.getInteger(settings, "layout");
        int layout = (tempLayout >= 0) ? tempLayout : 0;
        fullScreenMode = layout == 0;

        homeWebPages = SettingsExtractor.getStringArrayList(settings, "home");
        sitesWebPages = SettingsExtractor.getStringArrayList(settings, "sites");

        handleWebViewSetup(layout);
        handleAutoCycleSecondary(settings);

        screenSaverController = new ScreenSaverController(mainActivity, subscribers, this);
        screenSaverController.handleScreenSaving(settings);
    }

    private void handleWebViewSetup(int layout) {
        if (homeWebPages.size() > 1) {
            float weight = WebHelper.layoutTranslator(layout, true);
            // If the weight to the main view is "below" fullscreen and there are alternative sites set the main view to fullscreen.
            if (weight < 1.0 && sitesWebPages.isEmpty())
                setupWebView(true, homeWebPages.get(0), 1.0f, true);
            if (weight > 0.0)
                setupWebView(true, homeWebPages.get(0), weight, true);
        }

        if (sitesWebPages.size() > 1 && !fullScreenMode) {
            float weight = WebHelper.layoutTranslator(layout, false);
            if (weight > 0.0)
                setupWebView(false, sitesWebPages.get(0), weight, true);
        }
    }

    private void handleAutoCycleSecondary(LinkedHashMap settings) {
        // Only handle secondary cycling if we are not in fullscreen layout
        if (!fullScreenMode) {
            boolean allowSwitching = SettingsExtractor.getBoolean(settings, "allowSwitching");
            Constants.setAllowSwitching(mainActivity, allowSwitching);
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
            }

            @Override
            public void onError(Throwable e) {
                Log.e(Constants.TAG, "Error while cycling secondary screen.", e);
            }

            @Override
            public void onNext(Long l) {
                if (webViews.size() > 1) {
                    Log.d(Constants.TAG, "Cycling secondary screen.");
                    secondaryCycleIndex = (secondaryCycleIndex += 2) % sitesWebPages.size();
                    String url = sitesWebPages.get(secondaryCycleIndex);
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
     *  @param homeView is this the main view, if so we don't allow the user to change the url.
     * @param homeUrl  the main url for this web view.
     * @param weight   how much screen estate should this main take?
     * @param allowReloading should this be reloaded according to the reloadPeriodMins from the settings?
     */
    protected void setupWebView(boolean homeView, String homeUrl, float weight, boolean allowReloading) {
        WebView webView = getWebView();
        webViews.add(webView);
        webView.loadUrl(homeUrl);
        addTapToSettings(webView);
        if (reloadPeriodMins > 0 && allowReloading) {
            Subscriber<Long> reloadSubscriber = reloadSubscriber(webView);

            // Add our subscriber to subscribers so that we can cancel it later
            subscribers.add(reloadSubscriber);
            Observable.timer(reloadPeriodMins, TimeUnit.MINUTES)
                    .repeat()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(reloadSubscriber);
        }

        // A frame layout enables us to overlay the navigation on the web view.
        FrameLayout frameLayout = new FrameLayout(mainActivity);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        frameLayout.setLayoutParams(params);

        // Add navigation options to the web view.
        NavigationLayout navigationLayout = new NavigationLayout(homeView, mainActivity, webView, sitesWebPages);
        navigationLayout.hideNavigation();
        navigationLayouts.add(navigationLayout);

        // Add the web view and our navigation in a frame layout.
        frameLayout.addView(webView);
        frameLayout.addView(navigationLayout);
        mainActivity.addView(frameLayout, weight);
    }



    /**
     * Returns a WebView with a specified weight.
     *
     * @return a WebView with the specified weight.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private WebView getWebView() {
        WebView.setWebContentsDebuggingEnabled(true);
        final WebView webView = new WebView(mainActivity);
        webView.setWebViewClient(new KioskerWebViewClient(errorReloadMins));
        webView.setWebChromeClient(new KioskerWebChromeClient());
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
                            i.putExtra(Constants.KIOSKER_ALLOW_HOME_ID, Constants.getAllowHome(mainActivity));
                            i.putExtra(Constants.KIOSKER_PASSWORD_HASH_ID, Constants.getPasswordHash(mainActivity));
                            i.putExtra(Constants.KIOSKER_MASTER_PASSWORD_HASH_ID, Constants.getMasterPasswordHash(mainActivity));
                            mainActivity.startActivityForResult(i, 0);
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
                    mainActivity.startScheduledTasks();
                    if (webViews.contains(webView))
                        navigationLayouts.get(webViews.indexOf(webView)).showNavigation();
                }
                return false;
            }
        });
    }

    public void clearWebViews() {
        for (WebView webView : webViews) {
            webView.destroy();
        }
        for (NavigationLayout navigationLayout : navigationLayouts) {
            navigationLayout.removeAllViews();
        }
        clearArray(navigationLayouts);
        clearArray(webViews);
        clearArray(homeWebPages);
        clearArray(sitesWebPages);
    }

    private void clearArray(ArrayList array) {
        if (array != null) array.clear();
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
                            webViews.get(i).loadUrl(homeWebPages.get(0));
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
        return new Subscriber<Long>() {
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
                if (url == null) {
                    unsubscribe();
                    subscribers.remove(this);
                }
                else {
                    Log.d(Constants.TAG, String.format("Reloading web view with url %s.", url));
                    webView.reload();
                }
            }
        };
    }

    public void startScreenSaverSubscription() {
        screenSaverController.startScreenSaverSubscription();
    }

    public void stopScreenSaverSubscription() {
        screenSaverController.stopScreenSaverSubscription();
    }

    public void startCycleSecondarySubscription() {
        Log.d(Constants.TAG, "Starting cycle subscriber.");
        secondaryCycleObservable.subscribe(getCycleSecondarySubscriber());
    }

    public void stopCycleSecondarySubscription() {
        Log.d(Constants.TAG, "Stopping cycle subscriber.");
        if (secondaryCycleSubscriber != null && !secondaryCycleSubscriber.isUnsubscribed()) {
            secondaryCycleSubscriber.unsubscribe();
            subscribers.remove(secondaryCycleSubscriber);
        }
    }
}