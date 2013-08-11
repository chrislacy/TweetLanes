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

package org.appdotnet4j.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AdnInteractions {

    private ArrayList<AdnInteraction> mInteractions;

    public AdnInteractions(String jsonAsString) {

        mInteractions = new ArrayList<AdnInteraction>();

        try {
            JSONArray jsonArray = new JSONObject(jsonAsString).getJSONArray("data");
            for (int i = 0; i < jsonArray.length(); i++) {
                String listString = jsonArray.getString(i);
                AdnInteraction interaction = new AdnInteraction(listString);
                if (!interaction.mAction.equals("reply")) {
                    mInteractions.add(interaction);
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (mInteractions.size() == 0) {
            mInteractions = null;
        }
    }

    public AdnPosts getAsPosts() {
        AdnPosts posts = new AdnPosts();
        if (mInteractions != null) {
            for (AdnInteraction interaction : mInteractions) {
                if (interaction.mPosts != null && interaction.mPosts.mPosts != null) {
                    posts.mPosts.addAll(interaction.mPosts.mPosts);
                }
            }
        }

        return posts;
    }
}
