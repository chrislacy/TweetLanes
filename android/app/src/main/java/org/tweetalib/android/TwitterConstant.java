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

public class TwitterConstant {

    public enum ContentType {
        AUTH, BOOLEAN, // a true or false value (eg, does friendship exist?)
        DIRECT_MESSAGES, // a collection of direct messages
        STATUS, // a single tweet/status
        STATUSES, // a collection of tweets
        USER, // a single user
        USERS, // a collection of users (eg, followers)
    }

    public enum DirectMessagesType {
        ALL_MESSAGES, ALL_MESSAGES_FRESH, SENT_MESSAGE, RECIEVED_MESSAGES
    }

    public enum BooleanType {
        FRIENDSHIP_EXISTS, BLOCK_EXISTS,
    }

    public enum StatusType {
        GET_STATUS, SET_STATUS, SET_RETWEET,
    }

    public enum StatusesType {
        NONE, USER_TIMELINE, USER_HOME_TIMELINE, USER_MENTIONS, USER_LIST_TIMELINE, USER_FAVORITES, RETWEETS_OF_ME,
        SCREEN_NAME_SEARCH, STATUS_SEARCH, PREVIOUS_CONVERSATION, FULL_CONVERSATION, SET_FAVORITE, GLOBAL_FEED, DELETE
    }

    public enum UsersType {
        FRIENDS, FOLLOWERS, RETWEETED_BY, PEOPLE_SEARCH, UPDATE_FRIENDSHIP, CREATE_BLOCK, REPORT_SPAM,
    }
}
