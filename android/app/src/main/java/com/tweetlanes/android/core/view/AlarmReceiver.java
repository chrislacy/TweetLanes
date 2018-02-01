package com.tweetlanes.android.core.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.tweetlanes.android.core.App;
import com.tweetlanes.android.core.AppSettings;
import com.tweetlanes.android.core.Constant;
import com.tweetlanes.android.core.ConsumerKeyConstants;
import com.tweetlanes.android.core.Notifier;
import com.tweetlanes.android.core.SharedPreferencesConstants;
import com.tweetlanes.android.core.model.AccountDescriptor;

import org.json.JSONArray;
import org.json.JSONException;
import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.ConnectionStatus;
import org.tweetalib.android.TwitterConstant;
import org.tweetalib.android.TwitterContentHandle;
import org.tweetalib.android.TwitterContentHandleBase;
import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.TwitterPaging;
import org.tweetalib.android.callback.TwitterFetchDirectMessagesFinishedCallback;
import org.tweetalib.android.callback.TwitterFetchStatusesFinishedCallback;
import org.tweetalib.android.model.TwitterDirectMessage;
import org.tweetalib.android.model.TwitterDirectMessages;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatuses;

import java.util.ArrayList;

public class AlarmReceiver extends BroadcastReceiver {

    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            mContext = context;
            checkForNewNotifications();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final TwitterFetchStatusesFinishedCallback getMentionsCallback = new TwitterFetchStatusesFinishedCallback() {
        @Override
        public void finished(TwitterFetchResult result, TwitterStatuses feed, TwitterContentHandle contentHandle) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            long lastDisplayedMentionId = preferences.getLong(SharedPreferencesConstants.NOTIFICATION_LAST_DISPLAYED_MENTION_ID +
                    contentHandle.getCurrentAccountKey(), 0);

            if (feed != null && feed.getStatusCount() > 0) {
                int notificationId = (contentHandle.getCurrentAccountKey() + SharedPreferencesConstants.NOTIFICATION_TYPE_MENTION).hashCode();
                String name = contentHandle.getScreenName();
                int count = feed.getStatusCount();

                TwitterStatus first = feed.getStatus(0);

                String fullDetail = "";
                if (first.mId > lastDisplayedMentionId) {

                    JSONArray statusArray = new JSONArray();
                    int statusCount = feed.getStatusCount();
                    for (int i = 0; i < statusCount; ++i) {
                        TwitterStatus status = feed.getStatus(i);
                        statusArray.put(status.toString());
                    }

                    final SharedPreferences.Editor edit = preferences.edit();
                    edit.putString("mentions_" + contentHandle.getCurrentAccountKey(), statusArray.toString());
                    edit.commit();

                    String noun = feed.getStatusCount() == 1 ? "mention" : "mentions";

                    String detail = feed.getStatusCount() == 1 ? "@" + first.getAuthorScreenName() + ": " + first.mStatus
                            : "@" + name + " has " + count + " new " + noun;

                    for (int i = 0; i < feed.getStatusCount(); ++i) {
                        TwitterStatus status = feed.getStatus(i);
                        fullDetail += status.mStatus + "\n";
                    }
                    fullDetail = fullDetail.substring(0, fullDetail.length() - 1);

                    Notifier.notify("@" + name + ": " + count + " new " + noun, detail, fullDetail, true, notificationId,
                            contentHandle.getCurrentAccountKey(), SharedPreferencesConstants.NOTIFICATION_TYPE_MENTION, first.mId, mContext);

                    Notifier.setDashclockValues(mContext, contentHandle.getCurrentAccountKey(), SharedPreferencesConstants.NOTIFICATION_TYPE_MENTION, count, fullDetail);
                } else {
                    Notifier.setDashclockValues(mContext, contentHandle.getCurrentAccountKey(), SharedPreferencesConstants.NOTIFICATION_TYPE_MENTION, 0, "");
                }
            } else {
                Notifier.setDashclockValues(mContext, contentHandle.getCurrentAccountKey(), SharedPreferencesConstants.NOTIFICATION_TYPE_MENTION, 0, "");
            }
        }
    };

    private final TwitterFetchDirectMessagesFinishedCallback getDirectMessagesCallback = new TwitterFetchDirectMessagesFinishedCallback() {
        @Override
        public void finished(TwitterContentHandle contentHandle, TwitterFetchResult result, TwitterDirectMessages messages) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);

            long lastDisplayedId = preferences.getLong(SharedPreferencesConstants.NOTIFICATION_LAST_DISPLAYED_DIRECT_MESSAGE_ID +
                    contentHandle.getCurrentAccountKey(), 0);

            ArrayList<TwitterDirectMessage> received = messages != null ? messages.getRawReceivedMessages() : null;

            if (received != null && received.size() > 0) {
                int notificationId = (contentHandle.getCurrentAccountKey() + SharedPreferencesConstants.NOTIFICATION_TYPE_DIRECT_MESSAGE).hashCode();
                String name = contentHandle.getScreenName();

                TwitterDirectMessage first = received.get(0);

                String fullDetail = "";
                int count = 0;
                if (first.getId() > lastDisplayedId) {

                    JSONArray statusArray = new JSONArray();
                    int statusCount = received.size();
                    for (int i = 0; i < statusCount; ++i) {
                        TwitterDirectMessage status = received.get(i);
                        statusArray.put(status.toString());
                    }

                    final SharedPreferences.Editor edit = preferences.edit();
                    edit.putString("dm_" + contentHandle.getCurrentAccountKey(), statusArray.toString());
                    edit.commit();

                    String noun = received.size() == 1 ? "direct message" : "direct messages";

                    for (int i = 0; i < received.size(); ++i) {
                        TwitterDirectMessage status = received.get(i);
                        if (status.getOtherUserScreenName().equals(contentHandle.getScreenName())) {
                            continue;
                        }

                        fullDetail += "@" + status.getOtherUserScreenName() + ": " + status.getText() + "\n";
                        count++;
                    }

                    if (count == 0) {
                        return;
                    }

                    String detail = count == 1 ? "@" + first.getOtherUserScreenName() + ": " + first.getText()
                            : "@" + name + " has " + count + " new " + noun;

                    fullDetail = fullDetail.substring(0, fullDetail.length() - 1);

                    String detailNoun = received.size() == 1 ? "DM" : "DMs";

                    Notifier.notify("@" + name + ": " + count + " new " + detailNoun, detail, fullDetail, true, notificationId,
                            contentHandle.getCurrentAccountKey(), SharedPreferencesConstants.NOTIFICATION_TYPE_DIRECT_MESSAGE, first.getId(), mContext);

                    Notifier.setDashclockValues(mContext, contentHandle.getCurrentAccountKey(), SharedPreferencesConstants.NOTIFICATION_TYPE_DIRECT_MESSAGE, count, fullDetail);
                } else {
                    Notifier.setDashclockValues(mContext, contentHandle.getCurrentAccountKey(), SharedPreferencesConstants.NOTIFICATION_TYPE_DIRECT_MESSAGE, 0, "");
                }
            } else {
                Notifier.setDashclockValues(mContext, contentHandle.getCurrentAccountKey(), SharedPreferencesConstants.NOTIFICATION_TYPE_DIRECT_MESSAGE, 0, "");
            }
        }
    };

    private void checkForNewNotifications() {
        TwitterManager manager = TwitterManager.get();

        for (AccountDescriptor account : getAccounts(mContext)) {
            initSocialNetLib(account.getSocialNetType(), account.getAccountKey(), account.getOAuthToken(),
                    account.getOAuthSecret());

            if (AppSettings.get().isShowNotificationsEnabled()) {
                String notificationTypes = AppSettings.get().getNotificationType();
                if (notificationTypes.contains("m")) {
                    checkForMentions(account);
                }
                if (notificationTypes.contains("d") && account.getSocialNetType() == SocialNetConstant.Type.Twitter) {
                    checkForDirectMessages(account);
                }
            }
        }

        if (manager != null) {
            TwitterManager.initModule(manager);
        }
    }

    private void checkForDirectMessages(AccountDescriptor account) {
        TwitterContentHandleBase base = new TwitterContentHandleBase(
                TwitterConstant.ContentType.DIRECT_MESSAGES,
                TwitterConstant.DirectMessagesType.RECIEVED_MESSAGES);
        TwitterContentHandle contentHandle = new TwitterContentHandle(base, account.getScreenName(),
                Long.valueOf(account.getId()).toString(), account.getAccountKey());

        TwitterPaging paging;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        long lastActionedId = preferences.getLong(SharedPreferencesConstants.NOTIFICATION_LAST_ACTIONED_DIRECT_MESSAGE_ID +
                account.getAccountKey(), 0);

        if (lastActionedId == 0) {
            paging = TwitterPaging.createGetMostRecent();
        } else {
            paging = TwitterPaging.createGetNewer(lastActionedId);
        }

        TwitterManager.get().getDirectMessages(contentHandle, paging, getDirectMessagesCallback);
    }

    private void checkForMentions(AccountDescriptor account) {
        TwitterContentHandleBase base = new TwitterContentHandleBase(
                TwitterConstant.ContentType.STATUSES,
                TwitterConstant.StatusesType.USER_MENTIONS);
        TwitterContentHandle contentHandle = new TwitterContentHandle(base, account.getScreenName(),
                Long.valueOf(account.getId()).toString(), account.getAccountKey());

        TwitterPaging paging;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        long lastActionMentionId = preferences.getLong(SharedPreferencesConstants.NOTIFICATION_LAST_ACTIONED_MENTION_ID +
                account.getAccountKey(), 0);

        if (lastActionMentionId == 0) {
            paging = TwitterPaging.createGetMostRecent();
        } else {
            paging = TwitterPaging.createGetNewer(lastActionMentionId);
        }

        TwitterManager.get().triggerFetchStatuses(contentHandle, paging, getMentionsCallback, 1);
    }

    private void initSocialNetLib(SocialNetConstant.Type socialNetType, String accountKey, String authToken,
                                  String authSecret) {
        TwitterManager.initModule(socialNetType,
                socialNetType == SocialNetConstant.Type.Twitter ? ConsumerKeyConstants.TWITTER_CONSUMER_KEY : ConsumerKeyConstants.APPDOTNET_CONSUMER_KEY,
                socialNetType == SocialNetConstant.Type.Twitter ? ConsumerKeyConstants.TWITTER_CONSUMER_SECRET : ConsumerKeyConstants.APPDOTNET_CONSUMER_SECRET,
                authToken,
                authSecret,
                accountKey,
                mConnectionStatusCallbacks);
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

    private final ConnectionStatus.Callbacks mConnectionStatusCallbacks = new ConnectionStatus.Callbacks() {
        @Override
        public boolean isOnline() {
            return true;
        }

        @Override
        public String getErrorMessageNoConnection() {
            return "No connection";
        }

        @Override
        public void handleError(TwitterFetchResult fetchResult) {
        }
    };
}
