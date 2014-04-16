package dk.itu.kiosker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

import dk.itu.kiosker.R;
import dk.itu.kiosker.controllers.HardwareController;
import dk.itu.kiosker.models.Constants;


public class SettingsActivity extends Activity {
    private String PASSWORD_HASH;
    private String MASTER_PASSWORD_HASH;
    private String PASSWORD_SALT;
    private String MASTER_PASSWORD_SALT;
    private Boolean resetDevice = false;
    private Boolean allowHome = false;
    private Boolean wrongOrNoPasswordEntered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        keepScreenOn();

        PASSWORD_HASH = this.getIntent().getStringExtra(Constants.KIOSKER_PASSWORD_HASH_ID);
        MASTER_PASSWORD_HASH = this.getIntent().getStringExtra(Constants.KIOSKER_MASTER_PASSWORD_HASH_ID);
        PASSWORD_SALT = this.getIntent().getStringExtra(Constants.KIOSKER_PASSWORD_SALT_ID);
        MASTER_PASSWORD_SALT = this.getIntent().getStringExtra(Constants.KIOSKER_MASTER_PASSWORD_SALT_ID);

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
                HardwareController.showNavigationUI();
                startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
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
        wrongOrNoPasswordEntered = true;

        // Set an EditText view to get user input
        EditText passwordField = new EditText(this);
        passwordField.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordField.setTransformationMethod(PasswordTransformationMethod.getInstance());
        final EditText[] input = {passwordField};
        final Boolean[] okToEnter = {false};
        final Boolean[] hasEnteredPassword = {false};
        new AlertDialog.Builder(this)
                .setTitle("Enter device password")
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (!okToEnter[0]) {
                            String toastMessage = hasEnteredPassword[0] ? "Wrong password!" : "Restarting application!";
                            Toast.makeText(SettingsActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                })
                .setView(input[0])
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable inputValue = input[0].getText();
                        String encodedPassword = "";
                        String encodedMasterPassword = "";
                        if (inputValue != null) {
                            encodedPassword = encryptPassword(encryptPassword(PASSWORD_SALT + inputValue.toString()));
                            encodedMasterPassword = encryptPassword(encryptPassword(MASTER_PASSWORD_SALT + inputValue.toString()));
                        }
                        if (encodedPassword.equals(PASSWORD_HASH) || encodedMasterPassword.equals(MASTER_PASSWORD_HASH)) {
                            Toast.makeText(SettingsActivity.this, "Correct password!", Toast.LENGTH_SHORT).show();
                            setupSettingsView();
                            okToEnter[0] = true;
                            wrongOrNoPasswordEntered = false;
                        } else if (!encodedPassword.equals(PASSWORD_HASH) || !encodedMasterPassword.equals(MASTER_PASSWORD_HASH)) {
                            hasEnteredPassword[0] = true;
                        }
                    }
                }).setNegativeButton("Restart App", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                restartApp(null);
            }
        }).show();
    }

    @Override
    public void finish() {
        // Prepare data intent
        Intent data = new Intent();

        // Did the user input a wrong password?
        data.putExtra(Constants.KIOSKER_WRONG_OR_NO_PASSWORD_ID, wrongOrNoPasswordEntered);

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

    public void showNavigationUI(View v) {
        HardwareController.showNavigationUI();
    }

    public void restartApp(View v) {
        Constants.restartApp(this);
        finish();
    }

    public void killApp(View v) {
        Constants.killApp(this);
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        HardwareController.handleNavigationUI();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private static String encryptPassword(String password) {
        String sha1 = "";
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(password.getBytes("UTF-8"));
            sha1 = byteToHex(crypt.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return sha1;
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}