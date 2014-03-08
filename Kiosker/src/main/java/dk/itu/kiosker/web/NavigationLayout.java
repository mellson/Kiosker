package dk.itu.kiosker.web;

import android.animation.Animator;
import android.util.Log;
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
import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.models.Constants;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

public class NavigationLayout extends LinearLayout {
    private final Button backButton;
    private final Button forwardButton;
    private final Button homeButton;
    private final WebView webView;
    private final Observable<Long> navigationHideObservable = Observable.timer(Constants.NAVIGATION_ONSCREEN_TIME_SECONDS, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread());
    private final int MINIMUM_HEIGHT = 50;
    private Spinner titleSpinner;
    private boolean firstTimeHere = true;
    // if this is a sites web view and user is allowed to switch sites we should show the navigation ui immediately.
    private boolean allowSwitching = false;
    private String homeUrl;
    private Subscriber<Long> navigationHideSubscriber;

    private ArrayList<String> siteTitles;
    private ArrayList<String> siteUrls;

    public NavigationLayout(boolean homeView, MainActivity mainActivity, final WebView webView, String title, ArrayList<String> sitesWebPages) {
        super(mainActivity);
        this.webView = webView;

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        backButton = new Button(mainActivity);
        backButton.setLayoutParams(params);
        backButton.setText(mainActivity.getString(R.string.BackButton));
        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack())
                    webView.goBack();
            }
        });

        forwardButton = new Button(mainActivity);
        forwardButton.setLayoutParams(params);
        forwardButton.setText(mainActivity.getString(R.string.ForwardButton));
        forwardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoForward())
                    webView.goForward();
            }
        });

        homeButton = new Button(mainActivity);
        homeButton.setLayoutParams(params);
        homeButton.setText(mainActivity.getString(R.string.HomeButton));
        homeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                titleSpinner.setSelection(0);
                webView.loadUrl(homeUrl);
            }
        });

        this.addView(backButton);
        this.addView(forwardButton);
        this.addView(homeButton);

        // Add the site selector if we are on the secondary panel and the user is allowed to change sites.
        allowSwitching = homeView ? false : Constants.getAllowSwitching(mainActivity);
        if (!homeView && allowSwitching) {
            siteTitles = new ArrayList<>();
            siteUrls = new ArrayList<>();

            // Split the incoming sites array into titles and urls
            for (int i = 0; i < sitesWebPages.size(); i++) {
                if (i % 2 == 0)
                    siteUrls.add(sitesWebPages.get(i));
                else
                    siteTitles.add(sitesWebPages.get(i));
            }

            titleSpinner = new Spinner(mainActivity);
            titleSpinner.setLayoutParams(params);
            ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(mainActivity, android.R.layout.simple_spinner_dropdown_item, siteTitles);
            titleSpinner.setAdapter(spinnerArrayAdapter);
            titleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    webView.loadUrl(siteUrls.get(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
            this.addView(titleSpinner);
        }
    }

    /**
     * Show navigation if we are not showing the home screen.
     *
     * @return true if we are not on the home screen.
     */
    void hideNavigation() {
        if (this.getAlpha() > 0)
            animateView(this, true);
    }

    public void showNavigation() {
        // The first time a user tries to navigate we set the home url to the current one on display.
        if (firstTimeHere) {
            homeUrl = webView.getUrl();
            firstTimeHere = false;
        }
        if (allowSwitching || !webView.getUrl().equals(homeUrl)) {
            backButton.setEnabled(webView.canGoBack());
            forwardButton.setEnabled(webView.canGoForward());
            animateView(this, false);
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
        float alpha = 0.0f;
        if (!hide)
            alpha = 1.0f;
        view.animate().setDuration(Constants.NAVIGATION_ANIMATION_TIME_MILLISECONDS).alpha(alpha).setListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (hide)
                    view.setVisibility(View.INVISIBLE);
                else
                    view.setVisibility(View.VISIBLE);
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
