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

package com.tweetlanes.android.core.view;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tweetlanes.android.core.R;
import com.tweetlanes.android.core.util.Util;
import com.tweetlanes.android.core.widget.urlimageviewhelper.UrlImageViewHelper;

import org.socialnetlib.android.SocialNetConstant;
import org.tweetalib.android.TwitterFetchBooleans.FinishedCallback;
import org.tweetalib.android.TwitterFetchResult;
import org.tweetalib.android.TwitterFetchUser;
import org.tweetalib.android.TwitterFetchUsers;
import org.tweetalib.android.TwitterManager;
import org.tweetalib.android.TwitterUtil;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterUser;
import org.tweetalib.android.model.TwitterUsers;
import org.tweetalib.android.widget.URLSpanNoUnderline;

import java.util.ArrayList;

import twitter4j.URLEntity;

public class ProfileFragment extends BaseLaneFragment {

    /*
     *
	 */
    public static ProfileFragment newInstance(int laneIndex, final Long userId) {

        ProfileFragment fragment = new ProfileFragment();

        fragment.configureBaseLaneFragment(laneIndex, "Profile",
                new ConfigureBundleListener() {

                    @Override
                    public void addValues(Bundle args) {
                        if (userId != null) {
                            args.putString("userId", userId.toString());
                        }
                    }

                });

        return fragment;
    }

    /*
     *
	 */
    public static ProfileFragment newInstance(int laneIndex,
                                              final String screenName) {

        ProfileFragment fragment = new ProfileFragment();

        fragment.configureBaseLaneFragment(laneIndex, "Profile",
                new ConfigureBundleListener() {

                    @Override
                    public void addValues(Bundle args) {
                        if (screenName != null) {
                            args.putString("screenName", screenName);
                        }
                    }

                });

        return fragment;
    }

    private TwitterUser mUser;
    private View mProfileView;
    private Boolean mFollowsLoggedInUser;
    private Boolean mLoggedInUserFollows;
    private FinishedCallback mFriendshipCallback;
    private Button mFriendshipButton;
    private View mFriendshipDivider;

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
     * android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        SocialNetConstant.Type socialNetType = SocialNetConstant.Type.Twitter;

        if (getArguments().getString("userId") != null) {
            Long userId = Long.parseLong(getArguments().getString("userId"));
            mUser = TwitterManager.get().getUser(userId);
            if (mUser != null) {
                socialNetType = mUser.getSocialNetType();
            }
        }

        mProfileView = inflater
                .inflate(
                        socialNetType == SocialNetConstant.Type.Appdotnet ? R.layout.profile_adn
                                : R.layout.profile, null);

        if (mFollowsLoggedInUser == null && mUser != null
                && mFriendshipCallback == null) {

            String currentUserScreenName = getBaseLaneActivity().getApp()
                    .getCurrentAccountScreenName();
            if (currentUserScreenName != null) {
                mFriendshipCallback = TwitterManager.get()
                        .getFetchBooleansInstance().new FinishedCallback() {

                    @Override
                    public void finished(TwitterFetchResult result,
                                         ArrayList<Boolean> returnValues) {

                        if (result.isSuccessful() && returnValues != null
                                && returnValues.size() == 2) {
                            mFollowsLoggedInUser = returnValues.get(0);
                            mLoggedInUserFollows = returnValues.get(1);
                            TextView followingTextView = (TextView) mProfileView
                                    .findViewById(R.id.followState);
                            if (mFollowsLoggedInUser != null
                                    && mFollowsLoggedInUser.booleanValue()) {
                                followingTextView.setText(R.string.follows_you);
                            } else {
                                followingTextView.setText(null);
                            }

                            configureFriendshipButtonVisibility(mLoggedInUserFollows);

                        }

                    }

                };

                TwitterManager.get().getFriendshipExists(mUser.getScreenName(),
                        currentUserScreenName, mFriendshipCallback);
            }
        }

        // Always download the user again
        setInitialDownloadState(InitialDownloadState.WAITING);

        configureView();

        return mProfileView;
    }

    /*
     *
	 */
    void configureView() {

        TextView fullNameTextView = (TextView) mProfileView
                .findViewById(R.id.fullNameTextView);
        TextView followingTextView = (TextView) mProfileView
                .findViewById(R.id.followState);
        TextView descriptionTextView = (TextView) mProfileView
                .findViewById(R.id.bioTextView);
        TextView tweetCount = (TextView) mProfileView
                .findViewById(R.id.tweetCountLabel);
        TextView followingCount = (TextView) mProfileView
                .findViewById(R.id.followingCountLabel);
        TextView followersCount = (TextView) mProfileView
                .findViewById(R.id.followersCountLabel);
        TextView favoritesCount = (TextView) mProfileView
                .findViewById(R.id.favorites_count);
        LinearLayout linkLayout = (LinearLayout) mProfileView
                .findViewById(R.id.linkLayout);
        TextView link = (TextView) mProfileView.findViewById(R.id.link);
        LinearLayout locationLayout = (LinearLayout) mProfileView
                .findViewById(R.id.locationLayout);
        TextView location = (TextView) mProfileView.findViewById(R.id.location);
        LinearLayout detailsLayout = (LinearLayout) mProfileView
                .findViewById(R.id.detailsLayout);
        ImageView privateAccountImage = (ImageView) mProfileView
                .findViewById(R.id.private_account_image);
        mFriendshipButton = (Button) mProfileView
                .findViewById(R.id.friendship_button);
        mFriendshipDivider = mProfileView.findViewById(R.id.friendship_divider);

        if (mUser != null) {
            ImageView avatar = (ImageView) mProfileView
                    .findViewById(R.id.profileImage);
            // String imageUrl =
            // TwitterManager.getProfileImageUrl(mUser.getScreenName(),
            // TwitterManager.ProfileImageSize.ORIGINAL);
            String imageUrl = mUser
                    .getProfileImageUrl(TwitterManager.ProfileImageSize.ORIGINAL);
            UrlImageViewHelper.setUrlDrawable(avatar, imageUrl,
                    R.drawable.ic_contact_picture);
            // avatar.setImageURL(imageUrl);

            ImageView coverImage = (ImageView) mProfileView
                    .findViewById(R.id.coverImage);
            if (coverImage != null) {
                String url = mUser.getCoverImageUrl();
                if (url != null) {
                    UrlImageViewHelper.setUrlDrawable(coverImage, url,
                            R.drawable.ic_contact_picture);
                }
            }

            fullNameTextView.setText(mUser.getName());
            if (mFollowsLoggedInUser != null
                    && mFollowsLoggedInUser.booleanValue()) {
                followingTextView.setText(R.string.follows_you);
            } else {
                followingTextView.setText(null);
            }

            String description = mUser.getDescription();
            URLEntity[] urlEntities = mUser.getUrlEntities();
            if (description != null) {
                String descriptionMarkup = TwitterUtil.getTextMarkup(description, urlEntities);
                descriptionTextView.setText(Html.fromHtml(descriptionMarkup
                        + " "));
                descriptionTextView.setMovementMethod(LinkMovementMethod
                        .getInstance());
                URLSpanNoUnderline.stripUnderlines(descriptionTextView);
            }

            if (mUser.getUrl() != null) {
                linkLayout.setVisibility(View.VISIBLE);
                String urlMarkup = TwitterUtil.getTextMarkup(mUser.getUrl(), urlEntities);
                link.setText(Html.fromHtml(urlMarkup + ""));
                link.setMovementMethod(LinkMovementMethod.getInstance());
                URLSpanNoUnderline.stripUnderlines(link);
            } else {
                linkLayout.setVisibility(View.GONE);
            }

            detailsLayout.setVisibility(View.VISIBLE);
            privateAccountImage
                    .setVisibility(mUser.getProtected() ? View.VISIBLE
                            : View.GONE);

            tweetCount.setText(Util.getPrettyCount(mUser.getStatusesCount()));
            followingCount
                    .setText(Util.getPrettyCount(mUser.getFriendsCount()));
            followersCount.setText(Util.getPrettyCount(mUser
                    .getFollowersCount()));
            if (favoritesCount != null) {
                favoritesCount.setText(Util.getPrettyCount(mUser
                        .getFavoritesCount()));
            }

            if (mUser.getLocation() != null) {
                locationLayout.setVisibility(View.VISIBLE);
                location.setText(mUser.getLocation());
            } else {
                locationLayout.setVisibility(View.GONE);
            }


            configureFriendshipButtonVisibility(mLoggedInUserFollows);

        } else {
            fullNameTextView.setText(null);
            followingTextView.setText(null);
            descriptionTextView.setText(null);

            detailsLayout.setVisibility(View.GONE);
            linkLayout.setVisibility(View.GONE);
            locationLayout.setVisibility(View.GONE);
            mFriendshipButton.setVisibility(View.GONE);
            mFriendshipDivider.setVisibility(View.GONE);
            privateAccountImage.setVisibility(View.GONE);
        }
    }

    /*
	 * 
	 */
    private void configureFriendshipButtonVisibility(Boolean loggedInUserFollows) {

        if (loggedInUserFollows == null) {
            mFriendshipButton.setVisibility(View.GONE);
            mFriendshipDivider.setVisibility(View.GONE);
        } else {
            mFriendshipButton.setVisibility(View.VISIBLE);
            mFriendshipDivider.setVisibility(View.VISIBLE);
            mFriendshipButton
                    .setText(loggedInUserFollows ? R.string.action_unfollow
                            : R.string.action_follow);
            mFriendshipButton.setOnClickListener(mFrienshipButtonListener);
        }
    }

    /*
	 * 
	 */
    private final OnClickListener mFrienshipButtonListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            if (mUser != null && mLoggedInUserFollows != null
                    && mUpdateFriendshipFinishedCallback == null) {
                String loggedInUserScreenName = getBaseLaneActivity().getApp()
                        .getCurrentAccountScreenName();

                final boolean willCreateFriendship = mLoggedInUserFollows != null && !mLoggedInUserFollows;
                configureFriendshipButtonVisibility(willCreateFriendship);

                mUpdateFriendshipFinishedCallback = TwitterManager.get()
                        .getFetchUsersInstance().new FinishedCallback() {

                    @Override
                    public void finished(TwitterFetchResult result,
                                         TwitterUsers users) {
                        if (result.isSuccessful()) {
                            mLoggedInUserFollows = willCreateFriendship;
                        }

                        mUpdateFriendshipFinishedCallback = null;
                    }

                };

                TwitterManager.get().updateFriendship(loggedInUserScreenName,
                        mUser, willCreateFriendship,
                        mUpdateFriendshipFinishedCallback);
            }
        }

    };

    private TwitterFetchUsers.FinishedCallback mUpdateFriendshipFinishedCallback;

    /*
         * (non-Javadoc)
         *
         * @see
         * com.tweetlanes.android.core.view.BaseLaneFragment#triggerInitialDownload()
         */
    @Override
    public void triggerInitialDownload() {

        TwitterFetchUser.FinishedCallback callback = TwitterManager.get()
                .getFetchUserInstance().new FinishedCallback() {

            public void finished(TwitterFetchResult result, TwitterUser user) {
                setInitialDownloadState(InitialDownloadState.DOWNLOADED);
                if (user != null) {
                    mUser = user;
                }
                configureView();
            }

        };

        Long userId = null;
        if (getArguments().getString("userId") != null) {
            userId = Long.parseLong(getArguments().getString("userId"));
        }

        if (userId != null) {
            TwitterManager.get().getUser(userId, callback);
            setInitialDownloadState(InitialDownloadState.DOWNLOADING);
        } else {
            String screenName = getArguments().getString(("screenName"));
            if (screenName != null) {
                TwitterManager.get().getUser(screenName, callback);
                setInitialDownloadState(InitialDownloadState.DOWNLOADING);
            } else {
                setInitialDownloadState(InitialDownloadState.DOWNLOADED);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#UpdateTweetCache()
     */
    @Override
    public void UpdateTweetCache(TwitterStatus status, boolean deleteStatus) {

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#onJumpToTop()
     */
    @Override
    public void onJumpToTop() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#clearLocalCache()
     */
    @Override
    public void clearLocalCache() {
        mUser = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.tweetlanes.android.core.view.BaseLaneFragment#getContentToCache()
     */
    @Override
    public String getDataToCache() {
        return null;
    }
}
