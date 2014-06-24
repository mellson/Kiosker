package dk.itu.kiosker.web;

import android.content.Context;
import android.webkit.WebView;

public class KioskerWebView extends WebView {
    public KioskerWebViewClient client;

    public KioskerWebView(Context context) {
        super(context);
    }

    public void stopSensors() {
        client.stopSensors();
    }
}
