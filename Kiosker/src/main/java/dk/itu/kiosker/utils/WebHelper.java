package dk.itu.kiosker.utils;

import android.util.Log;
import android.webkit.WebView;

import dk.itu.kiosker.models.Constants;
import rx.Subscriber;

public class WebHelper {
    /**
     * Get subscriber for reloading the web view.
     *
     * @param webView the web view you want reloaded.
     */
    public static Subscriber<Long> reloadSubscriber(final WebView webView) {
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
                if (url == null)
                    this.unsubscribe();
                else {
                    Log.d(Constants.TAG, String.format("Reloading web view with url %s.", webView.getUrl()));
                    webView.reload();
                }
            }
        };
    }

    /**
     * Translate a layout int to a float weight value used to distribute web views on the screen.
     * @param layout the layout to transform. 0 equals full screen.
     * @param mainWebPage is this a translation for the main view?
     * @return a float value indicating how much screen estate a view should take. 0.1f for 10% etc.
     */
    public static float layoutTranslator(int layout, boolean mainWebPage) {
        switch (layout) {
            case 1:
                return 0.5f;
            case 2:
                if (mainWebPage)
                    return 0.6f;
                else
                    return 0.4f;
            case 3:
                if (mainWebPage)
                    return 0.7f;
                else
                    return 0.3f;
            case 4:
                if (mainWebPage)
                    return 0.8f;
                else
                    return 0.2f;
            default: // Our default is the fullscreen layout
                if (mainWebPage)
                    return 1.0f;
                else
                    return 0f;
        }
    }
}
