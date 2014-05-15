package dk.itu.kiosker.web;

import android.animation.Animator;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.R;
import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.models.Constants;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class NavigationLayout extends LinearLayout {
    public static final float NAVIGATION_SHOWN_ALPHA = 0.7f;
    private static final float NAVIGATION_HIDDEN_ALPHA = 0.0f;
    private final Button backButton;
    private final Button forwardButton;
    private final WebView webView;
    private final Observable<Long> navigationHideObservable = Observable.timer(Constants.NAVIGATION_ONSCREEN_TIME_SECONDS, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread());
    private final LinearLayout navigationControls;
    private final KioskerActivity kioskerActivity;
    private Spinner titleSpinner;
    private boolean firstTimeHere = true;
    // if this is a sites web view and user is allowed to switch sites we should show the navigation ui immediately.
    private boolean allowSwitching = false;
    private String homeUrl;
    private Subscriber<Long> navigationHideSubscriber;

    public NavigationLayout(boolean homeView, KioskerActivity kioskerActivity, final WebView webView, final ArrayList<WebPage> sitesWebPages) {
        super(kioskerActivity);
        this.webView = webView;
        this.setGravity(Gravity.CENTER_VERTICAL);
        this.kioskerActivity = kioskerActivity;

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        navigationControls = new LinearLayout(kioskerActivity);
        navigationControls.setLayoutParams(params);
        navigationControls.setOrientation(HORIZONTAL);
        navigationControls.setBackgroundColor(Color.BLACK);

        Typeface font = Typeface.createFromAsset(kioskerActivity.getAssets(), "fontawesome-webfont.ttf");
        backButton = new Button(kioskerActivity, null, android.R.attr.buttonStyleSmall);
        backButton.setTypeface(font);
        backButton.setText(kioskerActivity.getString(R.string.icon_back));
        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack())
                    webView.goBack();
            }
        });

        forwardButton = new Button(kioskerActivity, null, android.R.attr.buttonStyleSmall);
        forwardButton.setTypeface(font);
        forwardButton.setText(kioskerActivity.getString(R.string.icon_forward));
        forwardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoForward())
                    webView.goForward();
            }
        });

        Button homeButton = new Button(kioskerActivity, null, android.R.attr.buttonStyleSmall);
        homeButton.setTypeface(font);
        homeButton.setText(kioskerActivity.getString(R.string.icon_home));
        homeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (titleSpinner != null)
                    titleSpinner.setSelection(0);
                webView.loadUrl(homeUrl);
            }
        });

        navigationControls.addView(backButton);
        navigationControls.addView(forwardButton);
        navigationControls.addView(homeButton);

        // Add the site selector if we are on the secondary panel and the user is allowed to change sites.
        allowSwitching = !homeView && Constants.getBoolean(kioskerActivity, Constants.KIOSKER_ALLOW_SWITCHING_ID);
        if (!homeView && allowSwitching) {
            ArrayList<String> siteTitles = new ArrayList<>();
            for (WebPage webPage : sitesWebPages) {
                siteTitles.add(webPage.title);
            }

            titleSpinner = new Spinner(kioskerActivity);
            titleSpinner.setPadding(0, 0, 0, 0);
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(kioskerActivity, android.R.layout.simple_spinner_dropdown_item, siteTitles);
            titleSpinner.setAdapter(spinnerArrayAdapter);
            titleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    webView.loadUrl(sitesWebPages.get(position).url);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            navigationControls.addView(titleSpinner);
        }
        this.addView(navigationControls);
    }

    /**
     * Show navigation if we are not showing the home screen.
     */
    public void hideNavigation() {
        if (navigationControls.getAlpha() > 0)
            animateView(navigationControls, true);
    }

    public void showNavigation() {
        // The first time a user tries to navigate we set the home url to the current one on display.
        if (firstTimeHere) {
            homeUrl = webView.getUrl();
            Constants.setString(kioskerActivity, homeUrl, Constants.KIOSKER_HOME_URL_ID);
            firstTimeHere = false;
        }
        if (allowSwitching || !webView.getUrl().equals(homeUrl)) {
            backButton.setEnabled(webView.canGoBack());
            forwardButton.setEnabled(webView.canGoForward());
            animateView(navigationControls, false);
            if (navigationHideSubscriber != null && !navigationHideSubscriber.isUnsubscribed()) {
                navigationHideSubscriber.unsubscribe();
                navigationHideObservable.subscribe(getNavigationHideSubscriber());
            } else
                navigationHideObservable.subscribe(getNavigationHideSubscriber());
        }
    }

    private Subscriber<Long> getNavigationHideSubscriber() {
        navigationHideSubscriber = new Subscriber<Long>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.e(Constants.TAG, "Error while trying to hide navigation controls.", e);
            }

            @Override
            public void onNext(Long aLong) {
                hideNavigation();
            }
        };
        return navigationHideSubscriber;
    }

    private void animateView(final View view, final boolean hide) {
        float alpha = NAVIGATION_HIDDEN_ALPHA;
        if (!hide)
            alpha = NAVIGATION_SHOWN_ALPHA;
        view.animate().setDuration(Constants.NAVIGATION_ANIMATION_TIME_MILLISECONDS).alpha(alpha).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (hide) {
                    view.setClickable(false);
                    view.setVisibility(View.INVISIBLE);
                } else {
                    view.setClickable(true);
                    view.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }
}
