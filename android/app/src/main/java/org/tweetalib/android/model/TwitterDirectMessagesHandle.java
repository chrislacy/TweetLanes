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

public class TwitterDirectMessagesHandle {

    /*
     *
	 */
    public TwitterDirectMessagesHandle(long userId, Long otherUserId) {
        mUserId = userId;
        mOtherUserId = otherUserId;
    }

    /*
     *
	 */
    String getKey() {
        String key = mUserId.toString();
        if (mOtherUserId != null) {
            key += "_" + mOtherUserId.toString();
        }
        return key;
    }

    private final Long mUserId;
    final Long mOtherUserId;
}
