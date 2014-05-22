package dk.itu.kiosker.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import dk.itu.kiosker.R;
import dk.itu.kiosker.controllers.HardwareController;
import dk.itu.kiosker.models.Constants;

public class ShowSettingsActivity extends Activity {
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_settings);
        keepScreenOn();
    }

    protected void keepScreenOn() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        getWindow().setAttributes(params);
    }

    @Override
    public void onStart() {
        super.onStart();
        getSettings();
        HardwareController.showNavigationUI();
    }

    private void getSettings() {
        tv = (TextView) findViewById(R.id.showSettingsTextView);
        tv.setText(Constants.settingsText);
    }

    @Override
    public void onStop() {
        super.onStop();
        HardwareController.handleNavigationUI();
    }
}
