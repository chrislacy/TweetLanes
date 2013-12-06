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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.TwitterUtil;

import java.text.ParseException;
import java.util.Date;

import twitter4j.URLEntity;

public class AdnPost {

    public long mId;
    public Long mInReplyTo;
    public String mText;
    public AdnUser mUser;
    public Date mCreatedAt;
    public String mSource;
    public boolean mIsDeleted;
    public boolean mIsRetweet;
    public boolean mIsRetweetedByMe;
    public boolean mIsFavorited;
    public AdnUser mOriginalAuthor;
    public long mOriginalId;
    public AdnMedia mEmbeddedMedia;
    public URLEntity[] mUrls;

    public AdnPost() {
    }

    public AdnPost(String jsonAsString) {
        try {
            JSONObject object = new JSONObject(jsonAsString);
            if (object.has("data")) {
                object = object.getJSONObject("data");
            }

            // It's possible to have a status with no text (likely when items
            // are deleted)
            mText = object.getString("text");
            if (mText == null) {
                mText = " ";
            }

            if (object.has("repost_of")) {
                mIsRetweet = true;
                AdnPost repost = new AdnPost(object.getJSONObject("repost_of")
                        .toString());
                mOriginalAuthor = repost.mUser;
                mOriginalId = repost.mId;
                mText = repost.mText;
                mEmbeddedMedia = repost.mEmbeddedMedia;
            }

            if (object.has("you_reposted")) {
                mIsRetweetedByMe = object.getBoolean("you_reposted");
            }

            if (object.has("you_starred")) {
                mIsFavorited = object.getBoolean("you_starred");
            }

            mId = object.getLong("id");
            if (object.has("reply_to")) {
                try {
                    // This value comes back as 'null' when no value.
                    mInReplyTo = object.getLong("reply_to");
                } catch (JSONException e) {
                }
            }

            if (object.has("is_deleted")) {
                try {
                    // This value comes back as 'null' when no value.
                    mIsDeleted = object.getBoolean("is_deleted");
                } catch (JSONException e) {
                }
            }

            String createdAtString = object.getString("created_at");
            mCreatedAt = TwitterUtil.iso6801StringToDate(createdAtString);

            if (object.has("user")) {
                String userString = object.getString("user");
                mUser = new AdnUser(userString);
            }

            if (object.has("source")) {
                JSONObject source = object.getJSONObject("source");
                mSource = source.getString("name");
            }

            if (object.has("entities")) {
                JSONObject entities = object.getJSONObject("entities");
                if (entities.has("mentions")) {
                    JSONArray jsonArray = entities.getJSONArray("mentions");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject mention = jsonArray.getJSONObject(i);
                        if (mention.has("id") && mention.has("name")) {
                            Long id = mention.getLong("id");
                            String username = mention.getString("name");
                            // HACK
                            TwitterManager.addUserIdentifier(username, id);
                        }
                    }
                }
                if (entities.has("links")) {
                    JSONArray jsonArray = entities.getJSONArray("links");
                    mUrls = new URLEntity[jsonArray.length()];
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject link = jsonArray.getJSONObject(i);
                        if (link.has("text") && link.has("url")) {
                            URLEntity url = new AdnUrl(link.getString("text"), link.getString("url"));
                            mUrls[i] = url;
                        }
                    }
                }
            }

            if (object.has("annotations")) {
                JSONArray annotations = object.getJSONArray("annotations");
                for (int i = 0; i < annotations.length(); i++) {
                    JSONObject annotation = annotations.getJSONObject(i);
                    if (annotation.getString("type").equals("net.app.core.oembed") && annotation.has("value")) {
                        JSONObject value = annotation.getJSONObject("value");
                        if (value.has("thumbnail_url")) {
                            mEmbeddedMedia = new AdnMedia(value.toString());
                        }
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

}
