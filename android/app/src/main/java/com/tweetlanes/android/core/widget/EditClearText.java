package com.tweetlanes.android.core.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

public class EditClearText extends EditText {

    private EditClearTextListener mListener;

    public EditClearText(Context context) {
        super(context);
        init();
    }

    public EditClearText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public EditClearText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    void init() {

        // Manually set the padding. For some reason this doesn't seem to want
        // to work when set in the XML file
        setPadding(getPaddingLeft(), getPaddingTop(), 8, getPaddingBottom());

        // if the Close image is displayed and the user remove his finger from
        // the button, clear it. Otherwise do nothing
        setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (mListener != null) {
                    mListener.onTouch(v, event);
                }

                if (getCompoundDrawables()[2] == null) return false;

                if (event.getAction() != MotionEvent.ACTION_UP) return false;


                return false;
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.widget.TextView#onKeyPreIme(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {

        // From http://stackoverflow.com/a/4390592/328679
        // TODO: Consider this from the comments:
        // "I am getting reports from Android users with Hardware keyboards that
        // doing this somehow interferes with key presses."
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {

            if (mListener != null) {
                if (mListener.onBackButtonPressed()) {
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /*
     *
	 */
    public interface EditClearTextListener {

        public boolean canClearText();

        /*
         * Triggered when the back button is pressedEditClearTextListener
         */
        public boolean onBackButtonPressed();

        /*
         * Triggered when the view is touched. Use as a replacement for
         * setOnTouchListener(), which EditClearText overrides
         */
        public void onTouch(View v, MotionEvent event);

    }

    /*
     *
	 */
    public void setEditClearTextListener(EditClearTextListener listener) {
        mListener = listener;
    }
}
