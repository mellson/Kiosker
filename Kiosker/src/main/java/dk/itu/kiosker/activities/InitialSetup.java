package dk.itu.kiosker.activities;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.Toast;

import dk.itu.kiosker.models.Constants;

public class InitialSetup {
    /**
     * This method takes care of the initial setup of the device.
     * User needs to input a base url for the json settings.
     * <p/>
     * This initial run can be triggered by resetting the device.
     */
    public static void start(final KioskerActivity kioskerActivity) {
        Constants.setInitialRun(kioskerActivity, true);
        kioskerActivity.updateMainStatus("Initial Run");
        kioskerActivity.updateSubStatus("Please set the base url.");
        final EditText et = new EditText(kioskerActivity);
        et.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        et.setGravity(Gravity.CENTER);
        et.setTextSize(25);
        et.setBackgroundColor(Color.BLACK);
        et.setAlpha(0.7f);
        et.setSingleLine(true);
        et.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String baseUrl = et.getText().toString();
                    if (baseUrl.isEmpty() || !URLUtil.isValidUrl(baseUrl)) {
                        Toast.makeText(kioskerActivity, "You did not enter a valid base url", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    // Hide the keyboard
                    Constants.setJsonBaseUrl(kioskerActivity, baseUrl);
                    InputMethodManager imm = (InputMethodManager) kioskerActivity.getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
                    Constants.setInitialRun(kioskerActivity, false);
                    kioskerActivity.refreshDevice();
                    return true;
                }
                return false;
            }
        });
        et.requestFocus();
        kioskerActivity.mainLayout.addView(et);
    }
}
