package dk.itu.kiosker.web;

import android.animation.Animator;
import android.content.Context;
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
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class NavigationLayout extends LinearLayout {
    private final Button backButton;
    private final Button forwardButton;
    private final Button homeButton;

    private String homeUrl;
    private WebView webView;

    public NavigationLayout(Context context, final WebView webView, final String homeUrl, String title) {
        super(context);
        this.webView = webView;
        this.homeUrl = homeUrl;

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
    public void hideNavigation() {
        if (this.getAlpha() > 0)
            animateView(this, true);
    }

    public void showNavigation() {
        if (webView.getUrl() != homeUrl) {
            backButton.setEnabled(webView.canGoBack());
            forwardButton.setEnabled(webView.canGoForward());
            animateView(this, false);

            Observable.timer(Constants.NAVIGATION_ONSCREEN_TIME_SECONDS, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
                @Override
                public void call(Long aLong) {
                    hideNavigation();
                }
            });
        }
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
