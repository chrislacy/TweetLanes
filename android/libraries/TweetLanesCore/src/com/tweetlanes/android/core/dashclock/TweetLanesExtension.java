package com.tweetlanes.android.core.dashclock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.tweetlanes.android.core.App;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.SharedPreferencesConstants;
import com.tweetlanes.android.core.model.AccountDescriptor;
import com.tweetlanes.android.core.view.HomeActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.socialnetlib.android.SocialNetConstant;

import java.util.ArrayList;

public class TweetLanesExtension extends DashClockExtension {

    @Override
    protected void onInitialize(boolean isReconnect) {
        setUpdateWhenScreenOn(true);
    }

    @Override
    protected void onUpdateData(int arg0) {
        // Get preference value.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        int mentionCount = 0;
        int dmCount = 0;
        String body = "";
        String accountKey = null;
        for (AccountDescriptor account : getAccounts(this)) {
            int accountMentionCount = sp.getInt(SharedPreferencesConstants.NOTIFICATION_COUNT + account.getAccountKey() + SharedPreferencesConstants.NOTIFICATION_TYPE_MENTION, 0);
            int accountDmCount = sp.getInt(SharedPreferencesConstants.NOTIFICATION_COUNT + account.getAccountKey() + SharedPreferencesConstants.NOTIFICATION_TYPE_DIRECT_MESSAGE, 0);

            if (accountMentionCount > 0) {
                mentionCount += accountMentionCount;
                body += (sp.getString(SharedPreferencesConstants.NOTIFICATION_SUMMARY + account.getAccountKey() + SharedPreferencesConstants.NOTIFICATION_TYPE_MENTION, "") + "\n");
            }

            if (accountDmCount > 0) {
                dmCount += accountDmCount;
                body += (sp.getString(SharedPreferencesConstants.NOTIFICATION_SUMMARY + account.getAccountKey() + SharedPreferencesConstants.NOTIFICATION_TYPE_DIRECT_MESSAGE, "") + "\n");
            }

            if (accountKey == null) {
                accountKey = account.getAccountKey();
            }
        }
        body = body.replaceAll("\\s+$", "");

        // Publish the extension data update.
        if (mentionCount > 0 || dmCount > 0) {
            String title = mentionCount > 0 ? mentionCount + " new mentions" : "";
            if (title.length() == 0) {
                title = dmCount + " new direct messages";
            } else if (dmCount > 0) {
                title += ", " + dmCount + " new direct mentions";
            }
            publishUpdate(new ExtensionData().visible(true).icon(R.drawable.ic_launcher).status(String.valueOf(mentionCount + dmCount))
                    .expandedTitle(title).expandedBody(body).clickIntent(getHomeIntent(accountKey, mentionCount > 0 ? SharedPreferencesConstants.NOTIFICATION_TYPE_MENTION : SharedPreferencesConstants.NOTIFICATION_TYPE_DIRECT_MESSAGE)));
        } else {
            publishUpdate(null);
        }
    }

    private Intent getHomeIntent(String accountKey, String type) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        long postId;

        if (type.equals(SharedPreferencesConstants.NOTIFICATION_TYPE_MENTION)) {
            postId = preferences.getLong(SharedPreferencesConstants.NOTIFICATION_LAST_DISPLAYED_MENTION_ID + accountKey, 0);
        } else {
            postId = preferences.getLong(SharedPreferencesConstants.NOTIFICATION_LAST_DISPLAYED_DIRECT_MESSAGE_ID + accountKey, 0);
        }

        Intent intent = new Intent(this, HomeActivity.class);
        intent.putExtra("account_key", accountKey);
        intent.putExtra("notification_post_id", postId);
        intent.putExtra("notification_type", type);

        return intent;
    }

    private static ArrayList<AccountDescriptor> getAccounts(Context context) {
        final ArrayList<AccountDescriptor> accounts = new ArrayList<AccountDescriptor>();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String accountIndices = preferences.getString(SharedPreferencesConstants.ACCOUNT_INDICES, null);

        if (accountIndices != null) {
            try {
                JSONArray jsonArray = new JSONArray(accountIndices);
                for (int i = 0; i < jsonArray.length(); i++) {
                    Long id = jsonArray.getLong(i);

                    String key = App.getAccountDescriptorKey(id);
                    String jsonAsString = preferences.getString(key, null);
                    if (jsonAsString != null) {
                        AccountDescriptor account = new AccountDescriptor(context, jsonAsString);
                        if (Constant.ENABLE_APP_DOT_NET == false
                                && account.getSocialNetType() == SocialNetConstant.Type.Appdotnet) {
                            continue;
                        }
                        accounts.add(account);
                    }
                }

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return accounts;
    }
}