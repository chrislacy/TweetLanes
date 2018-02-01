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

package com.tweetlanes.android.core.model;

import com.tweetlanes.android.core.Constant;

import org.tweetalib.android.TwitterContentHandleBase;

public class LaneDescriptor {

    private static final String kDefaultId = null;

    /*
     *
	 */
    public LaneDescriptor(Constant.LaneType laneType, String laneTitle,
                          TwitterContentHandleBase contentHandleBase) {

        this(laneType, laneTitle, kDefaultId, contentHandleBase);
    }

    /*
     *
	 */
    public LaneDescriptor(Constant.LaneType laneType, String laneTitle,
                          String identifier, TwitterContentHandleBase contentHandleBase) {

        mLaneType = laneType;
        mLaneTitle = laneTitle;
        mContentHandleBase = contentHandleBase;
        mIdentifier = identifier;
        mDisplay = true;
    }

    /*
     *
	 */
    public Constant.LaneType getLaneType() {
        return mLaneType;
    }

    public TwitterContentHandleBase getContentHandleBase() {
        return mContentHandleBase;
    }

    public String getLaneTitle() {
        return mLaneTitle;
    }

    public void setLaneTitle(String title) {
        mLaneTitle = title;
    }

    public String getIdentifier() {
        return mIdentifier;
    }

    public boolean getDisplay() {
        return mDisplay;
    }

    public void setDisplay(boolean display) {
        mDisplay = display;
    }

    /*
	 * 
	 */
    public String getCacheKey(String accountName) {
        String key = accountName + "_" + mLaneType.toString() + "_"
                + getLaneTitle();
        if (mIdentifier != null) {
            key += "_" + mIdentifier;
        }
        key = key.replaceAll("\\s", "");
        key += "_v" + Constant.CACHE_VERSION;
        return key;
    }

    private final Constant.LaneType mLaneType;
    private final TwitterContentHandleBase mContentHandleBase;
    private String mLaneTitle;
    private final String mIdentifier;
    private boolean mDisplay; // Will the lane be drawn?
}
