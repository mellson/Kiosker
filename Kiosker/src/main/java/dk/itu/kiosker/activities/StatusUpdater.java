package dk.itu.kiosker.activities;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import java.util.Random;

import dk.itu.kiosker.R;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class StatusUpdater {
    /**
     * Overloaded method which implies that we are not yet finished downloading settings.
     *
     * @param activity
     * @param id
     * @param status
     */
    public static void updateTextView(Activity activity, final int id, final String status) {
        updateTextView(activity, id, status, false);
    }

    /**
     * Update a text view defined in a layout file.
     * We use it to update the status while downloading settings.
     *
     * @param id       id of the text view.
     * @param status   the status you would like to show in the text view.
     * @param finished are we finished downloading settings.
     */
    private static void updateTextView(final Activity activity, final int id, final String status, final Boolean finished) {
        Observable o = Observable.from(1);
        o.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1() {
            @Override
            public void call(Object o) {
                TextView textView = (TextView) activity.findViewById(id);
                View view = textView.getRootView();

                // While we download settings we flash the background in random colors.
                int color;
                if (!finished) {
                    textView.setVisibility(View.VISIBLE);
                    Random rnd = new Random();
                    color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                } else {
                    color = Color.BLACK;
                    textView.setVisibility(View.GONE);
                }
                textView.setText(status);
                view.setBackgroundColor(color);
            }
        });
    }

    /**
     * Remove the status text views we have on screen while fetching settings.
     */
    public static void removeStatusTextViews(Activity activity) {
        updateTextView(activity, R.id.downloadingTextView, "", true);
        updateTextView(activity, R.id.statusTextView, "", true);
    }
    //endregion
}
