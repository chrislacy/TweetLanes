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

package org.tweetalib.android.model;

import java.util.Date;

import org.tweetalib.android.TwitterUtil;
import twitter4j.DirectMessage;
import twitter4j.User;

import android.text.Html;
import android.text.Spanned;

public class TwitterDirectMessage implements Comparable<TwitterDirectMessage> {

    /*
	 * 
	 */
    public enum MessageType {
        SENT, RECEIVED
    }

    /*
	 * 
	 */
    public TwitterDirectMessage(DirectMessage message, User otherUser) {
        mId = message.getId();
        mMessageType = message.getRecipientId() == otherUser.getId() ? MessageType.SENT
                : MessageType.RECEIVED;
        mText = message.getText();
        TwitterMediaEntity mediaEntity = TwitterMediaEntity.createMediaEntity(message);
        String descriptionMarkup = TwitterUtil.getStatusMarkup(mText,message.getMediaEntities() ,message.getURLEntities());
        mTextSpanned = Html.fromHtml(descriptionMarkup + " ");
        mCreatedAt = message.getCreatedAt();
        mOtherUserId = otherUser.getId();
        mOtherUserScreenName = otherUser.getScreenName();
        mOtherUser = new TwitterUser(otherUser);
        mSender = new TwitterUser(message.getSender());
    }

    /*
	 * 
	 */
    public long getId() {
        return mId;
    }

    public MessageType getMessageType() {
        return mMessageType;
    }

    public String getText() {
        return mText;
    }

    public Date getCreatedAt() {
        return mCreatedAt;
    }

    public long getOtherUserId() {
        return mOtherUserId;
    }

    public String getOtherUserScreenName() {
        return mOtherUserScreenName;
    }

    public TwitterUser getOtherUser()
    {
        return mOtherUser;
    }

    public TwitterUser getSender()
    {
        return mSender;
    }

    /*
	 *
	 */
    private final long mId;
    private final MessageType mMessageType;
    private final String mText;
    public final Spanned mTextSpanned;
    private final Date mCreatedAt;
    private final long mOtherUserId;
    private final String mOtherUserScreenName;
    private final TwitterUser mOtherUser;
    private final TwitterUser mSender;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(TwitterDirectMessage another) {
        // TODO: Change to use messageID
        if (mCreatedAt.before(another.mCreatedAt)) {
            return 1;
        }
        return -1;
    }
}
