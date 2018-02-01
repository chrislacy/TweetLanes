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

/*
 * From http://stackoverflow.com/a/4463535/328679
 */

package org.tweetalib.android.widget;

import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.widget.TextView;

public class URLSpanNoUnderline extends URLSpan {

    /*
     *
	 */
    private URLSpanNoUnderline(String url) {
        super(url);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.text.style.ClickableSpan#updateDrawState(android.text.TextPaint)
     */
    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setUnderlineText(false);
    }

    /*
     * 
     */
    public static void stripUnderlines(TextView textView) {
        Spannable s = stripUnderlines((Spannable) textView.getText());
        textView.setText(s);
    }

    /*
     * 
     */
    public static Spannable stripUnderlines(Spanned s) {
        return stripUnderlines((Spannable) s);
    }

    /*
     * 
     */
    private static Spannable stripUnderlines(Spannable s) {
        URLSpan[] spans = s.getSpans(0, s.length(), URLSpan.class);
        for (URLSpan span : spans) {
            int start = s.getSpanStart(span);
            int end = s.getSpanEnd(span);
            s.removeSpan(span);
            span = new URLSpanNoUnderline(span.getURL());
            s.setSpan(span, start, end, 0);
        }
        return s;
    }
}
