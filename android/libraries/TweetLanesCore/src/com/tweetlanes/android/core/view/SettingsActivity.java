/*
 * Copyright (C) 2013 Chris Lacy Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.tweetlanes.android.core.view;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.crittercism.app.Crittercism;
import com.inscription.ChangeLogDialog;
import com.tweetlanes.android.core.App;
import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.Constant.LaneType;
import com.tweetlanes.android.core.ConsumerKeyConstants;
import com.tweetlanes.android.core.Notifier;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.model.LaneDescriptor;

import java.util.ArrayList;

public class SettingsActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    public static final String KEY_THEME_PREFERENCE = "theme_preference";
    public static final String KEY_CUSTOMIZE_LANES_PREFERENCE = "customizelanes_preference";
    public static final String KEY_SHOW_TABLET_MARGIN_PREFERENCE = "showtabletmargin_preference";
    public static final String KEY_STATUS_SIZE_PREFERENCE = "statussize_preference";
    public static final String KEY_PROFILE_IMAGE_SIZE_PREFERENCE = "profileimagesize_preference";
    public static final String KEY_VOLSCROLL_PREFERENCE = "volscroll_preference";
    public static final String KEY_DOWNLOADIMAGES_PREFERENCE = "downloadimages_preference";
    public static final String KEY_SHOW_TWEET_SOURCE_PREFERENCE = "showtweetsource_preference";
    public static final String KEY_QUOTE_TYPE_PREFERENCE = "quotetype_preference";
    private static final String KEY_CREDITS_PREFERENCE = "preference_credits";
    private static final String KEY_SOURCE_CODE_PREFERENCE = "preference_source";
    private static final String KEY_DONATE_PREFERENCE = "preference_donate";
    private static final String KEY_VERSION_PREFERENCE = "version_preference";
    public static final String KEY_RINGTONE_PREFERENCE = "ringtone_preference";
    public static final String KEY_NOTIFICATION_TIME_PREFERENCE = "notificationtime_preference";
    public static final String KEY_NOTIFICATION_TYPE_PREFERENCE = "notificationtype_preference";
    public static final String KEY_NOTIFICATION_VIBRATION = "notificationvibration_preference";

    private ListPreference mThemePreference;
    private Preference mCustomizeLanesPreference;
    private CheckBoxPreference mShowTabletMarginPreference;
    private ListPreference mStatusSizePreference;
    private ListPreference mProfileImageSizePreference;
    private CheckBoxPreference mDownloadImagesPreference;
    private CheckBoxPreference mShowTweetSourcePreference;
    private ListPreference mQuoteTypePreference;
    private CheckBoxPreference mVolScrollPreference;
    private Preference mCreditsPreference;
    private Preference mSourceCodePreference;
    private Preference mDonatePreference;
    private Preference mVersionPreference;
    private ListPreference mNotificationTimePreference;
    private ListPreference mNotificationTypePreference;

    /*
     *
	 */
    App getApp() {
        return (App) getApplication();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.preference.PreferenceActivity#onCreate(android.os.Bundle)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (Constant.ENABLE_CRASH_TRACKING) {
            Crittercism.init(getApplicationContext(),
                    ConsumerKeyConstants.CRITTERCISM_APP_ID);
        }

        setTheme(AppSettings.get().getCurrentThemeStyle());

        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        PreferenceCategory category = (PreferenceCategory) findPreference("category_display");

        mThemePreference = (ListPreference) getPreferenceScreen()
                .findPreference(KEY_THEME_PREFERENCE);
        mShowTabletMarginPreference = (CheckBoxPreference) getPreferenceScreen()
                .findPreference(KEY_SHOW_TABLET_MARGIN_PREFERENCE);

        final Resources res = getResources();
        float value = res.getDimension(R.dimen.lane_content_width);
        if (value == 0F) {
            category.removePreference(mShowTabletMarginPreference);
            mShowTabletMarginPreference = null;
        }

        mStatusSizePreference = (ListPreference) getPreferenceScreen()
                .findPreference(KEY_STATUS_SIZE_PREFERENCE);
        mProfileImageSizePreference = (ListPreference) getPreferenceScreen()
                .findPreference(KEY_PROFILE_IMAGE_SIZE_PREFERENCE);
        mDownloadImagesPreference = (CheckBoxPreference) getPreferenceScreen()
                .findPreference(KEY_DOWNLOADIMAGES_PREFERENCE);
        mCustomizeLanesPreference = getPreferenceScreen()
                .findPreference(KEY_CUSTOMIZE_LANES_PREFERENCE);
        mCustomizeLanesPreference
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {

                        ArrayList<LaneDescriptor> laneDefinitions = getApp()
                                .getCurrentAccount().getAllLaneDefinitions();
                        LaneCustomizationAdapter adapter = new LaneCustomizationAdapter(SettingsActivity.this, laneDefinitions);
                        ListView listView = new ListView(SettingsActivity.this);
                        listView.setAdapter(adapter);

                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                                SettingsActivity.this);
                        alertDialogBuilder
                                .setTitle(R.string.alert_customize_lanes_title)
                                .setView(listView)
                                .setCancelable(false)
                                .setPositiveButton(R.string.done,
                                        new DialogInterface.OnClickListener() {

                                            public void onClick(
                                                    DialogInterface dialog,
                                                    int id) {
                                                AppSettings
                                                        .get()
                                                        .refresh(
                                                                KEY_CUSTOMIZE_LANES_PREFERENCE);
                                            }
                                        });
                        alertDialogBuilder.create().show();
                        return true;
                    }
                });

        mShowTweetSourcePreference = (CheckBoxPreference) getPreferenceScreen()
                .findPreference(KEY_SHOW_TWEET_SOURCE_PREFERENCE);

        mVolScrollPreference = (CheckBoxPreference) getPreferenceScreen()
                .findPreference(KEY_VOLSCROLL_PREFERENCE);

        mQuoteTypePreference = (ListPreference) getPreferenceScreen()
                .findPreference(KEY_QUOTE_TYPE_PREFERENCE);
        mCreditsPreference = getPreferenceScreen().findPreference(
                KEY_CREDITS_PREFERENCE);
        mSourceCodePreference = getPreferenceScreen().findPreference(
                KEY_SOURCE_CODE_PREFERENCE);
        mDonatePreference = getPreferenceScreen().findPreference(
                KEY_DONATE_PREFERENCE);
        mVersionPreference = getPreferenceScreen().findPreference(
                KEY_VERSION_PREFERENCE);
        mNotificationTimePreference = (ListPreference) getPreferenceScreen()
                .findPreference(KEY_NOTIFICATION_TIME_PREFERENCE);
        mNotificationTypePreference = (ListPreference) getPreferenceScreen()
                .findPreference(KEY_NOTIFICATION_TYPE_PREFERENCE);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onResume()
     */
    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        if (mThemePreference.getEntry() == null) {
            mThemePreference.setValueIndex(0);
        }
        mThemePreference.setSummary(mThemePreference.getEntry());

        if (mShowTabletMarginPreference != null) {
            boolean showTabletMargin = sharedPreferences.getBoolean(
                    KEY_SHOW_TABLET_MARGIN_PREFERENCE,
                    AppSettings.DEFAULT_SHOW_TABLET_MARGIN);
            mShowTabletMarginPreference.setChecked(showTabletMargin);
        }

        if (mStatusSizePreference.getEntry() == null) {
            mStatusSizePreference.setValueIndex(2);
        }
        mStatusSizePreference.setSummary(mStatusSizePreference.getEntry());

        if (mProfileImageSizePreference.getEntry() == null) {
            mProfileImageSizePreference.setValueIndex(1);
        }
        mProfileImageSizePreference.setSummary(mProfileImageSizePreference
                .getEntry());

        boolean showTweetSource = sharedPreferences.getBoolean(
                KEY_SHOW_TWEET_SOURCE_PREFERENCE,
                AppSettings.DEFAULT_SHOW_TWEET_SOURCE);
        mShowTweetSourcePreference.setChecked(showTweetSource);

        boolean downloadImages = sharedPreferences.getBoolean(
                KEY_DOWNLOADIMAGES_PREFERENCE,
                AppSettings.DEFAULT_DOWNLOAD_IMAGES);
        mDownloadImagesPreference.setChecked(downloadImages);

        boolean volScroll = sharedPreferences.getBoolean(
                KEY_VOLSCROLL_PREFERENCE, AppSettings.DEFAULT_VOLSCROLL);
        mVolScrollPreference.setChecked(volScroll);

        if (mQuoteTypePreference.getEntry() == null) {
            mQuoteTypePreference.setValueIndex(0);
        }
        mQuoteTypePreference.setSummary(mQuoteTypePreference.getEntry());

        mSourceCodePreference
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent browserIntent = new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/chrislacy/TweetLanes"));
                        startActivity(browserIntent);
                        return true;
                    }
                });

        mDonatePreference
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://www.tweetlanes.com/donate"));
                        startActivity(browserIntent);
                        return true;
                    }
                });

        mCreditsPreference
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    @SuppressLint("SetJavaScriptEnabled")
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        final View layout = View.inflate(SettingsActivity.this,
                                R.layout.credits, null);

                        TextView ossTextView = (TextView) layout
                                .findViewById(R.id.credits_oss_items);
                        ossTextView.setMovementMethod(LinkMovementMethod
                                .getInstance());

                        TextView artTextView = (TextView) layout
                                .findViewById(R.id.credits_art_items);
                        artTextView.setMovementMethod(LinkMovementMethod
                                .getInstance());

                        TextView lacyNetworksTextView = (TextView) layout
                                .findViewById(R.id.credit_lacy_networks);
                        lacyNetworksTextView.setMovementMethod(LinkMovementMethod
                                .getInstance());

                        TextView fammyNetworksTextView = (TextView) layout
                                .findViewById(R.id.credit_fammy_networks);
                        fammyNetworksTextView.setMovementMethod(LinkMovementMethod
                                .getInstance());

                        TextView duffyNetworksTextView = (TextView) layout
                                .findViewById(R.id.credit_duffy_networks);
                        duffyNetworksTextView.setMovementMethod(LinkMovementMethod
                                .getInstance());

                        TextView blythmeisterNetworksTextView = (TextView) layout
                                .findViewById(R.id.credit_blythmeister_networks);
                        blythmeisterNetworksTextView.setMovementMethod(LinkMovementMethod
                                .getInstance());

                        AlertDialog.Builder builder = new AlertDialog.Builder(
                                SettingsActivity.this);
                        builder.setIcon(0);
                        builder.setNegativeButton(R.string.ok, null);
                        builder.setView(layout);
                        builder.setTitle(R.string.settings_credits_title);

                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                        return true;
                    }
                });

        mVersionPreference
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    @SuppressLint("SetJavaScriptEnabled")
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        ChangeLogDialog _ChangelogDialog = new ChangeLogDialog(SettingsActivity.this);
                        _ChangelogDialog.show();
                        return true;
                    }
                });

        mVersionPreference.setSummary(App.getAppVersionName());

        if (mNotificationTimePreference.getEntry() == null) {
            mNotificationTimePreference.setValueIndex(1);
        }
        mNotificationTimePreference.setSummary(mNotificationTimePreference.getEntry());

        if (mNotificationTypePreference.getEntry() == null) {
            mNotificationTypePreference.setValueIndex(0);
        }
        mNotificationTypePreference.setSummary(mNotificationTypePreference.getEntry());


        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#
     * onSharedPreferenceChanged(android.content.SharedPreferences,
     * java.lang.String)
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        Preference pref = findPreference(key);

        AppSettings.get().refresh(key);

        if (pref instanceof ListPreference) {
            ListPreference listPref = (ListPreference) pref;
            pref.setSummary(listPref.getEntry());

            if (listPref == mThemePreference) {
                // Restart the activity
                finish();
                getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                overridePendingTransition(0, 0);
                startActivity(getIntent());
            } else if (listPref == mNotificationTimePreference) {
                //Stop and start notifications (with new time)
                Notifier.setNotificationAlarm(this);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (super.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return false;
        }
    }

    /*
	 *
	 */
    public class LaneCustomizationAdapter extends ArrayAdapter<LaneDescriptor> {

        private final Context mContext;
        private final ArrayList<LaneDescriptor> mLaneDefinitions;

        public LaneCustomizationAdapter(Context context,
                                        ArrayList<LaneDescriptor> laneDefinitions) {
            super(context, R.layout.lane_customization_item, laneDefinitions);
            mContext = context;
            mLaneDefinitions = laneDefinitions;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final LaneDescriptor lane = mLaneDefinitions.get(position);
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.lane_customization_item,
                    parent, false);

            final CheckBox checkbox = (CheckBox) view
                    .findViewById(R.id.checkbox);
            if (lane.getLaneType() == LaneType.USER_HOME_TIMELINE) {
                checkbox.setChecked(true);
                checkbox.setEnabled(false);
            } else {
                checkbox.setChecked(lane.getDisplay());
                checkbox.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        lane.setDisplay(checkbox.isChecked());
                        getApp().getCurrentAccount()
                                .setDisplayedLaneDefinitionsDirty(true);
                    }

                });
            }

            TextView textView = (TextView) view.findViewById(R.id.laneTitle);
            textView.setText(lane.getLaneTitle());

            if (lane.getLaneType() != LaneType.USER_HOME_TIMELINE) {
                OnClickListener onClickListener = new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        checkbox.setChecked(!checkbox.isChecked());
                        lane.setDisplay(checkbox.isChecked());
                        getApp().getCurrentAccount()
                                .setDisplayedLaneDefinitionsDirty(true);
                    }

                };

                textView.setOnClickListener(onClickListener);
                view.setOnClickListener(onClickListener);
            }

            return view;
        }
    }
}
