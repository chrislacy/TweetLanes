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
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/*
 * 
 */
public class InflatedLayoutFragment extends Fragment {

    private static final String KEY_RESOURCE_ID = "LayoutFragment:ResourceId";
    private Integer mLayoutResourceId;
    private Callback mCallback;

    /*
     *
	 */
    public interface Callback {

        public void onCreateView(View view);
    }

    /*
     *
	 */
    public static InflatedLayoutFragment newInstance(int layoutResourceId) {
        return newInstance(layoutResourceId, null);
    }

    /*
     *
	 */
    public static InflatedLayoutFragment newInstance(int layoutResourceId,
                                                     Callback callback) {
        InflatedLayoutFragment fragment = new InflatedLayoutFragment();
        fragment.mLayoutResourceId = layoutResourceId;
        fragment.mCallback = callback;
        return fragment;
    }

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
        if ((savedInstanceState != null)
                && savedInstanceState.containsKey(KEY_RESOURCE_ID)) {
            mLayoutResourceId = savedInstanceState.getInt(KEY_RESOURCE_ID);
        }

        View result = null;

        if (mLayoutResourceId != null) {
            result = inflater.inflate(mLayoutResourceId, null);
            if (result != null && mCallback != null) {
                mCallback.onCreateView(result);
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null && mLayoutResourceId != null) {
            outState.putInt(KEY_RESOURCE_ID, mLayoutResourceId);
        }
    }

}
