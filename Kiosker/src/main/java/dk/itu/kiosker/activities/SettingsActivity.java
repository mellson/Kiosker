package dk.itu.kiosker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import dk.itu.kiosker.R;
import dk.itu.kiosker.models.Constants;


public class SettingsActivity extends Activity {
    private Boolean refreshSettings = false;
    private Boolean resetDevice = false;
    private Boolean allowHome = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        keepScreenOn();

//        showPasswordDialog();

        Button logButton = (Button) findViewById(R.id.logButton);
        logButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(SettingsActivity.this, LogActivity.class);
                startActivity(i);
            }
        });

        Button close = (Button) findViewById(R.id.closeButton);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Button settings = (Button) findViewById(R.id.settingsButton);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
            }
        });

        Button refreshSettingsButton = (Button) findViewById(R.id.refreshSettingsButton);
        refreshSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshSettings = true;
                finish();
            }
        });

        Button resetDeviceButton = (Button) findViewById(R.id.resetDeviceButton);
        resetDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetDevice = true;
                finish();
            }
        });

        EditText deviceIdEditText = (EditText) findViewById(R.id.deviceIdEditText);
        String deviceId = this.getIntent().getStringExtra(Constants.KIOSKER_DEVICE_ID);
        deviceIdEditText.setText(deviceId);

        final CheckBox allowHomeCheckbox = (CheckBox) findViewById(R.id.allowHomeCheckBox);
        allowHome = this.getIntent().getBooleanExtra(Constants.KIOSKER_ALLOW_HOME_ID, false);
        allowHomeCheckbox.setChecked(allowHome);
        // Only allow to allow home if the device is actually rooted
        allowHomeCheckbox.setEnabled(Constants.isDeviceRooted());

        Button saveAllowHomeButton = (Button) findViewById(R.id.saveAllowHomeButton);
        saveAllowHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                allowHome = allowHomeCheckbox.isChecked();
                finish();
            }
        });

        Button saveDeviceIdButton = (Button) findViewById(R.id.saveDeviceIdButton);
        saveDeviceIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshSettings = true;
                finish();
            }
        });

        EditText baseUrlEditText = (EditText) findViewById(R.id.baseUrlEditText);
        String baseUrl = Constants.JSON_BASE_URL;
        baseUrlEditText.setText(baseUrl);

        Button saveBaseUrlButton = (Button) findViewById(R.id.saveBaseUrlButton);
        saveBaseUrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshSettings = true;
                finish();
            }
        });

        TextView tv = (TextView) findViewById(R.id.settingsTextView);
        tv.setText(Constants.settingsText);

        // Let all views accept touch.
        allowTouches(findViewById(R.id.settingsMainLayout));
    }

    private void keepScreenOn() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        getWindow().setAttributes(params);
    }

    private void showPasswordDialog() {
        // Set an EditText view to get user input
        final EditText input = new EditText(this);

        new AlertDialog.Builder(this)
                .setTitle("Update Status")
                .setMessage("Hej")
                .setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        if (value != "hej")
                            finish();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (input.getText().toString() != "hej")
                    finish();
            }
        }).show();
    }

    @Override
    public void finish() {
        // Prepare data intent
        Intent data = new Intent();

        // Should the device be refreshed?
        data.putExtra(Constants.KIOSKER_REFRESH_SETTINGS_ID, refreshSettings);

        // Should the device be reset?
        data.putExtra(Constants.KIOSKER_RESET_DEVICE_ID, resetDevice);

        // Should the user be able to use the navigation UI?
        data.putExtra(Constants.KIOSKER_ALLOW_HOME_ID, allowHome);

        // Put in device id.
        EditText deviceIdEditText = (EditText) findViewById(R.id.deviceIdEditText);
        String deviceId = deviceIdEditText.getText().toString();
        data.putExtra(Constants.KIOSKER_DEVICE_ID, deviceId);

        // Put in base url.
        EditText baseUrlEditText = (EditText) findViewById(R.id.baseUrlEditText);
        String baseUrl = baseUrlEditText.getText().toString();
        data.putExtra(Constants.JSON_BASE_URL_ID, baseUrl);

        // Activity finished ok, return the data.
        setResult(RESULT_OK, data);

        super.finish();
    }

    /**
     * Setup view so that touches will hide the onscreen keyboard
     *
     * @param view
     */
    public void allowTouches(View view) {
        //Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {

                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard();
                    return false;
                }

            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                allowTouches(innerView);
            }
        }
    }

    public void hideSoftKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
    }
}
