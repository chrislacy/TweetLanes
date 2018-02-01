package org.appdotnet4j.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: Jason
 * Date: 3/5/13
 * Time: 12:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class AdnMedia {
    public String mThumbnailUrl;
    public String mUrl;
    public String mExpandedUrl;

    public AdnMedia(String json) {
        try {
            JSONObject object = new JSONObject(json);
            mThumbnailUrl = object.getString("thumbnail_url");
            mUrl = object.getString("url");
            if (object.has("embeddable_url")) {
                mExpandedUrl = object.getString("embeddable_url");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        JSONObject object = new JSONObject();
        try {
            object.put("url", mUrl);
            object.put("thumbnail_url", mThumbnailUrl);
            object.put("embeddable_url", mExpandedUrl);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object.toString();
    }
}
