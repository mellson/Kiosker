package dk.itu.kiosker.utils;

import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;

import dk.itu.kiosker.models.Constants;

public class BluetoothDeviceTuple {
    public final String name;
    public final String address;
    public BluetoothDeviceTuple(String name, String address) {
        this.name = name;
        this.address = address;
    }

    @Override
    public String toString() {
        // Return a json object string
        return "{\"name\": \"" + name + "\",\"address\": \"" + address + "\"}";
    }
}