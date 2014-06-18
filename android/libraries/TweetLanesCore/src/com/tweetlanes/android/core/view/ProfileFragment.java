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

import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
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

//        SocialNetConstant.Type socialNetType = SocialNetConstant.Type.Twitter;

        if (getArguments().getString("userId") != null) {
            Long userId = Long.parseLong(getArguments().getString("userId"));
            mUser = TwitterManager.get().getUser(userId);
//            if (mUser != null) {
//                socialNetType = mUser.getSocialNetType();
//            }
        }

        mProfileView = inflater.inflate(R.layout.profile, null);
//                socialNetType == SocialNetConstant.Type.Appdotnet
//                        ? R.layout.profile_adn : R.layout.profile, null
//        );

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
                                    && mFollowsLoggedInUser) {
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
        Log.d("TweetLanes", "0");
        TextView fullNameTextView = (TextView) mProfileView
                .findViewById(R.id.fullNameTextView);
        TextView followingTextView = (TextView) mProfileView
                .findViewById(R.id.followState);
        TextView descriptionTextView = (TextView) mProfileView
                .findViewById(R.id.bioTextView);
        TextView link = (TextView) mProfileView
                .findViewById(R.id.link);
        mFriendshipButton = (Button) mProfileView
                .findViewById(R.id.friendship_button);
        TextView location = (TextView) mProfileView.findViewById(R.id.location);

        TableLayout detailsLayout = (TableLayout) mProfileView
                .findViewById(R.id.details_table_layout);
        TextView tweetCount = (TextView) mProfileView
                .findViewById(R.id.tweetCountLabel);
        TextView followingCount = (TextView) mProfileView
                .findViewById(R.id.followingCountLabel);
        TextView followersCount = (TextView) mProfileView
                .findViewById(R.id.followersCountLabel);
        TextView favoritesCount = (TextView) mProfileView
                .findViewById(R.id.favorites_count);


        if (mUser != null) {
            // user photo
            ImageView avatar = (ImageView) mProfileView
                    .findViewById(R.id.profileImage);
            String imageUrl = mUser
                    .getProfileImageUrl(
                            TwitterManager.ProfileImageSize.ORIGINAL);
            UrlImageViewHelper.setUrlDrawable(avatar, imageUrl,
                    R.drawable.ic_contact_picture);

//            // cover image (app.net)
//            ImageView coverImage = (ImageView) mProfileView
//                    .findViewById(R.id.coverImage);
//            if (coverImage != null) {
//                String url = mUser.getCoverImageUrl();
//                if (url != null) {
//                    UrlImageViewHelper.setUrlDrawable(coverImage, url,
//                            R.drawable.ic_contact_picture);
//                }
//            }

            // full name
            fullNameTextView.setText(mUser.getName());
            // verified account?
            if (mUser.getVerified()) {
                if (android.os.Build.VERSION.SDK_INT
                        >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    fullNameTextView
                            .setCompoundDrawablesRelativeWithIntrinsicBounds(
                                    R.drawable.ic_verified, 0, 0, 0);
                } else {
                    fullNameTextView
                            .setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.ic_verified, 0, 0, 0);
                }
            }

            // follows me?
            if (mFollowsLoggedInUser != null && mFollowsLoggedInUser) {
                followingTextView.setText(R.string.follows_you);
            } else {
                followingTextView.setText(null);
            }

            // biography
            URLEntity[] urlEntities = mUser.getUrlEntities();
            String description = mUser.getDescription();
            if (description != null) {
                String descriptionMarkup = TwitterUtil.getTextMarkup(description, urlEntities);
                descriptionTextView.setText(Html.fromHtml(descriptionMarkup
                        + " "));
                descriptionTextView.setMovementMethod(LinkMovementMethod
                        .getInstance());
                URLSpanNoUnderline.stripUnderlines(descriptionTextView);
            } else {
                descriptionTextView.setVisibility(View.GONE);
            }

            // link
            if (mUser.getUrl() != null) {
                String urlMarkup = TwitterUtil.getTextMarkup(mUser.getUrl(), urlEntities);
                link.setText(Html.fromHtml(urlMarkup + ""));
                link.setMovementMethod(LinkMovementMethod.getInstance());
                URLSpanNoUnderline.stripUnderlines(link);
            } else {
                link.setVisibility(View.GONE);
            }

            // location
            if (mUser.getLocation() != null) {
                location.setText(mUser.getLocation());
            } else {
                location.setVisibility(View.GONE);
            }

            // twitter stats
            detailsLayout.setVisibility(View.VISIBLE);
            tweetCount.setText(Util.getPrettyCount(mUser.getStatusesCount()));
            followingCount
                    .setText(Util.getPrettyCount(mUser.getFriendsCount()));
            followersCount.setText(Util.getPrettyCount(mUser
                    .getFollowersCount()));
            if (favoritesCount != null) {
                favoritesCount.setText(Util.getPrettyCount(mUser
                        .getFavoritesCount()));
            }

            // follow/unfollow button
            configureFriendshipButtonVisibility(mLoggedInUserFollows);

        } else {
            fullNameTextView.setText(null);
            followingTextView.setText(null);
            descriptionTextView.setText(null);

            detailsLayout.setVisibility(View.GONE);
            location.setVisibility(View.GONE);
            mFriendshipButton.setVisibility(View.GONE);
        }
    }

    private void configureFriendshipButtonVisibility(Boolean loggedInUserFollows) {

        if (loggedInUserFollows == null) {
            mFriendshipButton.setVisibility(View.GONE);
        } else {
            mFriendshipButton.setVisibility(View.VISIBLE);
            mFriendshipButton
                    .setText(loggedInUserFollows ? R.string.action_unfollow
                            : R.string.action_follow);
            mFriendshipButton.setOnClickListener(mFrienshipButtonListener);
        }
    }

    private final OnClickListener mFrienshipButtonListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            if (mUser != null && mLoggedInUserFollows != null
                    && mUpdateFriendshipFinishedCallback == null) {
                String loggedInUserScreenName = getBaseLaneActivity().getApp()
                        .getCurrentAccountScreenName();

                final boolean willCreateFriendship = !mLoggedInUserFollows;
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
