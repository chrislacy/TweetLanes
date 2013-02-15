/*
 * Copyright (C) 2013 Chris Lacy
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

package org.tweetalib.android.model;

import java.io.File;

import org.appdotnet4j.model.AdnPostCompose;
import org.twitter4j.StatusUpdate;

public class TwitterStatusUpdate {

	public TwitterStatusUpdate(String status, Long inReplyToStatusId, File mediaFile) {
		mStatus = status;
		mInReplyToStatusId = inReplyToStatusId;
		mMediaFile = mediaFile;
	}
	
	public TwitterStatusUpdate(String status, Long inReplyToStatusId) {
		this(status, inReplyToStatusId, null);
	}
	
	public TwitterStatusUpdate(String status) {
		this(status, null, null);
	}
	
	public StatusUpdate getT4JStatusUpdate() {
		StatusUpdate statusUpdate = new StatusUpdate(mStatus);
		
		if (mInReplyToStatusId != null) {
			statusUpdate.setInReplyToStatusId(mInReplyToStatusId);
		}
		
		if (mMediaFile != null) {
			statusUpdate.setMedia(mMediaFile);
		}
		
		if (mMediaFilePath != null) {
			mMediaFile = new File(mMediaFilePath);
			statusUpdate.setMedia(mMediaFile);
		}
		
		return statusUpdate;
	}
	
	public AdnPostCompose getAdnComposePost() {
		AdnPostCompose statusUpdate = new AdnPostCompose(mStatus, mInReplyToStatusId);
		return statusUpdate;
	}
	
	public File getMediaFile()	{
		return mMediaFile;
	}
	
	public String getMediaFilePath() {
		return mMediaFilePath;
	}
	
	public void setMediaFilePath(String mediaFilePath) {
		mMediaFilePath = mediaFilePath;
	}
	
	String mStatus;
	Long mInReplyToStatusId;
	File mMediaFile;
	String mMediaFilePath;
}
