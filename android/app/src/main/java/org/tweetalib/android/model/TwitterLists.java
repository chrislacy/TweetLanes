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

import java.util.ArrayList;
import java.util.Collections;

import twitter4j.ResponseList;
import twitter4j.UserList;

public class TwitterLists {

    public TwitterLists(ResponseList<UserList> lists) {
        mLists = new ArrayList<TwitterList>();

        for (UserList list : lists) {
            mLists.add(new TwitterList(list));
        }

        if (mLists.size() > 1) {
            sort();
        }

    }

    public TwitterLists() {
        mLists = new ArrayList<TwitterList>();
    }

    public int getListCount() {
        return mLists.size();
    }

    public TwitterList getList(int index) {
        return mLists.get(index);
    }

    private void sort() {
        Collections.sort(mLists);
    }

    private final ArrayList<TwitterList> mLists;

}
