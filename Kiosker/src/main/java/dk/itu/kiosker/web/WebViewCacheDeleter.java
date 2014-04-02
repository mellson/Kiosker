package dk.itu.kiosker.web;

import android.content.Context;

public class WebViewCacheDeleter {
    public static void deleteWebViewCache(Context context) {
        context.deleteDatabase("webview.db");
        context.deleteDatabase("webviewCache.db");
    }
}
