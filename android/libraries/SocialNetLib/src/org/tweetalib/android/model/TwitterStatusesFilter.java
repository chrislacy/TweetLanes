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

/*
 * 
 */
public final class TwitterStatusesFilter {

    enum FilterType {
        ALL, HIDE_RETWEETS, HIDE_REPLIES, HIDE_RETWEETS_REPLIES, FILTER_MAX,
    }

    public void setShowRetweets(boolean show) {
        mShowRetweets = show;
    }

    public boolean getShowRetweets() {
        return mShowRetweets;
    }

    public void setShowReplies(boolean show) {
        mShowReplies = show;
    }

    public boolean getShowReplies() {
        return mShowReplies;
    }

    /*
     *
	 */
    private boolean mShowRetweets = true;
    private boolean mShowReplies = true;

    FilterType getFilterType() {
        if (!mShowRetweets && !mShowReplies) {
            return FilterType.HIDE_RETWEETS_REPLIES;
        } else if (!mShowReplies) {
            return FilterType.HIDE_REPLIES;
        } else if (!mShowRetweets) {
            return FilterType.HIDE_RETWEETS;
        }
        return FilterType.ALL;
    }
}