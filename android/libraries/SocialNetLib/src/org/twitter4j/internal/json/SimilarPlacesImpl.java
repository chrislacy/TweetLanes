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

package org.twitter4j.internal.json;

import org.tjson.JSONException;
import org.tjson.JSONObject;
import org.twitter4j.Place;
import org.twitter4j.ResponseList;
import org.twitter4j.SimilarPlaces;
import org.twitter4j.TwitterException;
import org.twitter4j.conf.Configuration;
import org.twitter4j.internal.http.HttpResponse;

/**
 * @author Yusuke Yamamoto - yusuke at mac.com
 * @since org.twitter4j 2.1.7
 */
public class SimilarPlacesImpl extends ResponseListImpl<Place> implements SimilarPlaces {
    private static final long serialVersionUID = -7897806745732767803L;
    private final String token;

    SimilarPlacesImpl(ResponseList<Place> places, HttpResponse res, String token) {
        super(places.size(), res);
        this.addAll(places);
        this.token = token;
    }

    /**
     * {@inheritDoc}
     */
    public String getToken() {
        return token;
    }

    /*package*/
    static SimilarPlaces createSimilarPlaces(HttpResponse res, Configuration conf) throws TwitterException {
        JSONObject json = null;
        try {
            json = res.asJSONObject();
            JSONObject result = json.getJSONObject("result");
            return new SimilarPlacesImpl(PlaceJSONImpl.createPlaceList(result.getJSONArray("places"), res, conf), res
                    , result.getString("token"));
        } catch (JSONException jsone) {
            throw new TwitterException(jsone.getMessage() + ":" + json.toString(), jsone);
        }
    }

}
