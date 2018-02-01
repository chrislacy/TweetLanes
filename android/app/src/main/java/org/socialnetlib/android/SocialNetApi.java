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

package org.socialnetlib.android;

import org.appdotnet4j.model.AdnUser;
import org.socialnetlib.android.SocialNetConstant.Type;
import org.tweetalib.android.ConnectionStatus;
import org.tweetalib.android.TwitterContentHandle;
import org.tweetalib.android.TwitterFetchBooleans;
import org.tweetalib.android.TwitterFetchBooleans.FetchBooleansWorkerCallbacks;
import org.tweetalib.android.TwitterFetchLists;
import org.tweetalib.android.TwitterFetchLists.FetchListsWorkerCallbacks;
import org.tweetalib.android.TwitterFetchStatus;
import org.tweetalib.android.TwitterFetchStatus.FetchStatusWorkerCallbacks;
import org.tweetalib.android.TwitterFetchUser;
import org.tweetalib.android.TwitterFetchUser.FetchUserWorkerCallbacks;
import org.tweetalib.android.TwitterFetchUsers;
import org.tweetalib.android.TwitterFetchUsers.FetchUsersWorkerCallbacks;
import org.tweetalib.android.TwitterModifyDirectMessages;
import org.tweetalib.android.TwitterModifyDirectMessages.ModifyDirectMessagesWorkerCallbacks;
import org.tweetalib.android.TwitterModifyStatuses;
import org.tweetalib.android.TwitterModifyStatuses.ModifyStatusesWorkerCallbacks;
import org.tweetalib.android.TwitterPaging;
import org.tweetalib.android.TwitterSignIn;
import org.tweetalib.android.TwitterSignIn.SignInWorkerCallbacks;
import org.tweetalib.android.callback.TwitterFetchDirectMessagesFinishedCallback;
import org.tweetalib.android.callback.TwitterFetchStatusesFinishedCallback;
import org.tweetalib.android.fetch.TwitterFetchDirectMessages;
import org.tweetalib.android.fetch.TwitterFetchDirectMessages.FetchMessagesWorkerCallbacks;
import org.tweetalib.android.fetch.TwitterFetchStatuses;
import org.tweetalib.android.fetch.TwitterFetchStatuses.FetchStatusesWorkerCallbacks;
import org.tweetalib.android.model.TwitterDirectMessages;
import org.tweetalib.android.model.TwitterLists;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatusUpdate;
import org.tweetalib.android.model.TwitterStatuses;
import org.tweetalib.android.model.TwitterUser;
import org.tweetalib.android.model.TwitterUsers;

import java.util.ArrayList;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.auth.RequestToken;

public abstract class SocialNetApi {

    protected abstract void init();

    protected abstract TwitterUser verifyCredentialsSync(String oAuthToken,
                                                         String oAuthSecret);

    abstract Twitter getAndConfigureApiInstance();

    abstract void clearApiInstance();

    private final SocialNetConstant.Type mType;
    String mCurrentOAuthToken;
    String mCurrentOAuthSecret;
    final String mAppConsumerKey;
    final String mAppConsumerSecret;
    private final String mCurrentAccountKey;

    private TwitterFetchBooleans mFetchBooleans;
    private TwitterFetchDirectMessages mFetchDirectMessages;
    private TwitterFetchStatus mFetchStatus;
    private TwitterFetchStatuses mFetchStatuses;
    private TwitterFetchUser mFetchUser;
    private TwitterFetchUsers mFetchUsers;
    private TwitterFetchLists mFetchLists;
    private TwitterModifyStatuses mModifyStatuses;
    private TwitterModifyDirectMessages mModifyDirectMessages;
    private TwitterSignIn mSignIn;
    private ConnectionStatus mConnectionStatus;

    SocialNetApi(SocialNetConstant.Type type, String consumerKey,
                 String consumerSecret, String currentAccountKey) {

        mType = type;
        mAppConsumerKey = consumerKey;
        mAppConsumerSecret = consumerSecret;
        mCurrentAccountKey = currentAccountKey;

        init();

        initFetchBooleans();
        initFetchDirectMessages();
        initFetchStatus();
        initFetchStatuses();
        initFetchUser();
        initFetchUsers();
        initFetchLists();
        initModifyStatuses();
        initModifyDirectMessages();
        if (mSignIn == null) {
            mSignIn = new TwitterSignIn();
            initSignIn(consumerKey, consumerSecret, type);
        }
    }

    /*
     *
	 */
    private void initFetchStatus() {

        mFetchStatus = new TwitterFetchStatus();

        FetchStatusWorkerCallbacks callbacks = new FetchStatusWorkerCallbacks() {

            public Twitter getTwitterInstance() {
                return SocialNetApi.this.getAndConfigureApiInstance();
            }

            @Override
            public void addUser(User user) {
                cacheUser(user);
            }

            @Override
            public AppdotnetApi getAppdotnetApi() {
                return SocialNetApi.this.getAppdotnetApi();
            }
        };

        mFetchStatus.setWorkerCallbacks(callbacks);
    }

    /*
	 *
	 */
    private void initFetchStatuses() {

        mFetchStatuses = new TwitterFetchStatuses();

        FetchStatusesWorkerCallbacks callbacks = new FetchStatusesWorkerCallbacks() {

            public Twitter getTwitterInstance() {
                return SocialNetApi.this.getAndConfigureApiInstance();
            }

            @Override
            public void addUser(User user) {
                cacheUser(user);
            }

            @Override
            public void addUser(AdnUser user) {
                cacheUser(user);
            }

            @Override
            public AppdotnetApi getAppdotnetApi() {
                return SocialNetApi.this.getAppdotnetApi();
            }

        };

        mFetchStatuses.setWorkerCallbacks(callbacks);
    }

    /*
	 *
	 */
    private void initFetchBooleans() {
        mFetchBooleans = new TwitterFetchBooleans();

        FetchBooleansWorkerCallbacks callbacks = new FetchBooleansWorkerCallbacks() {

            public Twitter getTwitterInstance() {
                return SocialNetApi.this.getAndConfigureApiInstance();
            }

            public AppdotnetApi getAppdotnetInstance() {
                return SocialNetApi.this.getAppdotnetApi();
            }
        };

        mFetchBooleans.setWorkerCallbacks(callbacks);
    }

    /*
	 *
	 */
    private void initFetchDirectMessages() {

        mFetchDirectMessages = new TwitterFetchDirectMessages();

        FetchMessagesWorkerCallbacks callbacks = new FetchMessagesWorkerCallbacks() {

            public Twitter getTwitterInstance() {
                return SocialNetApi.this.getAndConfigureApiInstance();
            }

            @Override
            public void addUser(User user) {
                cacheUser(user);
            }
        };

        mFetchDirectMessages.setWorkerCallbacks(callbacks);
    }

    /*
	 *
	 */
    public void initSignIn(final String consumerKey, final String consumerSecret, final SocialNetConstant.Type type) {
        mSignIn = new TwitterSignIn();

        SignInWorkerCallbacks callbacks = new SignInWorkerCallbacks() {

            @Override
            public String getConsumerKey() {
                return consumerKey;
            }

            @Override
            public String getConsumerSecret() {
                return consumerSecret;
            }

            @Override
            public TwitterUser verifyCredentials(String accessToken,
                                                 String accessTokenSecret) {
                return verifyCredentialsSync(accessToken, accessTokenSecret);
            }

            @Override
            public Type getType() {
                return type;
            }

        };

        mSignIn.setWorkerCallbacks(callbacks);
    }

    /*
	 *
	 */
    private void initFetchUser() {
        mFetchUser = new TwitterFetchUser();

        FetchUserWorkerCallbacks callbacks = new FetchUserWorkerCallbacks() {

            @Override
            public Twitter getTwitterInstance() {
                return SocialNetApi.this.getAndConfigureApiInstance();
            }

            @Override
            public AppdotnetApi getAppdotnetApi() {
                return SocialNetApi.this.getAppdotnetApi();
            }

        };

        mFetchUser.setWorkerCallbacks(callbacks);
    }

    AppdotnetApi getAppdotnetApi() {

        if (mType == SocialNetConstant.Type.Appdotnet) {
            return (AppdotnetApi) this;
        }

        return null;
    }

    /*
	 *
	 */
    private void initFetchUsers() {
        mFetchUsers = new TwitterFetchUsers();

        FetchUsersWorkerCallbacks callbacks = new FetchUsersWorkerCallbacks() {

            @Override
            public Twitter getTwitterInstance() {
                return SocialNetApi.this.getAndConfigureApiInstance();
            }

            @Override
            public AppdotnetApi getAppdotnetInstance() {
                return SocialNetApi.this.getAppdotnetApi();
            }

            @Override
            public String getCurrentAccountKey() {
                return mCurrentAccountKey;
            }

            @Override
            public void addUser(User user) {
                cacheUser(user);
            }

            @Override
            public void addUser(AdnUser user) {
                cacheUser(user);
            }

            @Override
            public TwitterUser getUser(Long userID) {
                return mFetchUser.getUser(userID, null, mConnectionStatus);
            }
        };

        mFetchUsers.setWorkerCallbacks(callbacks);
    }

    /*
	 *
	 */
    private void initFetchLists() {
        mFetchLists = new TwitterFetchLists();

        FetchListsWorkerCallbacks callbacks = new FetchListsWorkerCallbacks() {

            @Override
            public Twitter getTwitterInstance() {
                return SocialNetApi.this.getAndConfigureApiInstance();
            }

        };

        mFetchLists.setWorkerCallbacks(callbacks);
    }

    /*
	 *
	 */
    private void initModifyStatuses() {
        mModifyStatuses = new TwitterModifyStatuses();

        ModifyStatusesWorkerCallbacks callbacks = new ModifyStatusesWorkerCallbacks() {

            @Override
            public Twitter getTwitterInstance() {
                return SocialNetApi.this.getAndConfigureApiInstance();
            }

            @Override
            public AppdotnetApi getAppdotnetApi() {
                return SocialNetApi.this.getAppdotnetApi();
            }

        };

        mModifyStatuses.setWorkerCallbacks(callbacks);
    }

    private void initModifyDirectMessages() {
        mModifyDirectMessages = new TwitterModifyDirectMessages();

        ModifyDirectMessagesWorkerCallbacks callbacks = new ModifyDirectMessagesWorkerCallbacks() {

            @Override
            public Twitter getTwitterInstance() {
                return SocialNetApi.this.getAndConfigureApiInstance();
            }
        };

        mModifyDirectMessages.setWorkerCallbacks(callbacks);
    }

    /*
	 *
	 */
    public boolean isAuthenticated() {
        Twitter twitter = getAndConfigureApiInstance();
        if (twitter == null) {
            return false;
        }

        try {
            twitter.getAccountSettings();
            return true;
        } catch (TwitterException e) {
            return false;
        }
    }

    /*
	 *
	 */
    private void cacheUser(User user) {
        cacheUser(user, false);
    }

    /*
	 *
	 */
    private void cacheUser(User user, boolean forceUpdate) {
        if (user != null) {
            mFetchUser.setUser(user, forceUpdate);
        }
    }

    /*
	 *
	 */
    private void cacheUser(AdnUser user) {
        cacheUser(user, false);
    }

    /*
	 *
	 */
    private void cacheUser(AdnUser user, boolean forceUpdate) {
        if (user != null) {
            mFetchUser.setUser(user, forceUpdate);
        }
    }

    /*
	 *
	 */
    public void setOAuthTokenWithSecret(String oAuthToken, String oAuthSecret,
                                        boolean cancelPending) {

        if (oAuthToken == null && mCurrentOAuthToken == null) {
            return;
        } else if (oAuthToken != null && mCurrentOAuthToken != null
                && oAuthToken.equals(mCurrentOAuthToken)) {
            return;
        } else if (oAuthSecret != null && mCurrentOAuthSecret != null
                && oAuthSecret.equals(mCurrentOAuthSecret)) {
            return;
        }

        if (cancelPending) {
            mFetchBooleans.clearCallbacks();
            mFetchLists.clearCallbacks();
            mFetchDirectMessages.clearCallbacks();
            mFetchStatus.clearCallbacks();
            mFetchStatuses.clearCallbacks();
            mFetchUser.clearCallbacks();
            mFetchUsers.clearCallbacks();
            mModifyStatuses.clearCallbacks();
        }

        mCurrentOAuthToken = oAuthToken;
        mCurrentOAuthSecret = oAuthSecret;

        clearApiInstance();
        getAndConfigureApiInstance();
    }

    /*
	 *
	 */
    public void setConnectionStatus(
            ConnectionStatus.Callbacks connectionStatusCallbacks) {
        mConnectionStatus = new ConnectionStatus(connectionStatusCallbacks);
    }

    /*
	 *
	 */
    public ConnectionStatus getConnectionStatus() {
        return mConnectionStatus;
    }

    public TwitterStatuses getContentFeed(TwitterContentHandle handle) {
        return mFetchStatuses.getStatuses(handle);
    }

    public TwitterStatuses setContentFeed(TwitterContentHandle handle, TwitterStatuses newStatuses) {
        return mFetchStatuses.setStatuses(handle, newStatuses, false);
    }

    public void removeFromHashMap(TwitterStatuses statuses) {
        mFetchStatuses.removeFromHashMap(statuses);
    }

    public void removeFromDirectMessageHashMap(TwitterDirectMessages mesages) {
        mFetchDirectMessages.removeFromDirectMessageHashMap(mesages);
    }

    /*
	 *
	 */
    public void getAuthUrl(TwitterSignIn.GetAuthUrlCallback callback) {
        mSignIn.getAuthUrl(callback);
    }

    public void getOAuthAccessToken(RequestToken requestToken,
                                    String oauthVerifier,
                                    TwitterSignIn.GetOAuthAccessTokenCallback callback) {
        mSignIn.getOAuthAccessToken(requestToken, oauthVerifier, callback);
    }

    /*
     * Will be null if no cached entry exists
     */
    public TwitterUser getUser(Long userId) {
        return getUser(userId, null);
    }

    public TwitterUser getUser(Long userId,
                               TwitterFetchUser.FinishedCallback callback) {
        return mFetchUser.getUser(userId, callback,
                mConnectionStatus);
    }

    public TwitterUser getUser(String screenName,
                               TwitterFetchUser.FinishedCallback callback) {
        return mFetchUser.getUser(screenName, callback,
                mConnectionStatus);
    }

    public void verifyUser(TwitterFetchUser.FinishedCallback callback) {
        mFetchUser.verifyUser(callback, mConnectionStatus);
    }

    /*
	 *
	 */
    public TwitterUsers getUsers(TwitterContentHandle contentHandle,
                                 TwitterPaging paging) {
        return mFetchUsers.getUsers(contentHandle, paging);
    }

    public TwitterUsers getUsers(TwitterContentHandle contentHandle,
                                 TwitterPaging paging, TwitterFetchUsers.FinishedCallback callback) {
        return mFetchUsers.getUsers(contentHandle, paging,
                callback, mConnectionStatus);
    }

    /*
	 *
	 */
    public TwitterDirectMessages getDirectMessages(
            TwitterContentHandle contentHandle) {
        return mFetchDirectMessages
                .getDirectMessages(contentHandle);
    }

    public TwitterDirectMessages setDirectMessages(
            TwitterContentHandle contentHandle, TwitterDirectMessages messages) {
        return mFetchDirectMessages
                .setDirectMessages(contentHandle, messages);
    }

    public TwitterDirectMessages getDirectMessages(
            TwitterContentHandle contentHandle, TwitterPaging paging,
            TwitterFetchDirectMessagesFinishedCallback callback) {
        return mFetchDirectMessages
                .getDirectMessages(contentHandle, paging, callback,
                        mConnectionStatus);
    }

    public void sendDirectMessage(long userId, String recipientScreenName,
                                  String statusText,
                                  TwitterContentHandle contentHandle,
                                  TwitterFetchDirectMessagesFinishedCallback callback) {
        mFetchDirectMessages.sendDirectMessage(userId, recipientScreenName,
                statusText, contentHandle, callback, mConnectionStatus);
    }

    /*
	 *
	 */
    public void updateFriendship(String currentUserScreenName,
                                 TwitterUser userToUpdate, boolean create,
                                 TwitterFetchUsers.FinishedCallback callback) {
        mFetchUsers.updateFriendshipUser(currentUserScreenName, userToUpdate,
                create, callback, mConnectionStatus);
    }

    public void updateFriendship(String currentUserScreenName,
                                 TwitterUsers usersToUpdate, boolean create,
                                 TwitterFetchUsers.FinishedCallback callback) {
        mFetchUsers.updateFriendshipUsers(currentUserScreenName, usersToUpdate,
                create, callback, mConnectionStatus);
    }

    public void updateFriendshipScreenName(String currentUserScreenName,
                                           String screenNameToUpdate, boolean create,
                                           TwitterFetchUsers.FinishedCallback callback) {
        mFetchUsers.updateFriendshipScreenName(currentUserScreenName,
                screenNameToUpdate, create, callback, mConnectionStatus);
    }

    public void updateFriendshipScreenNames(String currentUserScreenName,
                                            ArrayList<String> screenNamesToUpdate, boolean create,
                                            TwitterFetchUsers.FinishedCallback callback) {
        mFetchUsers.updateFriendshipScreenNames(currentUserScreenName,
                screenNamesToUpdate, create, callback, mConnectionStatus);
    }

    public void updateFriendshipUserId(long currentUserId, long userIdToUpdate,
                                       boolean create, TwitterFetchUsers.FinishedCallback callback) {
        mFetchUsers.updateFriendshipUserId(currentUserId, userIdToUpdate,
                create, callback, mConnectionStatus);
    }

    public void updateFriendshipUserIds(long currentUserId,
                                        ArrayList<Long> userIdsToUpdate, boolean create,
                                        TwitterFetchUsers.FinishedCallback callback) {
        mFetchUsers.updateFriendshipUserIds(currentUserId, userIdsToUpdate,
                create, callback, mConnectionStatus);
    }

    /*
	 *
	 */
    public void createBlock(long currentUserId, Long userId,
                            TwitterFetchUsers.FinishedCallback callback) {
        mFetchUsers.createBlock(currentUserId, userId, callback,
                mConnectionStatus);
    }

    public void createBlock(long currentUserId, ArrayList<Long> userIds,
                            TwitterFetchUsers.FinishedCallback callback) {
        mFetchUsers.createBlock(currentUserId, userIds, callback,
                mConnectionStatus);
    }

    /*
	 *
	 */
    public void reportSpam(long currentUserId, Long userId,
                           TwitterFetchUsers.FinishedCallback callback) {
        mFetchUsers.reportSpam(currentUserId, userId, callback,
                mConnectionStatus);
    }

    public void reportSpam(long currentUserId, ArrayList<Long> userIds,
                           TwitterFetchUsers.FinishedCallback callback) {
        mFetchUsers.reportSpam(currentUserId, userIds, callback,
                mConnectionStatus);
    }

    public TwitterLists getLists(int userId,
                                 TwitterFetchLists.FinishedCallback callback) {
        return mFetchLists.getLists(userId, callback);
    }

    public TwitterLists getLists(String screenName,
                                 TwitterFetchLists.FinishedCallback callback) {
        return mFetchLists.getLists(screenName, callback);
    }

    /*
	 *
	 */
    public TwitterStatus getStatus(long statusId,
                                   TwitterFetchStatus.FinishedCallback callback) {
        return mFetchStatus.getStatus(statusId, callback, mConnectionStatus);
    }

    /*
	 *
	 */
    public void setStatus(TwitterStatusUpdate statusUpdate,
                          TwitterFetchStatus.FinishedCallback callback) {
        mFetchStatus.setStatus(statusUpdate, callback, mConnectionStatus);
    }

    /*
	 *
	 */
    public void setRetweet(long statusId,
                           TwitterFetchStatus.FinishedCallback callback) {
        mFetchStatus.setRetweet(statusId, callback, mConnectionStatus);
    }

    /*
	 *
	 */
    public void setFavorite(TwitterStatus status, boolean isFavorite,
                            TwitterModifyStatuses.FinishedCallback callback) {
        mModifyStatuses.setFavorite(status, isFavorite, callback);
    }

    public void setFavorite(TwitterStatuses statuses, boolean isFavorite,
                            TwitterModifyStatuses.FinishedCallback callback) {
        mModifyStatuses.setFavorite(statuses, isFavorite, callback);
    }

    public void deleteTweet(TwitterStatuses statuses, TwitterModifyStatuses.FinishedCallback callback) {
        mModifyStatuses.deleteTweets(statuses, callback);
    }

    public void deleteDirectMessage(TwitterDirectMessages messages, TwitterModifyDirectMessages.FinishedCallback callback) {
        mModifyDirectMessages.deleteDirectMessages(messages, callback);
    }


    /*
	 *
	 */
    public void triggerFetchStatuses(TwitterContentHandle contentHandle,
                                     TwitterPaging paging,
                                     TwitterFetchStatusesFinishedCallback callback, int priorityOffset) {
        mFetchStatuses.trigger(contentHandle, paging, callback,
                mConnectionStatus, priorityOffset);
    }

    /*
	 *
	 */
    public void cancelFetchStatuses(
            TwitterFetchStatusesFinishedCallback callback) {
        mFetchStatuses.cancel(callback);
    }

    /*
	 *
	 */
    public void getFriendshipExists(String userScreenName,
                                    String userScreenNameToCheck,
                                    TwitterFetchBooleans.FinishedCallback callback) {
        mFetchBooleans.getFriendshipExists(userScreenName,
                userScreenNameToCheck, callback, mConnectionStatus);
    }

    public abstract SocialNetConstant.Type getSocialNetType();

    public TwitterFetchLists getFetchListsInstance() {
        return mFetchLists;
    }

    public TwitterFetchStatus getFetchStatusInstance() {
        return mFetchStatus;
    }

    public TwitterFetchStatuses getFetchStatusesInstance() {
        return mFetchStatuses;
    }

    public TwitterFetchBooleans getFetchBooleansInstance() {
        return mFetchBooleans;
    }

    public TwitterFetchUser getFetchUserInstance() {
        return mFetchUser;
    }

    public TwitterFetchUsers getFetchUsersInstance() {
        return mFetchUsers;
    }

    public TwitterModifyStatuses getSetStatusesInstance() {
        return mModifyStatuses;
    }

    public TwitterModifyDirectMessages getSetDirectMessagesInstance() {
        return mModifyDirectMessages;
    }

    public TwitterSignIn getSignInInstance() {
        return mSignIn;
    }
}
