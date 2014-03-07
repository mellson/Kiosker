package dk.itu.kiosker.web;

import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;

import dk.itu.kiosker.models.Constants;

public class KioskerWebChromeClient extends WebChromeClient {
    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        Log.d(Constants.TAG, origin);
        callback.invoke(origin, true, true);
    }
}
