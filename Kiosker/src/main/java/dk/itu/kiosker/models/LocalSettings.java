package dk.itu.kiosker.models;

import android.content.SharedPreferences;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.LinkedHashMap;

import dk.itu.kiosker.activities.MainActivity;
import dk.itu.kiosker.utils.JsonFetcher;

public class LocalSettings {
    /**
     * This removes the safe settings from the device if there are any.
     * This gets called after a device reset.
     */
    public static void removeSafeSettings(MainActivity mainActivity) {
        Log.d(Constants.TAG, "Removing safe settings.");
        SharedPreferences.Editor editor = mainActivity.getPreferences(mainActivity.MODE_PRIVATE).edit();
        editor.remove(Constants.SAFE_JSON);
        editor.commit();
    }

    public static void setSafeJson(MainActivity mainActivity, LinkedHashMap settings) {
        String json = null;
        try {
            json = JsonFetcher.mapper.writeValueAsString(settings);
        } catch (JsonProcessingException e) {
            Log.e(Constants.TAG, "Error while saving safe settings.", e);
        }
        SharedPreferences.Editor editor = mainActivity.getPreferences(mainActivity.MODE_PRIVATE).edit();
        editor.putString(Constants.SAFE_JSON, json);
        editor.commit();
    }

    public static LinkedHashMap getSafeJson(MainActivity mainActivity) {
        SharedPreferences prefs = mainActivity.getPreferences(mainActivity.MODE_PRIVATE);
        String restoredJson = prefs.getString(Constants.SAFE_JSON, null);
        if (restoredJson != null) {
            try {
                LinkedHashMap json = JsonFetcher.mapper.readValue(restoredJson, LinkedHashMap.class);
                return json;
            } catch (IOException e) {
                Log.e(Constants.TAG, "Error while loading safe settings.", e);
            }
        }
        LinkedHashMap emptyMap = new LinkedHashMap();
        return emptyMap;
    }
}
