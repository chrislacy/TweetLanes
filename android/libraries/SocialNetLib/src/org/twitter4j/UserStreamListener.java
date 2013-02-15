/*
 * Copyright 2007 Yusuke Yamamoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.twitter4j;

/**
 * @author Rémy Rakic at gmail.com
 * @since org.twitter4j 2.1.3
 */
public interface UserStreamListener extends StatusListener {
    void onDeletionNotice(long directMessageId, long userId);

    /**
     * @param friendIds
     * @since org.twitter4j 2.1.3
     */
    void onFriendList(long[] friendIds);

    /**
     * @param source
     * @param target
     * @param favoritedStatus
     * @since org.twitter4j 2.1.3
     */
    void onFavorite(User source, User target, Status favoritedStatus);

    /**
     * @param source
     * @param target
     * @param unfavoritedStatus
     * @since org.twitter4j 2.1.3
     */
    void onUnfavorite(User source, User target, Status unfavoritedStatus);

    /**
     * @param source
     * @param followedUser
     * @since org.twitter4j 2.1.3
     */
    void onFollow(User source, User followedUser);

    /**
     * @param source
     * @param target
     * @param retweetedStatus
     * @since org.twitter4j 2.1.3
     */
    void onRetweet(User source, User target, Status retweetedStatus);

    /**
     * @param directMessage
     * @since org.twitter4j 2.1.3
     */
    void onDirectMessage(DirectMessage directMessage);

    /**
     * @param addedMember
     * @param listOwner
     * @param list
     * @since org.twitter4j 2.1.11
     */
    void onUserListMemberAddition(User addedMember, User listOwner, UserList list);

    /**
     * @param deletedMember
     * @param listOwner
     * @param list
     * @since org.twitter4j 2.1.11
     */
    void onUserListMemberDeletion(User deletedMember, User listOwner, UserList list);

    /**
     * @param subscriber
     * @param listOwner
     * @param list
     * @since org.twitter4j 2.1.3
     */
    void onUserListSubscription(User subscriber, User listOwner, UserList list);

    /**
     * @param subscriber
     * @param listOwner
     * @param list
     * @since org.twitter4j 2.1.11
     */
    void onUserListUnsubscription(User subscriber, User listOwner, UserList list);

    /**
     * @param listOwner
     * @param list
     * @since org.twitter4j 2.1.3
     */
    void onUserListCreation(User listOwner, UserList list);

    /**
     * @param listOwner
     * @param list
     * @since org.twitter4j 2.1.3
     */
    void onUserListUpdate(User listOwner, UserList list);

    /**
     * @param listOwner
     * @param list
     * @since org.twitter4j 2.1.3
     */
    void onUserListDeletion(User listOwner, UserList list);

    /**
     * @param updatedUser updated user
     * @since org.twitter4j 2.1.9
     */
    void onUserProfileUpdate(User updatedUser);

    /**
     * @param source
     * @param blockedUser
     * @since org.twitter4j 2.1.3
     */
    void onBlock(User source, User blockedUser);

    /**
     * @param source
     * @param unblockedUser
     * @since org.twitter4j 2.1.3
     */
    void onUnblock(User source, User unblockedUser);
}
