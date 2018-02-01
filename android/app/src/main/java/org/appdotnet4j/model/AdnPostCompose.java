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

package org.appdotnet4j.model;

import java.io.File;

public class AdnPostCompose {

    public final String mText;
    public String mInReplyTo;
    public final File mMediaFile;

    public AdnPostCompose(String text, Long inReplyTo, File mediaFile) {
        mText = text;
        if (inReplyTo != null) {
            mInReplyTo = inReplyTo.toString();
        }
        mMediaFile = mediaFile;
    }

}
