package dk.itu.kiosker.web;

import android.animation.Animator;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

import dk.itu.kiosker.R;
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
    private boolean firstTimeHere = true;
    private String homeUrl;
    private Subscriber<Long> navigationHideSubscriber;

    public NavigationLayout(Context context, final WebView webView, String title) {
        super(context);
        this.webView = webView;

        backButton = new Button(context);
        backButton.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        backButton.setText(context.getString(R.string.BackButton));
        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack())
                    webView.goBack();
            }
        });

        forwardButton = new Button(context);
        forwardButton.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        forwardButton.setText(context.getString(R.string.ForwardButton));
        forwardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoForward())
                    webView.goForward();
            }
        });

        homeButton = new Button(context);
        homeButton.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        homeButton.setText(context.getString(R.string.HomeButton));
        homeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.loadUrl(homeUrl);
            }
        });

        TextView titleTextView = new TextView(context);
        titleTextView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        titleTextView.setText(title);

        this.addView(backButton);
        this.addView(forwardButton);
        this.addView(homeButton);
        this.addView(titleTextView);
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
        if (!webView.getUrl().equals(homeUrl)) {
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
