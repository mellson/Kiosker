package dk.itu.kiosker.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.LinkedHashMap;

import dk.itu.kiosker.activities.KioskerActivity;
import dk.itu.kiosker.utils.CustomerErrorLogger;
import dk.itu.kiosker.utils.JsonFetcher;

public class LocalSettings {
    /**
     * This removes the safe settings from the device if there are any.
     * This gets called after a device reset.
     */
    public static void removeSafeSettings(KioskerActivity kioskerActivity) {
        Log.d(Constants.TAG, "Removing safe settings.");
        SharedPreferences.Editor editor = kioskerActivity.getPreferences(Context.MODE_PRIVATE).edit();
        editor.remove(Constants.SAFE_JSON);
        editor.commit();
    }

    public static void setSafeJson(KioskerActivity kioskerActivity, LinkedHashMap settings) {
        String json = null;
        try {
            JsonFetcher jsonFetcher = new JsonFetcher();
            json = jsonFetcher.mapper.writeValueAsString(settings);
        } catch (JsonProcessingException e) {
            CustomerErrorLogger.log("Error while saving safe settings.", e, kioskerActivity);
        }
        SharedPreferences.Editor editor = kioskerActivity.getPreferences(Context.MODE_PRIVATE).edit();
        editor.putString(Constants.SAFE_JSON, json);
        editor.commit();
    }

    public static LinkedHashMap getSafeJson(KioskerActivity kioskerActivity) {
        SharedPreferences prefs = kioskerActivity.getPreferences(Context.MODE_PRIVATE);
        String restoredJson = prefs.getString(Constants.SAFE_JSON, null);
        if (restoredJson != null) {
            try {
                JsonFetcher jsonFetcher = new JsonFetcher();
                return jsonFetcher.mapper.readValue(restoredJson, LinkedHashMap.class);
            } catch (IOException e) {
                CustomerErrorLogger.log("Error while loading safe settings.", e, kioskerActivity);
            }
        }
        return new LinkedHashMap();
    }
}
