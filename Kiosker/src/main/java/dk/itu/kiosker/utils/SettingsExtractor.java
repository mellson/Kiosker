package dk.itu.kiosker.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class SettingsExtractor {
    /**
     * @param map
     * @param key
     * @return the array from the map or an empty array
     */
    public static ArrayList<String> getStringArrayList(LinkedHashMap map, String key) {
        return map.containsKey(key) ? (ArrayList<String>) map.get(key) : new ArrayList<String>();
    }

    /**
     * @param map
     * @param key
     * @return the integer or -1
     */
    public static int getInteger(LinkedHashMap map, String key) {
        return map.containsKey(key) ? (int) map.get(key) : -1;
    }

    public static boolean getBoolean(LinkedHashMap map, String key) {
        return map.containsKey(key) && (boolean) map.get(key);
    }

    public static String getString(LinkedHashMap map, String key) {
        return map.containsKey(key) ? (String) map.get(key) : "";
    }
}
