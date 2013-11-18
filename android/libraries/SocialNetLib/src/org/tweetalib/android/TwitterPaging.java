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

import android.util.Log;

import org.appdotnet4j.model.AdnPaging;

import twitter4j.Paging;

public class TwitterPaging {

    public static final int DEFAULT_STATUS_COUNT = 100;
    public static final int INCREMENTING_STATUS_COUNT_START = 25;
    public static final int INCREMENTING_STATUS_COUNT_MAX = 200;

    /*
     *
	 */
    public static TwitterPaging createGetOlder(long statusId) {
        return new TwitterPaging(null, null, null, statusId);
    }

    public static TwitterPaging createGetNewer(long statusId) {
        return new TwitterPaging(null, null, statusId, null);
    }

    public static TwitterPaging createGetOlderWithPageSize(long statusId, int count) {
        return new TwitterPaging(null, count, null, statusId);
    }

    public static TwitterPaging createGetNewerWithPageSize(long statusId, int count) {
        return new TwitterPaging(null, count, statusId, null);
    }

    public static TwitterPaging createGetMostRecent() {
        return new TwitterPaging(1, null, null, null);
    }

    public static TwitterPaging createGetMostRecent(int count) {
        return new TwitterPaging(1, count, null, null);
    }

    /*
     *
	 */
    public TwitterPaging(Integer page, Integer count, Long sinceId, Long maxId) {
        if (page != null) {
            mPage = page;
        }
        if (count != null && count > 0) {
            mCount = count;
        } else {
            mCount = DEFAULT_STATUS_COUNT;
        }
        if (maxId != null && maxId > 0) {
            mMaxId = maxId - 1; // Decrement by 1 so we don't fetch the current
            // tweet again. Bit of
            // a hack...
            // TODO: Fix me
            if (mMaxId < 0) {
                Log.d("ERROR", "mMaxId is " + mMaxId.longValue()
                        + ", must be >= 0");
                mMaxId = (long) 0;
            }
        }
        if (sinceId != null) {
            mSinceId = sinceId;
            if (mSinceId < 0) {
                Log.d("ERROR", "mSinceId is " + mSinceId.longValue()
                        + ", must be >= 0");
                mSinceId = (long) 0;
            }
        }
    }

    /*
     *
	 */
    public Paging getT4JPaging() {
        Paging result = new Paging();
        if (mMaxId == null && mSinceId == null) {

            if (mPage != null) {
                result.setPage(mPage);
            } else {
                result.setPage(1);
            }
        } else {
            if (mMaxId != null) {
                if (mMaxId.longValue() >= 0) {
                    result.setMaxId(mMaxId);
                } else {
                    Log.d("ERROR", "mMaxId is " + mMaxId.longValue()
                            + ", must be >= 0");
                }
            }
            if (mSinceId != null) {
                if (mSinceId.longValue() >= 0) {
                    result.setSinceId(mSinceId);
                } else {
                    Log.d("ERROR", "mSinceId is " + mSinceId.longValue()
                            + ", must be >= 0");
                }
            }
        }

        if (mCount != null) {
            result.setCount(mCount);
        } else {
            result.setCount(DEFAULT_STATUS_COUNT);
        }

        return result;
    }

    public AdnPaging getAdnPaging() {

        AdnPaging result = new AdnPaging(1);
        if (mMaxId == null && mSinceId == null) {
            if (mPage != null) {
                result.mPage = mPage;
            } else {
                result.mPage = 1;
            }
        } else {
            if (mMaxId != null) {
                if (mMaxId.longValue() >= 0) {
                    result.setMaxId(mMaxId);
                }
            }
            if (mSinceId != null) {
                if (mSinceId.longValue() >= 0) {
                    result.setSinceId(mSinceId);
                }
            }
        }

        return result;
    }

    public Integer getCount() {
        return mCount;
    }

    private Integer mPage;
    private final Integer mCount;
    private Long mMaxId;
    private Long mSinceId;

}
