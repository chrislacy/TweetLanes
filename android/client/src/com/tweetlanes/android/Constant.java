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

package com.tweetlanes.android;

import android.widget.Toast;

public class Constant {

    // public static final SocialNetConstant.Type SOCIAL_NET_TYPE =
    // SocialNetConstant.Type.Twitter;

    /*
     * App Version info
     */
    public static final int SHARED_PREFERENCES_VERSION = 1;

    /*
     * Configuration values
     */
    public static final boolean ENABLE_PROFILE_IMAGES = false;
    public static boolean ENABLE_CRASH_TRACKING = true;
    public static final int CACHE_VERSION = 4;
    public static boolean ENABLE_STATUS_CACHING = true;
    public static boolean UPDATE_CACHED_STATUSES = true;

    public static final String DIR_NAME_PROFILE_IMAGES = "profile_images";
    public static final String DIR_NAME_CACHED_THUMBNAILS = "cached_thumbnails";

    /*
     * App Constants
     */
    public static final int REFRESH_LISTS_WAIT_TIME = 1000 * 30;
    public static final int DEFAULT_TOAST_DISPLAY_TIME = Toast.LENGTH_LONG;
    public static final int RESTORE_SAVED_DRAFT_SECONDS = 60 * 5;

    /*
	 *
	 */
    // Create a new key/secret here: https://dev.twitter.com/apps
    public static final String TWITTER_CONSUMER_KEY = "PiE6GzsRZw4xAXzSAJAJg";
    public static final String TWITTER_CONSUMER_SECRET = "YCHowuIwy8mFhSVUwOZvDXqssC58rw8NTN9LcttFyak";
    public static final String APPDOTNET_CONSUMER_KEY = "kKWVXd8vAVS5Va7uf58zGBvenmLQpe49";
    public static final String APPDOTNET_CONSUMER_SECRET = "gyVCKtAYHpkpssd6u6j4CMaQSa6xJwzt";

    public static final String REQUEST_URL = "https://api.twitter.com/oauth/request_token";
    public static final String ACCESS_URL = "https://api.twitter.com/oauth/access_token";
    public static final String AUTHORIZE_URL = "https://api.twitter.com/oauth/authorize";

    public static final String OAUTH_CALLBACK_SCHEME = "x-oauthflow-twitter";
    public static final String OAUTH_CALLBACK_HOST = "callback";
    public static final String OAUTH_CALLBACK_URL = OAUTH_CALLBACK_SCHEME
            + "://" + OAUTH_CALLBACK_HOST;

    /*
	 *
	 */
    public static final long USER_ID_CHRISMLACY = 195348872;
    public static final long USER_ID_TWEETLANES = 482651243;
    public static final long USER_ID_LACYMUFC = 377646103;

    // Note:
    // User == the person who signed in.
    // Profile == any other account on Twitter
    public enum LaneType {
        DIRECT_MESSAGES, USER_PROFILE, USER_PROFILE_TIMELINE, USER_HOME_TIMELINE, USER_LIST_TIMELINE, USER_MENTIONS, USER_FAVORITES, RETWEETS_OF_ME, PROFILE_PROFILE, // ahem
        PROFILE_PROFILE_TIMELINE, // yes, i know
        PROFILE_MENTIONS, PROFILE_FAVORITES, FRIENDS, FOLLOWERS, SEARCH_TERM, SEARCH_PERSON, STATUS_SPOTLIGHT, STATUS_CONVERSATION, STATUS_RETWEETED_BY, GLOBAL_FEED, MAX,
    }

    /*
	 *
	 */
    public enum SystemEvent {
        // OAuthLoginStateChange,
        VOLUME_UP_KEY_DOWN, VOLUME_DOWN_KEY_DOWN, FORCE_FRAGMENT_PAGER_ADAPTER_REFRESH, RESTART_APP, DISPLAY_TOAST,
    }

    /*
	 *
	 */
    public static final int REQUEST_CODE_IMAGE_PICKER = 12345;
    public static final int REQUEST_CODE_CAMERA = 12346;

    /*
	 *
	 */
    public static final String CRITTERCISM_APP_ID = "4f6d53e0b093157b43000033";

    public static final String JPEG_FILE_PREFIX = "IMG_";
    public static final String JPEG_FILE_SUFFIX = ".jpg";

}
