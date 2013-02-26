/*
 * Copyright (C) 2013 Chris Lacy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tweetalib.android;

import java.util.ArrayList;
import java.util.HashMap;

import org.socialnetlib.android.AppdotnetApi;
import org.socialnetlib.android.SocialNetApi;
import org.socialnetlib.android.SocialNetConstant;
import org.socialnetlib.android.TwitterApi;
import twitter4j.auth.RequestToken;

import org.tweetalib.android.callback.TwitterFetchDirectMessagesFinishedCallback;
import org.tweetalib.android.callback.TwitterFetchStatusesFinishedCallback;
import org.tweetalib.android.model.TwitterLists;
import org.tweetalib.android.model.TwitterDirectMessages;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatusUpdate;
import org.tweetalib.android.model.TwitterStatuses;
import org.tweetalib.android.model.TwitterUser;
import org.tweetalib.android.model.TwitterUsers;


/*
 * 
 */
public class TwitterManager {

	//http://android-developers.blogspot.com/2010/07/how-to-have-your-cupcake-and-eat-it-too.html
	
	/// TODO: This is probably too C++ ish. Will come back to this later...
	public static void initModule(SocialNetConstant.Type socNetType, String consumerKey, String consumerSecret, String oAuthToken, String oAuthSecret, ConnectionStatus.Callbacks connectionStatusCallbacks) { 
		mInstance = new TwitterManager(socNetType, consumerKey, consumerSecret); 
		mInstance.setOAuthTokenWithSecret(oAuthToken, oAuthSecret, false);
		mInstance.setConnectionStatus(connectionStatusCallbacks);
	}
	public static void deinitModule() 	{ mInstance = null;	}
	public static TwitterManager get() { return mInstance; }
	private static TwitterManager mInstance = null;
	
	private SocialNetApi mApi;
	
	/*
	 * 
	 */
	TwitterManager(SocialNetConstant.Type socialNetType, String consumerKey, String consumerSecret) {
		setSocialNetType(socialNetType, consumerKey, consumerSecret);
	}
	
	public SocialNetConstant.Type getSocialNetType() {
		return mApi.getSocialNetType();
	}

	public void setSocialNetType(SocialNetConstant.Type socialNetType, String consumerKey, String consumerSecret) {
		switch (socialNetType) {
		case Appdotnet:
			mApi = new AppdotnetApi(socialNetType, consumerKey, consumerSecret);
			break;
			
		default:
			mApi = new TwitterApi(socialNetType, consumerKey, consumerSecret);
			break;
		}
	}
	
	
	/*
	 * 
	 */
	public void setOAuthTokenWithSecret(String oAuthToken, String oAuthSecret, boolean cancelPending) {
			mApi.setOAuthTokenWithSecret(oAuthToken, oAuthSecret, cancelPending);
	}
	
	/*
	 * 
	 */
	public void setConnectionStatus(ConnectionStatus.Callbacks connectionStatusCallbacks) {
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
	public TwitterContentHandle getContentHandle(TwitterContentHandleBase contentHandleBase, String screenName,
			String identifier) {

		TwitterContentHandle handle = new TwitterContentHandle(contentHandleBase, screenName, identifier);
		
		return handle;
	}
	
	/*
	 * 
	 */
	public enum ProfileImageSize {
		MINI,		// 24x24
		NORMAL,		// 48x48
		BIGGER,		// 73x73
		ORIGINAL,	// undefined. This will be the size the image was originally uploaded in. 
					// The filesize of original images can be very big so use this parameter with caution.
	};
	
	public String getProfileImageUrl(String screenName, ProfileImageSize size) {
		if (getSocialNetType() == SocialNetConstant.Type.Appdotnet) {
			String w = "";
			switch (size) {
			case MINI:
				w = "?w=32";
				break;
			case NORMAL:
				w = "?w=48";
				break;
			case BIGGER:
				w = "?w=73";
				break;
			}
			return "https://alpha-api.app.net/stream/0/users/@" + screenName + "/avatar" + w;
		}
		else {
			return "https://api.twitter.com/1/users/profile_image/" + screenName + "?size=" + size.toString().toLowerCase();
		}
	}
	
	/*
	 * 
	 */
	public TwitterStatuses getContentFeed(TwitterContentHandle handle) {
		return mApi.getContentFeed(handle);
	}
	
	/*
	 * TODO: This is pretty hacky, just so the callback can be instantiated outside the class
	 */
	public TwitterFetchLists 	getFetchListsInstance() 	{ return mApi.getFetchListsInstance(); }
	public TwitterFetchStatus 	getFetchStatusInstance() 	{ return mApi.getFetchStatusInstance(); }
	public TwitterFetchBooleans	getFetchBooleansInstance()	{ return mApi.getFetchBooleansInstance(); }
	public TwitterFetchUser 	getFetchUserInstance() 		{ return mApi.getFetchUserInstance(); }
	public TwitterFetchUsers	getFetchUsersInstance() 	{ return mApi.getFetchUsersInstance(); }
	public TwitterModifyStatuses	getSetStatusesInstance()	{ return mApi.getSetStatusesInstance(); }
	public TwitterSignIn 		getSignInInstance() 		{ return mApi.getSignInInstance(); }
	
	
	
	
	/*
	 * 
	 */
	public void getAuthUrl(TwitterSignIn.GetAuthUrlCallback callback) {
		mApi.getAuthUrl(callback);
	}
	public void getOAuthAccessToken(RequestToken requestToken, String oauthVerifier, TwitterSignIn.GetOAuthAccessTokenCallback callback) {
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
	public TwitterUser getUser(Long userId, TwitterFetchUser.FinishedCallback callback) {
		TwitterUser cachedUser = mApi.getUser(userId, callback);
		return cachedUser;
	}
	public TwitterUser getUser(String screenName, TwitterFetchUser.FinishedCallback callback) {
		TwitterUser cachedUser = mApi.getUser(screenName, callback);
		return cachedUser;
	}
	
	public void verifyUser(TwitterFetchUser.FinishedCallback callback) {
		mApi.verifyUser(callback);
	}
	
	/*
	 * 
	 */
	public TwitterUsers getUsers(TwitterContentHandle contentHandle, TwitterPaging paging) {
		return mApi.getUsers(contentHandle, paging);
	}
	public TwitterUsers getUsers(TwitterContentHandle contentHandle, TwitterPaging paging, TwitterFetchUsers.FinishedCallback callback) {
		return mApi.getUsers(contentHandle, paging, callback);
	}
	
	/*
	 * 
	 */
	public TwitterDirectMessages getDirectMessages(TwitterContentHandle contentHandle) {
		TwitterDirectMessages cachedMessages = mApi.getDirectMessages(contentHandle);
		return cachedMessages;
	}
	public TwitterDirectMessages getDirectMessages(TwitterContentHandle contentHandle, TwitterPaging paging, TwitterFetchDirectMessagesFinishedCallback callback) {
		TwitterDirectMessages cachedMessages = mApi.getDirectMessages(contentHandle, paging, callback);
		return cachedMessages;
	}
	
	public void sendDirectMessage(long userId, String recipientScreenName, String statusText, TwitterFetchDirectMessagesFinishedCallback callback) {
		mApi.sendDirectMessage(userId, recipientScreenName, statusText, callback);
	}
	
	/*
	 * 
	 */
	public void updateFriendship(String currentUserScreenName, TwitterUser userToUpdate, boolean create, TwitterFetchUsers.FinishedCallback callback) {
		mApi.updateFriendship(currentUserScreenName, userToUpdate, create, callback);
	}
	public void updateFriendship(String currentUserScreenName, TwitterUsers usersToUpdate, boolean create, TwitterFetchUsers.FinishedCallback callback) {
		mApi.updateFriendship(currentUserScreenName, usersToUpdate, create, callback);
	}
	public void updateFriendshipScreenName(String currentUserScreenName, String screenNameToUpdate, boolean create, TwitterFetchUsers.FinishedCallback callback) {
		mApi.updateFriendshipScreenName(currentUserScreenName, screenNameToUpdate, create, callback);
	}
	public void updateFriendshipScreenNames(String currentUserScreenName, ArrayList<String> screenNamesToUpdate, boolean create, TwitterFetchUsers.FinishedCallback callback) {
		mApi.updateFriendshipScreenNames(currentUserScreenName, screenNamesToUpdate, create, callback);
	}
	public void updateFriendshipUserId(long currentUserId, long userIdToUpdate, boolean create, TwitterFetchUsers.FinishedCallback callback) {
		mApi.updateFriendshipUserId(currentUserId, userIdToUpdate, create, callback);
	}
	public void updateFriendshipUserIds(long currentUserId, ArrayList<Long> userIdsToUpdate, boolean create, TwitterFetchUsers.FinishedCallback callback) {
		mApi.updateFriendshipUserIds(currentUserId, userIdsToUpdate, create, callback);
	}
	
	/*
	 * 
	 */
	public void createBlock(long currentUserId, Long userId, TwitterFetchUsers.FinishedCallback callback) {
		mApi.createBlock(currentUserId, userId, callback);
	}
	public void createBlock(long currentUserId, ArrayList<Long> userIds, TwitterFetchUsers.FinishedCallback callback) {
		mApi.createBlock(currentUserId, userIds, callback);
	}
	
	/*
	 * 
	 */
	public void reportSpam(long currentUserId, Long userId, TwitterFetchUsers.FinishedCallback callback) {
		mApi.reportSpam(currentUserId, userId, callback);
	}
	public void reportSpam(long currentUserId, ArrayList<Long> userIds, TwitterFetchUsers.FinishedCallback callback) {
		mApi.reportSpam(currentUserId, userIds, callback);
	}
	
	/*
	 * 
	 */
	public TwitterLists getLists(int userId) {
		TwitterLists cachedLists = mApi.getLists(userId, null);
		return cachedLists;
	}
	public TwitterLists getLists(int userId, TwitterFetchLists.FinishedCallback callback) {
		TwitterLists cachedLists = mApi.getLists(userId, callback);
		return cachedLists;
	}
	public TwitterLists getLists(String screenName) {
		TwitterLists cachedLists = mApi.getLists(screenName, null);
		return cachedLists;
	}
	public TwitterLists getLists(String screenName, TwitterFetchLists.FinishedCallback callback) {
		TwitterLists cachedLists = mApi.getLists(screenName, callback);
		return cachedLists;
	}
	
	/*
	 * 
	 */
	public TwitterStatus getStatus(long statusId, TwitterFetchStatus.FinishedCallback callback) {
		return mApi.getStatus(statusId, callback);
	}
	
	/*
	 * 
	 */
	public void setStatus(TwitterStatusUpdate statusUpdate, TwitterFetchStatus.FinishedCallback callback) {
		mApi.setStatus(statusUpdate, callback);
	}
	
	/*
	 * 
	 */
	public void setRetweet(long statusId, TwitterFetchStatus.FinishedCallback callback) {
		mApi.setRetweet(statusId, callback);
	}

	/*
	 * 
	 */
	public void setFavorite(TwitterStatus status, boolean isFavorite, TwitterModifyStatuses.FinishedCallback callback) {
		mApi.setFavorite(status, isFavorite, callback);
	}
	public void setFavorite(TwitterStatuses statuses, boolean isFavorite, TwitterModifyStatuses.FinishedCallback callback) {
		mApi.setFavorite(statuses, isFavorite, callback);
	}
	
	/*
	 * 
	 */
	public void triggerFetchStatuses(TwitterContentHandle contentHandle, TwitterPaging paging, TwitterFetchStatusesFinishedCallback callback, int priorityOffset) {
		mApi.triggerFetchStatuses(contentHandle, paging, callback, priorityOffset);
	}
	
	/*
	 * 
	 */
	public void cancelFetchStatuses(TwitterFetchStatusesFinishedCallback callback) {
		mApi.cancelFetchStatuses(callback);
	}
	
	/*
	 * 
	 */
	public void getFriendshipExists(String userScreenName, String userScreenNameToCheck, TwitterFetchBooleans.FinishedCallback callback) {
		mApi.getFriendshipExists(userScreenName, userScreenNameToCheck, callback);
	}
	
	/*
	 * 
	 */
	public boolean isAuthenticated() {
		return mApi.isAuthenticated();
	}
	
	
	
	
	public boolean hasValidTwitterInstance() {
		return mApi == null ? false : true;
	}
	
	/*
	 * Used for https://dev.twitter.com/docs/auth/oauth/oauth-echo
	 */
	public String generateTwitterVerifyCredentialsAuthorizationHeader() {
		if (mApi instanceof TwitterApi) {
			return ((TwitterApi)mApi).generateTwitterVerifyCredentialsAuthorizationHeader();
		}
		
		return null;
    }
	
	
	/*
	 * Quick and dirty system to handle mapping an appdotnet username in an entity to an id
	 */
	private static HashMap<String, Long> mUserIdentifierHashMap = new HashMap<String, Long>();
	
	public static void addUserIdentifier(String username, long id) {
		if (mUserIdentifierHashMap.containsKey(username) == false) {
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
