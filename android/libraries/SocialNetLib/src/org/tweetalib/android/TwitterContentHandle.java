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

package org.tweetalib.android;

public class TwitterContentHandle extends TwitterContentHandleBase {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 231396385372202288L;
	
	public TwitterContentHandle(TwitterContentHandleBase contentHandleBase, String screenName) {
		this(contentHandleBase, screenName, null);
	}
	
	public TwitterContentHandle(TwitterContentHandleBase contentHandleBase, String screenName, String identifier) {
		
		super(contentHandleBase);
		mScreenName = screenName;
		mScreenNameLower = screenName.toLowerCase();
		mIdentifier = identifier != null ? identifier : "default";
	}

	public String getKey() {
		String key =  mScreenNameLower + "_" + getEnumsAsString() + "_" + mIdentifier;
		return key;
	}
	
	// TODO: Look at this and ensure there aren't savings to be had
	String getEnumsAsString() {
		String result = "" + mContentType;
		if (mStatusType != null) {
			result += "_" + mStatusType;
		}
		if (mStatusesType != null) {
			result += "_" + mStatusesType;
		}
		if (mUsersType != null) {
			result += "_" + mUsersType;
		}
		return result;
	}
	
	@Override
	public String getTypeAsString() {
		String result = super.getTypeAsString();
		if (mIdentifier != null) {
			result += "_" + mIdentifier;
		}
		return result;
	}
	
	public String getScreenName()		{ return mScreenName; }
	public String getIdentifier()		{ return mIdentifier; }
	
	private String mScreenNameLower;
	private String mScreenName;
	private String mIdentifier;
	
}
