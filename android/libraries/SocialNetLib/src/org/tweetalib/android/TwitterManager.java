/*
 * Copyright (C) 2013 Chris Lacy
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.tweetalib.android;

import org.socialnetlib.android.AppdotnetApi;
import org.socialnetlib.android.SocialNetApi;
import org.socialnetlib.android.SocialNetConstant;
import org.socialnetlib.android.TwitterApi;
import org.tweetalib.android.callback.TwitterFetchDirectMessagesFinishedCallback;
import org.tweetalib.android.callback.TwitterFetchStatusesFinishedCallback;
import org.tweetalib.android.fetch.TwitterFetchStatuses;
import org.tweetalib.android.model.TwitterDirectMessages;
import org.tweetalib.android.model.TwitterLists;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatusUpdate;
import org.tweetalib.android.model.TwitterStatuses;
import org.tweetalib.android.model.TwitterUser;
import org.tweetalib.android.model.TwitterUsers;

import java.util.ArrayList;
import java.util.HashMap;

import twitter4j.auth.RequestToken;

/*
 *
 */
public class TwitterManager {

    // http://android-developers.blogspot.com/2010/07/how-to-have-your-cupcake-and-eat-it-too.html

    // / TODO: This is probably too C++ ish. Will come back to this later...
    public static void initModule(SocialNetConstant.Type socNetType,
                                  String consumerKey, String consumerSecret, String oAuthToken,
                                  String oAuthSecret, String currentAccountKey,
                                  ConnectionStatus.Callbacks connectionStatusCallbacks) {
        mInstance = new TwitterManager(socNetType, consumerKey, consumerSecret, currentAccountKey);
        mInstance.setOAuthTokenWithSecret(oAuthToken, oAuthSecret, true);
        mInstance.setConnectionStatus(connectionStatusCallbacks);
    }

    public static void initModule(TwitterManager instance) {
        mInstance = instance;
    }

    public static void deinitModule() {
        mInstance = null;
    }

    public static TwitterManager get() {
        return mInstance;
    }

    private static TwitterManager mInstance = null;

    private SocialNetApi mApi;

    /*
     *
	 */
    private TwitterManager(SocialNetConstant.Type socialNetType, String consumerKey,
                           String consumerSecret, String currentAccountKey) {
        setSocialNetType(socialNetType, consumerKey, consumerSecret, currentAccountKey);
    }

    public SocialNetConstant.Type getSocialNetType() {
        return mApi.getSocialNetType();
    }

    public void setSocialNetType(SocialNetConstant.Type socialNetType,
                                 String consumerKey, String consumerSecret, String currentAccountKey) {
        switch (socialNetType) {
            case Appdotnet:
                mApi = new AppdotnetApi(socialNetType, consumerKey, consumerSecret, currentAccountKey);
                break;

            default:
                mApi = new TwitterApi(socialNetType, consumerKey, consumerSecret, currentAccountKey);
                break;
        }
    }


    /*
     *
	 */
    public void setOAuthTokenWithSecret(String oAuthToken, String oAuthSecret,
                                        boolean cancelPending) {
        mApi.setOAuthTokenWithSecret(oAuthToken, oAuthSecret, cancelPending);
    }

    /*
	 *
	 */
    void setConnectionStatus(
            ConnectionStatus.Callbacks connectionStatusCallbacks) {
        mApi.setConnectionStatus(connectionStatusCallbacks);
    }

    /*
	 *
	 */
    public ConnectionStatus getConnectionStatus() {
        return mApi.getConnectionStatus();
    }

    /*
	 *
	 */
    public TwitterContentHandle getContentHandle(
            TwitterContentHandleBase contentHandleBase, String screenName,
            String identifier, String currentAccountKey) {

        return new TwitterContentHandle(
                contentHandleBase, screenName, identifier, currentAccountKey);
    }

    /*
	 *
	 */
    public enum ProfileImageSize {
        MINI, // 24x24
        NORMAL, // 48x48
        BIGGER, // 73x73
        ORIGINAL, // undefined. This will be the size the image was originally
        // uploaded in.
        // The filesize of original images can be very big so use this
        // parameter with
        // caution.
    }

    /*
	 *
	 */
    public TwitterStatuses getContentFeed(TwitterContentHandle handle) {
        return mApi.getContentFeed(handle);
    }

    public TwitterStatuses setContentFeed(TwitterContentHandle handle, TwitterStatuses newStatuses) {
        return mApi.setContentFeed(handle, newStatuses);
    }

    public void removeFromHashMap(TwitterStatuses statuses) {
        mApi.removeFromHashMap(statuses);
    }

    public void removeFromDirectMessageHashMap(TwitterDirectMessages mesages) {
        mApi.removeFromDirectMessageHashMap(mesages);
    }

    /*
     * TODO: This is pretty hacky, just so the callback can be instantiated
     * outside the class
     */
    public TwitterFetchLists getFetchListsInstance() {
        return mApi.getFetchListsInstance();
    }

    public TwitterFetchStatus getFetchStatusInstance() {
        return mApi.getFetchStatusInstance();
    }

    public TwitterFetchStatuses getFetchStatusesInstance() {
        return mApi.getFetchStatusesInstance();
    }

    public TwitterFetchBooleans getFetchBooleansInstance() {
        return mApi.getFetchBooleansInstance();
    }

    public TwitterFetchUser getFetchUserInstance() {
        return mApi.getFetchUserInstance();
    }

    public TwitterFetchUsers getFetchUsersInstance() {
        return mApi.getFetchUsersInstance();
    }

    public TwitterModifyStatuses getSetStatusesInstance() {
        return mApi.getSetStatusesInstance();
    }

    public TwitterModifyDirectMessages getSetDirectMessagesInstance() {
        return mApi.getSetDirectMessagesInstance();
    }

    public TwitterSignIn getSignInInstance() {
        return mApi.getSignInInstance();
    }

    public void setSignInSocialNetType(String consumerKey, String consumerSecret, SocialNetConstant.Type type) {
        mApi.initSignIn(consumerKey, consumerSecret, type);
    }

    /*
	 *
	 */
    public void getAuthUrl(TwitterSignIn.GetAuthUrlCallback callback) {
        mApi.getAuthUrl(callback);
    }

    public void getOAuthAccessToken(RequestToken requestToken,
                                    String oauthVerifier,
                                    TwitterSignIn.GetOAuthAccessTokenCallback callback) {
        mApi.getOAuthAccessToken(requestToken, oauthVerifier, callback);
    }

    /*
     * Will be null if no cached entry exists
     */
    public TwitterUser getUser(Long userId) {
        return getUser(userId, null);
    }

    public TwitterUser getUser(String screenName) {
        return getUser(screenName, null);
    }

    public TwitterUser getUser(Long userId,
                               TwitterFetchUser.FinishedCallback callback) {
        return mApi.getUser(userId, callback);
    }

    public TwitterUser getUser(String screenName,
                               TwitterFetchUser.FinishedCallback callback) {
        return mApi.getUser(screenName, callback);
    }

    /*
	 *
	 */
    public TwitterUsers getUsers(TwitterContentHandle contentHandle,
                                 TwitterPaging paging) {
        return mApi.getUsers(contentHandle, paging);
    }

    public TwitterUsers getUsers(TwitterContentHandle contentHandle,
                                 TwitterPaging paging, TwitterFetchUsers.FinishedCallback callback) {
        return mApi.getUsers(contentHandle, paging, callback);
    }

    /*
	 *
	 */
    public TwitterDirectMessages getDirectMessages(
            TwitterContentHandle contentHandle) {
        return mApi
                .getDirectMessages(contentHandle);
    }

    public TwitterDirectMessages setDirectMessages(
            TwitterContentHandle contentHandle, TwitterDirectMessages newMessages) {
        return mApi
                .setDirectMessages(contentHandle, newMessages);
    }

    public TwitterDirectMessages getDirectMessages(
            TwitterContentHandle contentHandle, TwitterPaging paging,
            TwitterFetchDirectMessagesFinishedCallback callback) {
        return mApi.getDirectMessages(
                contentHandle, paging, callback);
    }

    public void sendDirectMessage(long userId, String recipientScreenName,
                                  String statusText, TwitterContentHandle contentHandle,
                                  TwitterFetchDirectMessagesFinishedCallback callback) {
        mApi.sendDirectMessage(userId, recipientScreenName, statusText,
                contentHandle, callback);
    }

    /*
	 *
	 */
    public void updateFriendship(String currentUserScreenName,
                                 TwitterUser userToUpdate, boolean create,
                                 TwitterFetchUsers.FinishedCallback callback) {
        mApi.updateFriendship(currentUserScreenName, userToUpdate, create,
                callback);
    }

    public void updateFriendshipUserIds(long currentUserId,
                                        ArrayList<Long> userIdsToUpdate, boolean create,
                                        TwitterFetchUsers.FinishedCallback callback) {
        mApi.updateFriendshipUserIds(currentUserId, userIdsToUpdate, create,
                callback);
    }

    public void createBlock(long currentUserId, ArrayList<Long> userIds,
                            TwitterFetchUsers.FinishedCallback callback) {
        mApi.createBlock(currentUserId, userIds, callback);
    }


    public void reportSpam(long currentUserId, ArrayList<Long> userIds,
                           TwitterFetchUsers.FinishedCallback callback) {
        mApi.reportSpam(currentUserId, userIds, callback);
    }


    public TwitterLists getLists(String screenName,
                                 TwitterFetchLists.FinishedCallback callback) {
        return mApi.getLists(screenName, callback);
    }

    /*
	 *
	 */
    public TwitterStatus getStatus(long statusId,
                                   TwitterFetchStatus.FinishedCallback callback) {
        return mApi.getStatus(statusId, callback);
    }

    /*
	 *
	 */
    public void setStatus(TwitterStatusUpdate statusUpdate,
                          TwitterFetchStatus.FinishedCallback callback) {
        mApi.setStatus(statusUpdate, callback);
    }

    /*
	 *
	 */
    public void setRetweet(long statusId,
                           TwitterFetchStatus.FinishedCallback callback) {
        mApi.setRetweet(statusId, callback);
    }

    /*
	 *
	 */
    public void setFavorite(TwitterStatus status, boolean isFavorite,
                            TwitterModifyStatuses.FinishedCallback callback) {
        mApi.setFavorite(status, isFavorite, callback);
    }

    public void setFavorite(TwitterStatuses statuses, boolean isFavorite,
                            TwitterModifyStatuses.FinishedCallback callback) {
        mApi.setFavorite(statuses, isFavorite, callback);
    }

    public void deleteTweet(TwitterStatuses statuses, TwitterModifyStatuses.FinishedCallback callback) {
        mApi.deleteTweet(statuses, callback);
    }

    public void deleteDirectMessage(TwitterDirectMessages messages, TwitterModifyDirectMessages.FinishedCallback callback) {
        mApi.deleteDirectMessage(messages, callback);
    }


    /*
	 *
	 */
    public void triggerFetchStatuses(TwitterContentHandle contentHandle,
                                     TwitterPaging paging,
                                     TwitterFetchStatusesFinishedCallback callback, int priorityOffset) {
        mApi.triggerFetchStatuses(contentHandle, paging, callback,
                priorityOffset);
    }

    /*
	 *
	 */
    public void getFriendshipExists(String userScreenName,
                                    String userScreenNameToCheck,
                                    TwitterFetchBooleans.FinishedCallback callback) {
        mApi.getFriendshipExists(userScreenName, userScreenNameToCheck,
                callback);
    }


    public boolean hasValidTwitterInstance() {
        return mApi != null;
    }

    /*
     * Used for https://dev.twitter.com/docs/auth/oauth/oauth-echo
     */
    public String generateTwitterVerifyCredentialsAuthorizationHeader() {
        if (mApi instanceof TwitterApi) {
            return ((TwitterApi) mApi)
                    .generateTwitterVerifyCredentialsAuthorizationHeader();
        }

        return null;
    }

    /*
     * Quick and dirty system to handle mapping an appdotnet username in an
     * entity to an id
     */
    private static final HashMap<String, Long> mUserIdentifierHashMap = new HashMap<String, Long>();

    public static void addUserIdentifier(String username, long id) {
        if (!mUserIdentifierHashMap.containsKey(username)) {
            mUserIdentifierHashMap.put(username, id);
        }
    }

    public static Long getUserIdFromScreenName(String username) {
        if (mUserIdentifierHashMap.containsKey(username)) {
            return mUserIdentifierHashMap.get(username);
        }
        return null;
    }

}
