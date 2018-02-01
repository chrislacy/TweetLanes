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

package org.appdotnet4j.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.tweetalib.android.TwitterUtil;

import java.text.ParseException;
import java.util.Date;

class AdnInteraction {

    public String mAction;
    public AdnPosts mPosts;

    public AdnInteraction(String jsonAsString) {
        try {
            JSONObject object = new JSONObject(jsonAsString);
            if (object.has("data")) {
                object = object.getJSONObject("data");
            }

            mAction = object.getString("action");

            String eventDateString = object.getString("event_date");
            Date createdAt = TwitterUtil.iso6801StringToDate(eventDateString);

            // more like "unique" Id.
            long unqiueId = Long.valueOf(eventDateString.replaceAll("-|:|Z|T", ""));

            AdnUsers users = new AdnUsers(object.getJSONArray("users"));

            if (mAction.equals("repost") || mAction.equals("star") || mAction.equals("reply")) {
                mPosts = new AdnPosts(object.getJSONArray("objects"));
                if (mPosts != null && mPosts.mPosts != null) {
                    String verb = "somethinged";
                    if (mAction.equals("repost")) {
                        verb = "reposted";
                    } else if (mAction.equals("star")) {
                        verb = "starred";
                    } else if (mAction.equals("reply")) {
                        verb = "replied to";
                    }
                    for (AdnPost post : mPosts.mPosts) {
                        String userString = "";
                        for (AdnUser user : users.mUsers) {
                            if (userString.equals("")) {
                                userString = "@" + user.mUserName;
                            } else {
                                userString += ", " + "@" + user.mUserName;
                            }
                        }
                        post.mId = unqiueId;
                        post.mText = userString + " " + verb + " the following post:\n\n" + post.mText;
                        post.mUser = users.mUsers.get(0);
                        post.mInReplyTo = null;
                        post.mCreatedAt = createdAt;
                    }
                }
            } else if (mAction.equals("follow") && users != null && users.mUsers != null) {
                mPosts = new AdnPosts();
                String userString = "";
                long id = 0;
                for (AdnUser user : users.mUsers) {
                    if (userString.equals("")) {
                        userString = "@" + user.mUserName;
                    } else {
                        userString += ", " + "@" + user.mUserName;
                    }
                    id += user.mId;
                }
                AdnPost meta = new AdnPost();
                meta.mId = unqiueId;
                meta.mText = userString + " started following you.";
                meta.mCreatedAt = createdAt;
                meta.mUser = users.mUsers.get(0);
                meta.mSource = "App.net";
                mPosts.mPosts.add(meta);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
