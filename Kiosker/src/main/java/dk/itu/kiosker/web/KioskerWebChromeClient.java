package dk.itu.kiosker.web;

import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;

import dk.itu.kiosker.models.Constants;

public class KioskerWebChromeClient extends WebChromeClient {

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        Log.d(Constants.TAG, origin);
        callback.invoke(origin, true, true);
    }

    /**
     * Here we catch error messages, including js errors, from the chrome client.
     * @param consoleMessage
     * @return
     */
    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        if (consoleMessage.messageLevel().equals(ConsoleMessage.MessageLevel.ERROR))
            Log.d(Constants.TAG, consoleMessage.message()); // TODO: How should we handle javascript errors
        return false;
    }
}
