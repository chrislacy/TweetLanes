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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tweetlanes.android.core.R;

public class LoadMoreView extends RelativeLayout {

    public LoadMoreView(Context context) {
        super(context);
    }

    public LoadMoreView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LoadMoreView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public enum Mode {
        LOADING, NONE_FOUND, LOAD_MORE, NO_MORE,
    }

    /*
     *
	 */
    public void configure(Mode mode) {

        TextView textView = (TextView) findViewById(R.id.load_more_text);

        switch (mode) {
            case LOADING:
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.load_more_progress);
                progressBar.setVisibility(VISIBLE);
                textView.setText(R.string.loading);
                break;

            case NONE_FOUND:
                textView.setText(R.string.no_content_found);
                break;

            case LOAD_MORE:
                textView.setText(R.string.load_more);
                break;

            case NO_MORE:
                textView.setText(R.string.no_more_content);

        }
    }

}
