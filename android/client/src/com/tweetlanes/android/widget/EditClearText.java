package com.tweetlanes.android.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import com.tweetlanes.android.AppSettings;
import com.tweetlanes.android.R;
// import android.graphics.Typeface;


public class EditClearText extends EditText {

    private Drawable mClearButtonImage = getResources()
            .getDrawable(
                    AppSettings.get().getCurrentTheme() == AppSettings.Theme.Holo_Dark ? R.drawable.ic_navigation_cancel_dark
                            : R.drawable.ic_navigation_cancel_light);
    private EditClearTextListener mListener;
    private Integer mInitialXSize;
    private Integer mInitialYSize;

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

                boolean canClearText = true;
                if (mListener != null) {
                    canClearText = mListener.canClearText();
                    mListener.onTouch(v, event);
                }

                if (getCompoundDrawables()[2] == null) return false;

                if (event.getAction() != MotionEvent.ACTION_UP) return false;

                if (canClearText == true
                        && event.getX() > getWidth() - getPaddingRight()
                                - mClearButtonImage.getIntrinsicWidth()
                        && event.getY() > getHeight() - getPaddingBottom()
                                - mClearButtonImage.getIntrinsicHeight()) {

                    setText("");
                    handleClearButton();
                    if (mListener != null) {
                        mListener.onClearPressed();
                    }
                }
                return false;
            }
        });

        // if text changes, take care of the button
        addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {

                handleClearButton();
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }
        });
    }

    void handleClearButton() {
        if (getText().toString().equals("")) {
            // add the clear button
            setCompoundDrawables(getCompoundDrawables()[0],
                    getCompoundDrawables()[1], null, getCompoundDrawables()[3]);
        } else {
            // remove clear button
            setCompoundDrawables(getCompoundDrawables()[0],
                    getCompoundDrawables()[1], mClearButtonImage,
                    getCompoundDrawables()[3]);
        }
    }

    /*
     * (non-Javadoc)
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
                if (mListener.onBackButtonPressed() == true) {
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    /*
     * (non-Javadoc)
     * @see android.view.View#onSizeChanged(int, int, int, int)
     */
    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {

        boolean firstTime = false;

        if (mInitialXSize == null && mInitialYSize == null) {
            mInitialXSize = xNew;
            mInitialYSize = Math.min(yNew, 75); // TODO: Magic number. Find a
                                                // better solution when the box
                                                // is initialised with multiple
                                                // lines
            firstTime = true;
        }

        // Resize the clear button so it doesn't grow larger than the EditText
        // field itself
        int buttonSize = (int) (mInitialYSize.intValue() * .60f);

        // Adjust the y position of the clear button so it is bottom aligned
        int buttonYOffset;
        if (getLineCount() > 1) {
            buttonYOffset = yNew - buttonSize - (yNew / 2);
        } else {
            buttonYOffset = 0;
        }
        mClearButtonImage.setBounds(0, buttonYOffset, buttonSize, buttonYOffset
                + buttonSize);

        // Bit of a hack, but does the job
        if (firstTime) {
            // There may be initial text in the field, so we may need to display
            // the button
            handleClearButton();
        }

        if (mListener != null) {
            mListener.onSizeChanged(xNew, yNew, xOld, yOld, mInitialXSize,
                    mInitialYSize);
        }
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
         * Triggered when the clear button is pressed
         */
        public void onClearPressed();

        /*
         * Triggered when the view is touched. Use as a replacement for
         * setOnTouchListener(), which EditClearText overrides
         */
        public void onTouch(View v, MotionEvent event);

        /*
         * Triggered when the view's size is changed.
         */
        public void onSizeChanged(int xNew, int yNew, int xOld, int yOld,
                int xInitial, int yInitial);
    }

    /*
	 * 
	 */
    public void setEditClearTextListener(EditClearTextListener listener) {
        mListener = listener;
    }
}
