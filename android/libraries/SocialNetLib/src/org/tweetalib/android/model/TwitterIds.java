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

package org.tweetalib.android.model;

import java.util.ArrayList;

import twitter4j.IDs;

public class TwitterIds {

    public TwitterIds() {
        mIds = new ArrayList<Long>();

    }

    public int getIdCount() {
        return mIds.size();
    }

    public Long getId(int index) {
        return mIds.get(index);
    }

    private final ArrayList<Long> mIds;

    public void add(IDs ids) {
        long[] idsArray = ids.getIDs();
        for (long id : idsArray) {
            mIds.add(id);
        }
    }

    public void add(long[] ids) {
        if (ids == null) {
            return;
        }
        for (long id : ids) {
            mIds.add(id);
        }
    }
}
