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

import org.appdotnet4j.model.AdnPost;
import org.appdotnet4j.model.AdnPostCompose;
import org.appdotnet4j.model.AdnPosts;
import org.appdotnet4j.model.AdnUser;
import org.appdotnet4j.model.AdnUsers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tweetalib.android.model.TwitterUser;

import twitter4j.Twitter;

import com.turbomanage.httpclient.BasicHttpClient;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;

public class AppdotnetApi extends SocialNetApi {

    /*
	 * 
	 */
    public AppdotnetApi(SocialNetConstant.Type type, String consumerKey,
            String consumerSecret) {
        super(type, consumerKey, consumerSecret);
    }

    @Override
    public void init() {
        // TODO Auto-generated method stub

    }

    boolean isResponseValid(HttpResponse httpResponse) {
        int status = httpResponse.getStatus();
        if (status >= 200 && status < 300) {
            return true;
        }
        return false;
    }

    BasicHttpClient getHttpClient() {

        BasicHttpClient httpClient = new BasicHttpClient(
                "https://alpha-api.app.net");
        httpClient.addHeader("Authorization", "Bearer " + mCurrentOAuthToken);
        httpClient.setConnectionTimeout(2000);
        return httpClient;
    }

    BasicHttpClient getHttpClient(String accessToken) {

        BasicHttpClient httpClient = new BasicHttpClient(
                "https://alpha-api.app.net");
        httpClient.addHeader("Authorization", "Bearer " + accessToken);
        httpClient.setConnectionTimeout(2000);
        return httpClient;
    }

    String doGet(String path, ParameterMap params) {
        return doGet(path, params, mCurrentOAuthToken);
    }

    String doGet(String path, ParameterMap params, String accessToken) {
        HttpResponse httpResponse = getHttpClient(accessToken)
                .get(path, params);
        if (isResponseValid(httpResponse)) {
            String body = httpResponse.getBodyAsString();
            return body;
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.socialnetlib.android.SocialNetApi#verifyCredentialsSync(java.lang
     * .String, java.lang.String)
     */
    @Override
    public TwitterUser verifyCredentialsSync(String oAuthToken,
            String oAuthSecret) {

        String userString = doGet("/stream/0/users/me", null, oAuthToken);
        if (userString != null) {
            AdnUser user = new AdnUser(userString);
            return new TwitterUser(user);
        }

        return null;
    }

    /*
	 * 
	 */
    public TwitterUser getAdnUser(long userId) {

        String userString = doGet("/stream/0/users/" + userId, null);
        if (userString != null) {
            AdnUser user = new AdnUser(userString);
            return new TwitterUser(user);
        }

        return null;
    }

    public TwitterUser getAdnUser(String userName) {

        String userString = doGet("/stream/0/users/@" + userName, null);
        if (userString != null) {
            AdnUser user = new AdnUser(userString);
            return new TwitterUser(user);
        }

        return null;
    }

    public long[] getAdnFollowing() {
        String userIds = doGet("/stream/0/users/me/following/ids", null);
        if (userIds != null) {
            try {
                JSONArray array = new JSONObject(userIds).getJSONArray("data");
                long[] ids = new long[array.length()];
                for (int i = 0; i < array.length(); ++i) {
                    ids[i] = array.getLong(i);
                }
                return ids;
            } catch (JSONException e) {
                return null;
            }
        }

        return null;
    }

    public long[] getAdnFollowedBy() {
        String userIds = doGet("/stream/0/users/me/followers/ids", null);
        if (userIds != null) {
            try {
                JSONArray array = new JSONObject(userIds).getJSONArray("data");
                long[] ids = new long[array.length()];
                for (int i = 0; i < array.length(); ++i) {
                    ids[i] = array.getLong(i);
                }
                return ids;
            } catch (JSONException e) {
                return null;
            }
        }

        return null;
    }

    public AdnUsers getAdnMultipleUsers(long[] ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.length; ++i) {
            if (i != 0) {
                sb.append(',');
            }
            sb.append(ids[i]);
        }
        return getUsers("/stream/0/users",
                new ParameterMap().add("ids", sb.toString()));
    }

    /*
	 * 
	 */
    public AdnPosts getAdnStream() {
        return getPosts("/stream/0/posts/stream", null);
    }

    /*
	 * 
	 */
    public AdnPosts getAdnGlobalStream() {
        return getPosts("/stream/0/posts/stream/global", null);
    }

    /*
	 * 
	 */
    public AdnPosts getAdnMentions(int userId) {
        return getPosts("/stream/0/users/" + userId + "/mentions", null);
    }

    /*
	 * 
	 */
    public AdnPosts getAdnUserStream(int userId) {
        return getPosts("/stream/0/users/" + userId + "/posts", null);
    }

    public AdnPosts getAdnFavorites(String userId) {
        return getPosts("/stream/0/users/" + userId + "/stars", null);
    }

    /*
	 * 
	 */
    public AdnPosts getAdnTagPosts(String tag) {
        return getPosts("/stream/0/posts/tag/" + tag, null);
    }

    /*
	 * 
	 */
    private AdnPosts getPosts(String path, ParameterMap params) {
        if (params == null) {
            params = new ParameterMap();
        }
        params.add("include_deleted", "0");
        params.add("include_muted", "0");
        String streamString = doGet(path, params);
        if (streamString != null) {
            AdnPosts posts = new AdnPosts(streamString);
            return posts;
        }

        return null;
    }

    private AdnUsers getUsers(String path, ParameterMap params) {
        if (params == null) {
            params = new ParameterMap();
        }
        String userString = doGet(path, params);
        if (userString != null) {
            AdnUsers users = new AdnUsers(userString);
            return users;
        }

        return null;
    }

    /*
	 * 
	 */
    public AdnPost getAdnPost(long id) {
        String postString = doGet("/stream/0/posts/" + id, null);
        if (postString != null) {
            return new AdnPost(postString);
        }
        return null;
    }

    public AdnPost setAdnStatus(AdnPostCompose compose) {
        BasicHttpClient httpClient = getHttpClient();
        ParameterMap params = httpClient.newParams().add("text", compose.mText);
        if (compose.mInReplyTo != null) {
            params = params.add("reply_to", compose.mInReplyTo.toString());
        }

        HttpResponse httpResponse = httpClient.post("/stream/0/posts", params);
        if (isResponseValid(httpResponse)) {
            String postAsString = httpResponse.getBodyAsString();
            if (postAsString != null) {
                return new AdnPost(postAsString);
            }
        }

        return null;
    }

    public AdnPost setAdnRepost(long existingPostId) {
        BasicHttpClient httpClient = getHttpClient();

        HttpResponse httpResponse = httpClient.post("/stream/0/posts/"
                + existingPostId + "/repost", null);

        if (isResponseValid(httpResponse)) {
            String postAsString = httpResponse.getBodyAsString();
            if (postAsString != null) {
                return new AdnPost(postAsString);
            }
        }

        return null;
    }

    public AdnUser setAdnFollow(String username, boolean follow) {
        if (follow) {
            return followUser(username);
        } else {
            return unfollowUser(username);
        }
    }

    public AdnUser setAdnFollow(long userId, boolean follow) {
        if (follow) {
            return followUser(userId);
        } else {
            return unfollowUser(userId);
        }
    }

    private AdnUser followUser(long userId) {
        BasicHttpClient httpClient = getHttpClient();
        HttpResponse httpResponse = httpClient.post("/stream/0/users/" + userId
                + "/follow", null);
        if (isResponseValid(httpResponse)) {
            String userAsString = httpResponse.getBodyAsString();
            if (userAsString != null) {
                return new AdnUser(userAsString);
            }
        }
        return null;
    }

    private AdnUser followUser(String username) {
        BasicHttpClient httpClient = getHttpClient();
        HttpResponse httpResponse = httpClient.post("/stream/0/users/@"
                + username + "/follow", null);
        if (isResponseValid(httpResponse)) {
            String userAsString = httpResponse.getBodyAsString();
            if (userAsString != null) {
                return new AdnUser(userAsString);
            }
        }
        return null;
    }

    private AdnUser unfollowUser(long userId) {
        BasicHttpClient httpClient = getHttpClient();
        httpClient.delete("/stream/0/users/" + userId + "/follow", null);
        return null;
    }

    private AdnUser unfollowUser(String username) {
        BasicHttpClient httpClient = getHttpClient();
        httpClient.delete("/stream/0/users/@" + username + "/follow", null);
        return null;
    }

    @Override
    Twitter getAndConfigureApiInstance() {
        return null;
    }

    @Override
    void clearApiInstance() {
        // TODO Auto-generated method stub

    }

    public SocialNetConstant.Type getSocialNetType() {
        return SocialNetConstant.Type.Appdotnet;
    }
}
