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

import twitter4j.UserList;

public class TwitterList implements Comparable<TwitterList> {

    public TwitterList(UserList list) {
        mId = list.getId();
        mName = list.getName();
        mFullName = list.getFullName();
        mDescription = list.getDescription();
        mIsPublic = list.isPublic();
        mIsFollowing = list.isFollowing();
        mMemberCount = list.getMemberCount();
        mSubscriberCount = list.getSubscriberCount();
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String getFullName() {
        return mFullName;
    }

    public String getDescription() {
        return mDescription;
    }

    public boolean getIsPublic() {
        return mIsPublic;
    }

    public boolean getIsFollowing() {
        return mIsFollowing;
    }

    public int getMemberCount() {
        return mMemberCount;
    }

    public int getSubscriberCount() {
        return mSubscriberCount;
    }

    private final int mId;
    private final String mName;
    private final String mFullName;
    private final String mDescription;
    private final boolean mIsPublic;
    private final boolean mIsFollowing;
    private final int mMemberCount;
    private final int mSubscriberCount;

    @Override
    public int compareTo(TwitterList another) {
        return mName.compareToIgnoreCase(another.mName);
    }
}
