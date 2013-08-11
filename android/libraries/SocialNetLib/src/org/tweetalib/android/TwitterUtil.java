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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.appdotnet4j.model.AdnPost;
import org.tweetalib.android.model.TwitterMediaEntity;
import twitter4j.MediaEntity;
import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;

import com.twitter.Autolink;

public class TwitterUtil {

    private static Autolink mAutoLink;

    private static void initCommon() {

        if (mAutoLink == null) {
            mAutoLink = new Autolink();
        }
    }

    /*
	 * 
	 */
    public static String stripMarkup(String text) {
        return text != null ? android.text.Html.fromHtml(text).toString()
                : null;
    }

    public static String getTextMarkup(String text, URLEntity[] urlEntities) {
        return getStatusMarkup(text, null, null, urlEntities);
    }

    /*
     * return the markup for a status, which replaces t.co/ links with the
     * visible links
     */
    public static String getStatusMarkup(Status status,
            TwitterMediaEntity twitterMediaEntity) {
        return getStatusMarkup(status.getText(), twitterMediaEntity,
                status.getMediaEntities(), status.getURLEntities());
    }

    /*
	 * 
	 */
    public static String getStatusMarkup(AdnPost post,
            TwitterMediaEntity mediaEntity) {
        return getStatusMarkup(post.mText, mediaEntity, null, null);
    }

    /*
	 * 
	 */
    public static String getStatusMarkup(String statusText,
            TwitterMediaEntity twitterMediaEntity, MediaEntity[] mediaEntities,
            URLEntity[] urlEntities) {

        initCommon();

        return mAutoLink.autoLinkAll(statusText,
                twitterMediaEntity, mediaEntities, urlEntities);
    }

    /*
	 * 
	 */
    public static String[] getUserMentions(UserMentionEntity[] entities) {
        if (entities != null && entities.length > 0) {

            ArrayList<String> arrayList = new ArrayList<String>();

            for (UserMentionEntity entity : entities) {
                if (entity.getScreenName() != null) {
                    arrayList.add(entity.getScreenName());
                }
            }

            if (arrayList.size() > 0) {
                String[] stringArray = new String[arrayList.size()];
                arrayList.toArray(stringArray);
                return stringArray;
            }
        }

        return null;
    }

    /*
     * public static String getStatusMarkup(String status, URLEntity[]
     * urlEntities) {
     * 
     * String result = null; if (urlEntities.length > 0) { int lastEnd = -1; for
     * (int i = 0; i < urlEntities.length; i++) { URLEntity urlEntity =
     * urlEntities[i]; int start = urlEntity.getStart(); int end =
     * urlEntity.getEnd(); int statusLength = status.length();
     * 
     * // TODO: Problem with this tweet:
     * https://twitter.com/#!/Proto3000_RP/status/177144322645434368 // Have to
     * keep an eye on this solution if (end > statusLength) { int diff = end -
     * statusLength; start -= diff; end -= diff; }
     * 
     * if (start > 0) { if (result == null) { result = status.substring(0,
     * start); } else if (lastEnd > -1) { if (lastEnd < start) { result +=
     * status.substring(lastEnd, start); } else { // TODO: Handle this, such as
     * for id 179136028555231232 } } } String link = "<a href=\"" +
     * urlEntity.getExpandedURL() + "\">" + urlEntity.getDisplayURL() + "</a>";
     * result += link; String after; if (i == urlEntities.length - 1) { if (end
     * <= statusLength) { after = status.substring(end, statusLength); } else {
     * after = ""; // should never get here } } else { after =
     * status.substring(end, urlEntities[i+1].getStart()); } result += after;
     * lastEnd = end; } } else { result = status; } return result; }
     */

    public static Query updateQueryWithPaging(Query query, Paging paging) {

        if (paging.getMaxId() > -1) {
            query.setMaxId(paging.getMaxId());
        }
        if (paging.getSinceId() > -1) {
            query.setSinceId(paging.getSinceId());
        }
        /*
         * if (paging.getPage() != 1 && paging.getPage() != -1) {
         * query.setPage(paging.getPage()); }
         */

        return query;
    }

    /*
	 * 
	 */
    public static Date iso6801StringToDate(String iso8601string)
            throws ParseException {

        String s = iso8601string.replace("Z", "+00:00");
        try {
            s = s.substring(0, 22) + s.substring(23);
        } catch (IndexOutOfBoundsException e) {
            throw new ParseException("Invalid length", 0);
        }
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);
    }

}
