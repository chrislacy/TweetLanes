package com.tweetlanes.android.core.widget.viewpagerindicator;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.tweetlanes.android.core.R;

import java.util.ArrayList;

/**
 * This widget implements the dynamic action bar tab behavior that can change
 * across different configurations or circumstances.
 */
public class ListTabPageIndicator extends ListView implements PageIndicator {

    private Runnable mTabSelector;

    private final OnClickListener mTabClickListener = new OnClickListener() {

        public void onClick(View view) {
            TabView tabView = (TabView) view;
            mViewPager.setCurrentItem(tabView.getIndex());
        }
    };

    // private LinearLayout mTabLayout;
    private final Context mContext;
    private ViewPager mViewPager;
    private ViewPager.OnPageChangeListener mListener;

    private final LayoutInflater mInflater;

    int mMaxTabWidth;
    private int mSelectedTabIndex;

    public ListTabPageIndicator(Context context) {
        this(context, null);
    }

    public ListTabPageIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        // setHorizontalScrollBarEnabled(false);

        mInflater = LayoutInflater.from(context);

        // mTabLayout = new LinearLayout(getContext());
        // addView(mTabLayout, new
        // ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
        // ViewGroup.LayoutParams.FILL_PARENT));
    }

    /*
     * @Override public void onMeasure(int widthMeasureSpec, int
     * heightMeasureSpec) { final int widthMode =
     * MeasureSpec.getMode(widthMeasureSpec); final boolean lockedExpanded =
     * widthMode == MeasureSpec.EXACTLY; //setFillViewport(lockedExpanded);
     * final int childCount = getChildCount(); if (childCount > 1 && (widthMode
     * == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST)) { if
     * (childCount > 2) { // TweetLanes: Change max width so names don't get cut
     * off //mMaxTabWidth = (int)(MeasureSpec.getSize(widthMeasureSpec) * 0.4f);
     * mMaxTabWidth = (int)(MeasureSpec.getSize(widthMeasureSpec) * 0.5f); }
     * else { mMaxTabWidth = MeasureSpec.getSize(widthMeasureSpec) / 2; } } else
     * { mMaxTabWidth = -1; } final int oldWidth = getMeasuredWidth();
     * super.onMeasure(widthMeasureSpec, heightMeasureSpec); final int newWidth
     * = getMeasuredWidth(); if (lockedExpanded && oldWidth != newWidth) { //
     * Recenter the tab display if we're at a new (scrollable) size.
     * setCurrentItem(mSelectedTabIndex); } }
     */

    /*
     * private void animateToTab(final int position) { //final View tabView =
     * mTabLayout.getChildAt(position); if (mTabSelector != null) {
     * removeCallbacks(mTabSelector); } mTabSelector = new Runnable() { public
     * void run() { //final int scrollPos = tabView.getLeft() - (getWidth() -
     * tabView.getWidth()) / 2; //smoothScrollTo(scrollPos, 0);
     * setSelection(position); // TODO: Animate mTabSelector = null; } };
     * post(mTabSelector); }
     */

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mTabSelector != null) {
            // Re-post the selector we saved
            post(mTabSelector);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mTabSelector != null) {
            removeCallbacks(mTabSelector);
        }
    }

    /*
     * private void addTab(String text, int index) { //Workaround for not being
     * able to pass a defStyle on pre-3.0 final TabView tabView =
     * (TabView)mInflater.inflate(R.layout.vpi_listtab, null);
     * tabView.init(this, text, index); tabView.setFocusable(true);
     * tabView.setOnClickListener(mTabClickListener);
     * //mTabLayout.addView(tabView, new LinearLayout.LayoutParams(0,
     * LayoutParams.FILL_PARENT, 1)); this.addView(tabView, index); }
     */

    @Override
    public void onPageScrollStateChanged(int arg0) {
        if (mListener != null) {
            mListener.onPageScrollStateChanged(arg0);
        }
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
        if (mListener != null) {
            mListener.onPageScrolled(arg0, arg1, arg2);
        }
    }

    @Override
    public void onPageSelected(int position) {
        setCurrentItem(position);
        if (mListener != null) {
            mListener.onPageSelected(position);
        }
    }

    @Override
    public void setViewPager(ViewPager view) {
        final PagerAdapter adapter = view.getAdapter();
        if (adapter == null) {
            throw new IllegalStateException(
                    "ViewPager does not have adapter instance.");
        }
        if (!(adapter instanceof TitleProvider)) {
            throw new IllegalStateException(
                    "ViewPager adapter must implement TitleProvider to be used with TitlePageIndicator.");
        }
        mViewPager = view;
        view.setOnPageChangeListener(this);
        notifyDataSetChanged();
    }

    public void notifyDataSetChanged() {
        // removeAllViews();

        ArrayList<String> names = new ArrayList<String>();
        TitleProvider adapter = (TitleProvider) mViewPager.getAdapter();
        final int count = ((PagerAdapter) adapter).getCount();
        for (int i = 0; i < count; i++) {
            CharSequence cs = adapter.getTitle(i);
            if (cs != null) {
                String name = cs.toString();
                names.add(name.toUpperCase());
            }
        }

        if (names.size() > 0) {
            String[] array = names.toArray(new String[names.size()]);
            ListArrayAdapter listAdapter = new ListArrayAdapter(mContext, array);

            // mListAdapter.notify();

            setAdapter(listAdapter);

            /*
             * TitleProvider adapter = (TitleProvider)mViewPager.getAdapter();
             * final int count = ((PagerAdapter)adapter).getCount(); for (int i
             * = 0; i < count; i++) { addTab(adapter.getTitle(i), i);
             * //addView(child, i); } if (mSelectedTabIndex > count) {
             * mSelectedTabIndex = count - 1; }
             */
            setCurrentItem(mSelectedTabIndex);
            requestLayout();
        }
    }

    @Override
    public void setViewPager(ViewPager view, int initialPosition) {
        setViewPager(view);
        setCurrentItem(initialPosition);
    }

    @Override
    public void setCurrentItem(int item) {
        if (mViewPager == null) {
            throw new IllegalStateException("ViewPager has not been bound.");
        }
        mSelectedTabIndex = item;
        final int tabCount = getChildCount();
        for (int i = 0; i < tabCount; i++) {
            final View child = getChildAt(i);
            final boolean isSelected = (i == item);
            child.setSelected(isSelected);
            if (isSelected) {
                // animateToTab(item);
            }
        }
    }

    @Override
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        mListener = listener;
    }

    /*
     * 
     */
    public static class TabView extends LinearLayout {

        // private ListTabPageIndicator mParent;
        private int mIndex;

        public TabView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public void init(String text, int index) {
            // mParent = parent;
            mIndex = index;

            TextView textView = (TextView) findViewById(android.R.id.text1);
            textView.setText(text);
        }

        /*
         * @Override public void onMeasure(int widthMeasureSpec, int
         * heightMeasureSpec) { super.onMeasure(widthMeasureSpec,
         * heightMeasureSpec); // Re-measure if we went beyond our maximum size.
         * if (mParent.mMaxTabWidth > 0 && getMeasuredWidth() >
         * mParent.mMaxTabWidth) {
         * super.onMeasure(MeasureSpec.makeMeasureSpec(mParent.mMaxTabWidth,
         * MeasureSpec.EXACTLY), heightMeasureSpec); } }
         */

        public int getIndex() {
            return mIndex;
        }
    }

    /*
     *
	 */
    public class ListArrayAdapter extends ArrayAdapter<String> {

        // private final Context context;
        private final String[] values;

        public ListArrayAdapter(Context context, String[] values) {
            super(context, R.layout.vpi_listtab, values);
            // this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // LayoutInflater inflater = (LayoutInflater)
            // context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            String text = values[position];

            final TabView tabView = (TabView) mInflater.inflate(
                    R.layout.vpi_listtab, null);
            tabView.init(text, position);
            tabView.setFocusable(true);
            tabView.setOnClickListener(mTabClickListener);

            return tabView;
            // View rowView = inflater.inflate(R.layout.vpi_listtab, parent,
            // false);
            // TextView textView = (TextView) rowView.findViewById(R.id.label);
            // ImageView imageView = (ImageView)
            // rowView.findViewById(R.id.logo);
            // textView.setText(values[position]);
            // return rowView;
        }
    }

}
