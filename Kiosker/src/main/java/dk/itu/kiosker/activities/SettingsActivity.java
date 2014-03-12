package dk.itu.kiosker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import dk.itu.kiosker.R;
import dk.itu.kiosker.models.Constants;


public class SettingsActivity extends Activity {
    private String PASSWORD_HASH;
    private String MASTER_PASSWORD_HASH;
    private Boolean refreshSettings = false;
    private Boolean resetDevice = false;
    private Boolean allowHome = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        keepScreenOn();

        PASSWORD_HASH = this.getIntent().getStringExtra(Constants.KIOSKER_PASSWORD_HASH_ID);
        MASTER_PASSWORD_HASH = this.getIntent().getStringExtra(Constants.KIOSKER_MASTER_PASSWORD_HASH_ID);
        if ((PASSWORD_HASH != null && !PASSWORD_HASH.isEmpty())
                || (MASTER_PASSWORD_HASH != null && !MASTER_PASSWORD_HASH.isEmpty()))
            showPasswordDialog();
        else
            setupSettingsView();
    }

    private void setupSettingsView() {
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
        final EditText[] input = {new EditText(this)};
        final Boolean[] okToEnter = {false};
        final Boolean[] hasEnteredPassword = {false};
        new AlertDialog.Builder(this)
                .setTitle("Enter device password")
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (!okToEnter[0]) {
                            String toastMessage = hasEnteredPassword[0] ? "Wrong password!" : "You need to enter a password!";
                            Toast.makeText(SettingsActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                })
                .setView(input[0])
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable inputValue = input[0].getText();
                        String md5 = "";
                        if (inputValue != null)
                            md5 = MD5(inputValue.toString());
                        if (md5.equals(PASSWORD_HASH) || md5.equals(MASTER_PASSWORD_HASH)) {
                            Toast.makeText(SettingsActivity.this, "Correct password!", Toast.LENGTH_SHORT).show();
                            setupSettingsView();
                            okToEnter[0] = true;
                        } else if (!md5.equals(PASSWORD_HASH) || !md5.equals(MASTER_PASSWORD_HASH)) {
                            hasEnteredPassword[0] = true;
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
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
     * @param view the view to add touches to.
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

    public String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte anArray : array) {
                sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            Log.e(Constants.TAG, "Error while trying to create MD5 value.", e);
        }
        return null;
    }
}
