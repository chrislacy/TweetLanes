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

import com.turbomanage.httpclient.BasicHttpClient;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.ParameterMap;

import org.appdotnet4j.model.AdnFile;
import org.appdotnet4j.model.AdnInteractions;
import org.appdotnet4j.model.AdnPaging;
import org.appdotnet4j.model.AdnPost;
import org.appdotnet4j.model.AdnPostCompose;
import org.appdotnet4j.model.AdnPosts;
import org.appdotnet4j.model.AdnUser;
import org.appdotnet4j.model.AdnUsers;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tweetalib.android.model.TwitterUser;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import twitter4j.Twitter;

public class AppdotnetApi extends SocialNetApi {

    /*
     *
	 */
    public AppdotnetApi(SocialNetConstant.Type type, String consumerKey,
                        String consumerSecret, String currentAccountKey) {
        super(type, consumerKey, consumerSecret, currentAccountKey);
    }

    private static byte[] readFile(File file) throws IOException {
        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");

        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength) throw new IOException("File size >= 2 GB");

            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }

    @Override
    public void init() {
        // TODO Auto-generated method stub

    }

    boolean isResponseValid(HttpResponse httpResponse) {
        if (httpResponse == null) {
            return false;
        }
        int status = httpResponse.getStatus();
        return status >= 200 && status < 300;
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
            return httpResponse.getBodyAsString();
        }
        return null;
    }

    String doPost(String path, JSONObject json) {
        byte[] data = json.toString().getBytes();

        HttpResponse httpResponse = getHttpClient().post(path, "application/json", data);
        if (isResponseValid(httpResponse)) {
            return httpResponse.getBodyAsString();
        }
        return null;
    }

    String doPost(String path, JSONObject json, String fileToken) {
        return doPost(path + "?file_token=" + fileToken, json);
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

    public AdnUsers getUsersWhoReposted(long postId) {
        return getUsers("/stream/0/posts/" + postId + "/reposters", null);
    }

    /*
     *
	 */
    public AdnPosts getAdnStream(AdnPaging paging) {
        return getPosts("/stream/0/posts/stream", null, paging);
    }

    /*
     *
	 */
    public AdnPosts getAdnGlobalStream(AdnPaging paging) {
        return getPosts("/stream/0/posts/stream/global", null, paging);
    }

    /*
     *
	 */
    public AdnPosts getAdnMentions(int userId, AdnPaging paging) {
        return getPosts("/stream/0/users/" + userId + "/mentions", null, paging);
    }

    /*
	 *
	 */
    public AdnPosts getAdnUserStream(int userId, AdnPaging paging) {
        return getPosts("/stream/0/users/" + userId + "/posts", null, paging);
    }

    public AdnInteractions getAdnInteractions() {
        return getInteractions("/stream/0/users/me/interactions", null);
    }

    public AdnPosts getAdnFavorites(String userId, AdnPaging paging) {
        return getPosts("/stream/0/users/" + userId + "/stars", null, paging);
    }

    /*
	 *
	 */
    public AdnPosts getAdnTagPosts(String tag, AdnPaging paging) {
        return getPosts("/stream/0/posts/tag/" + tag, null, paging);
    }

    public AdnPosts getAdnConversation(long postId, AdnPaging paging) {
        return getPosts("/stream/0/posts/" + postId + "/replies", null, paging);
    }

    /*
	 *
	 */
    private AdnPosts getPosts(String path, ParameterMap params, AdnPaging paging) {
        if (paging == null) {
            paging = new AdnPaging(1);
        }

        if (params == null) {
            params = new ParameterMap();
        }
        params.add("include_deleted", "0");
        params.add("include_muted", "0");
        params.add("include_post_annotations", "1");
        if (paging.getSinceId() > 0) {
            params.add("since_id", String.valueOf(paging.getSinceId()));
        }
        if (paging.getMaxId() > 0) {
            params.add("before_id", String.valueOf(paging.getMaxId()));
        }
        String streamString = doGet(path, params);
        if (streamString != null) {
            return new AdnPosts(streamString);
        }

        return null;
    }

    private AdnInteractions getInteractions(String path, ParameterMap params) {
        if (params == null) {
            params = new ParameterMap();
        }
        String interactionString = doGet(path, params);
        if (interactionString != null) {
            return new AdnInteractions(interactionString);
        }

        return null;
    }

    private AdnUsers getUsers(String path, ParameterMap params) {
        if (params == null) {
            params = new ParameterMap();
        }
        String userString = doGet(path, params);
        if (userString != null) {
            return new AdnUsers(userString);
        }

        return null;
    }

    /*
	 *
	 */
    public AdnPost getAdnPost(long id) {
        ParameterMap params = new ParameterMap();
        params.add("include_post_annotations", "1");
        String postString = doGet("/stream/0/posts/" + id, params);
        if (postString != null) {
            return new AdnPost(postString);
        }
        return null;
    }

    public AdnPost setAdnStatus(AdnPostCompose compose) {
        JSONObject post;
        String fileToken = null;
        try {
            post = new JSONObject()
                    .put("text", compose.mText)
                    .put("reply_to", compose.mInReplyTo);

            if (compose.mMediaFile != null) {
                AdnFile file = setAdnFile(compose.mMediaFile);
                if (file != null) {
                    JSONObject ann = new JSONObject();
                    ann.put("type", "net.app.core.oembed");
                    ann.put("value", new JSONObject()
                            .put("+net.app.core.file", new JSONObject()
                                    .put("file_id", file.mId)
                                    .put("file_token", file.mFileToken)
                                    .put("format", "oembed")
                            )
                    );
                    post.put("annotations", new JSONArray().put(ann));
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        String bodyAsString = fileToken == null ? doPost("/stream/0/posts", post) : doPost("/stream/0/posts", post,
                fileToken);
        if (bodyAsString != null) {
            return new AdnPost(bodyAsString);
        }

        return null;
    }

    private AdnFile setAdnFile(File file) {
        JSONObject json;
        try {
            json = new JSONObject()
                    .put("kind", "image")
                    .put("type", "com.tweetlanes.image")
                    .put("public", "0")
                    .put("name", file.getName());

            JSONObject response = new JSONObject(doPost("/stream/0/files", json));
            if (response.has("data")) {
                response = response.getJSONObject("data");
            }

            String id = response.getString("id");
            String fileToken = response.getString("file_token");
            byte[] data = AppdotnetApi.readFile(file);

            BasicHttpClient httpClient = getHttpClient();
            HttpResponse httpResponse = httpClient.put("/stream/0/files/" + id + "/content?file_token=" + fileToken,
                    "image/jpeg", data);

            if (isResponseValid(httpResponse)) {
                return new AdnFile(id, fileToken);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }


        return null;
    }

    public AdnPost deleteTweet(long existingPostId) {
        BasicHttpClient httpClient = getHttpClient();

        HttpResponse httpResponse = httpClient.delete("/stream/0/posts/" + existingPostId, null);

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

        HttpResponse httpResponse = httpClient.post("/stream/0/posts/" + existingPostId + "/repost", null);

        if (isResponseValid(httpResponse)) {
            String postAsString = httpResponse.getBodyAsString();
            if (postAsString != null) {
                return new AdnPost(postAsString);
            }
        }

        return null;
    }

    public AdnPost setAdnFavorite(long existingPostId, boolean favorite) {
        BasicHttpClient httpClient = getHttpClient();

        HttpResponse httpResponse;
        if (favorite) {
            httpResponse = httpClient.post("/stream/0/posts/" + existingPostId + "/star", null);
        } else {
            httpResponse = httpClient.delete("/stream/0/posts/" + existingPostId + "/star", null);
        }

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
            unfollowUser(username);
            return null;
        }
    }

    public AdnUser setAdnFollow(long userId, boolean follow) {
        if (follow) {
            return followUser(userId);
        } else {
            unfollowUser(userId);
            return null;
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

    private void unfollowUser(long userId) {
        BasicHttpClient httpClient = getHttpClient();
        httpClient.delete("/stream/0/users/" + userId + "/follow", null);
    }

    private void unfollowUser(String username) {
        BasicHttpClient httpClient = getHttpClient();
        httpClient.delete("/stream/0/users/@" + username + "/follow", null);
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
