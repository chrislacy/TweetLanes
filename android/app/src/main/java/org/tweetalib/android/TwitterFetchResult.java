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

public class TwitterFetchResult {

    private final boolean mSuccessful;
    final String mErrorMessage;

    /*
     *
	 */
    public TwitterFetchResult(boolean successful, String errorMessage) {
        mSuccessful = successful;
        mErrorMessage = errorMessage;

        if (!mSuccessful && TwitterManager.get() != null && TwitterManager.get().getConnectionStatus() !=
                null) {
            TwitterManager.get().getConnectionStatus().handleError(this);
        }
    }

    /*
     *
	 */
    public boolean isSuccessful() {
        return mSuccessful;
    }

    public String getErrorMessage() {
        return mErrorMessage;
    }
}
