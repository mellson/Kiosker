package dk.itu.kiosker.activities;

import android.app.Activity;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Random;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

class StatusUpdater {
    private final TextView mainStatusTextView;
    private final TextView subStatusTextView;
    private final LinearLayout mainLayout;

    public StatusUpdater(MainActivity mainActivity) {
        mainStatusTextView = getTextView(60, 0.7f, mainActivity);
        subStatusTextView = getTextView(25, 0.5f, mainActivity);
        this.mainLayout = mainActivity.mainLayout;
    }

    private TextView getTextView(int textSize, float alpha, Activity activity) {
        TextView tv = new TextView(activity);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setLayoutParams(layoutParams);
        tv.setBackgroundColor(Color.BLACK);
        tv.setTextColor(Color.WHITE);
        tv.setAlpha(alpha);
        return tv;
    }

    /**
     * Update a text view defined in a layout file.
     * We use it to update the status while downloading settings.
     *
     * @param textView the text view you would like to update.
     * @param status   the status you would like to show in the text view.
     */
    private void updateTextView(final TextView textView, final String status) {
        Observable<Long> o = Observable.from(1L);
        o.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
            @Override
            public void call(Long aLong) {
                // While we download settings we flash the background in random colors.
                Random rnd = new Random();
                int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                textView.setText(status);
                mainLayout.setBackgroundColor(color);
            }
        });
    }

    /**
     * Remove the status text views we have on screen while fetching settings.
     */
    public void removeStatusTextViews() {
        mainLayout.removeView(mainStatusTextView);
        mainLayout.removeView(subStatusTextView);
        mainLayout.setBackgroundColor(Color.BLACK);
    }

    public void updateMainStatus(String status) {
        if (mainLayout.indexOfChild(mainStatusTextView) == -1)
            mainLayout.addView(mainStatusTextView);
        updateTextView(mainStatusTextView, status);
    }

    public void updateSubStatus(String status) {
        if (mainLayout.indexOfChild(subStatusTextView) == -1)
            mainLayout.addView(subStatusTextView);
        updateTextView(subStatusTextView, status);
    }
    //endregion
}
