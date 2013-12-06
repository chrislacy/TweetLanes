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

public class TwitterUsers {

    /*
     *
	 */
    public TwitterUsers() {
        mUsers = new ArrayList<TwitterUser>();
    }

    /*
     *
	 */
    public TwitterUsers(TwitterUsers another) {
        mUsers = new ArrayList<TwitterUser>(another.mUsers);
    }

    /*
	 * 
	 */
    public TwitterUsers(TwitterUser another) {
        mUsers = new ArrayList<TwitterUser>();
        add(another);
    }

    /*
	 * 
	 */
    public int getUserCount() {
        return mUsers.size();
    }

    /*
	 * 
	 */
    public TwitterUser getUser(int position) {
        return mUsers.get(position);
    }

    /*
	 * 
	 */
    public void add(TwitterUser user) {
        mUsers.add(new TwitterUser(user));
    }

    /*
	 * 
	 */
    public void remove(TwitterUsers users) {

        if (mUsers != null) {
            ArrayList<Integer> toRemoveList = new ArrayList<Integer>();

            for (int existingIndex = 0; existingIndex < mUsers.size(); existingIndex++) {
                TwitterUser user = mUsers.get(existingIndex);
                for (int i = 0; i < users.getUserCount(); i++) {
                    TwitterUser userToRemove = users.getUser(i);
                    if (userToRemove.getId() == user.getId()) {
                        toRemoveList.add(existingIndex);
                    }
                }
            }

            for (int toRemoveIndex : toRemoveList) {
                mUsers.remove(toRemoveIndex);
            }
        }
    }

    /*
	 * 
	 */
    private final ArrayList<TwitterUser> mUsers;
}
