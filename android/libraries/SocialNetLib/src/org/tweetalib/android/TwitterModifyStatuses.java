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

import java.util.HashMap;

import org.asynctasktex.AsyncTaskEx;

import org.tweetalib.android.TwitterConstant.StatusesType;
import org.tweetalib.android.model.TwitterStatus;
import org.tweetalib.android.model.TwitterStatuses;

import org.twitter4j.Twitter;
import org.twitter4j.TwitterException;


public class TwitterModifyStatuses {

	private ModifyStatusesWorkerCallbacks mCallbacks;
	private Integer mModifyStatusesCallbackHandle;
	private HashMap<Integer, FinishedCallback> mFinishedCallbackMap;
	
	/*
	 * 
	 */
	public void clearCallbacks() {
		mFinishedCallbackMap.clear();
	}

	/*
	 * 
	 */
	public interface ModifyStatusesWorkerCallbacks {
		
		public Twitter getTwitterInstance();
	}
	
	/*
	 * 
	 */
	public interface FinishedCallbackInterface {
		
		public void finished(boolean successful, TwitterStatuses statuses, Integer value);
		
	}
	
	/*
	 * 
	 */
	public abstract class FinishedCallback implements FinishedCallbackInterface {
		
		static final int kInvalidHandle = -1; 
		
		public FinishedCallback() {
			mHandle = kInvalidHandle;
		}
		
		void setHandle(int handle) {
			mHandle = handle;
		}
		
		private int mHandle;
	}
	
	/*
	 * 
	 */
	public TwitterModifyStatuses() {
		mFinishedCallbackMap = new HashMap<Integer, FinishedCallback>();
		mModifyStatusesCallbackHandle = 0;
	}
	
	/*
	 * 
	 */
	public void setWorkerCallbacks(ModifyStatusesWorkerCallbacks callbacks) {
		mCallbacks = callbacks;
	}
	
	/*
	 * 
	 */
	
	
	/*
	 * 
	 */
	FinishedCallback getModifyStatusesCallback(Integer callbackHandle) {
		FinishedCallback callback = mFinishedCallbackMap.get(callbackHandle);
		return callback;
	}
	
	/*
	 * 
	 */
	void removeModifyStatusesCallback(FinishedCallback callback) {
		if (mFinishedCallbackMap.containsValue(callback)) {
			mFinishedCallbackMap.remove(callback.mHandle);
		}
	}
	
	/*
	 * 
	 */
	Twitter getTwitterInstance() {
		return mCallbacks.getTwitterInstance();
	}
	
	
	public void cancel(FinishedCallback callback) {
		
		removeModifyStatusesCallback(callback);
	}
	

	/*
	 * 
	 */
	public void setFavorite(TwitterStatus status, boolean isFavorite, FinishedCallback callback) {
		
		setFavorite(new TwitterStatuses(status), isFavorite, callback);
	}
	
	/*
	 * 
	 */
	public void setFavorite(TwitterStatuses statuses, boolean isFavorite, FinishedCallback callback) {
		
		mFinishedCallbackMap.put(mModifyStatusesCallbackHandle, callback);
		new ModifyStatusesTask().execute(AsyncTaskEx.PRIORITY_HIGH, "Set Favorite",
				new ModifyStatusesTaskInput(mModifyStatusesCallbackHandle, StatusesType.SET_FAVORITE, statuses, isFavorite ? 1 : 0));
		mModifyStatusesCallbackHandle += 1;
	}
	
	/*
	 * 
	 */
	class ModifyStatusesTaskInput {
		
		
		public ModifyStatusesTaskInput(Integer callbackHandle, StatusesType statusesType, TwitterStatuses statuses, Integer value) {
			mCallbackHandle = callbackHandle;
			mStatusesType = statusesType;
			mStatuses = new TwitterStatuses(statuses);
			mValue = value;
		}

		Integer mCallbackHandle;
		StatusesType mStatusesType;
		TwitterStatuses mStatuses;
		Integer mValue;
	}
	
	/*
	 * 
	 */
	class ModifyStatusesTaskOutput {
		
		ModifyStatusesTaskOutput(Integer callbackHandle, TwitterStatuses feed, Integer outputValue) {
			mCallbackHandle = callbackHandle;
			mFeed = feed;
			mValue = outputValue;
		}
		
		Integer mCallbackHandle;
		TwitterStatuses mFeed;
		Integer mValue;
	}
	
	/*
	 * 
	 */
	class ModifyStatusesTask extends AsyncTaskEx<ModifyStatusesTaskInput, Void, ModifyStatusesTaskOutput> {

		@Override
		protected ModifyStatusesTaskOutput doInBackground(ModifyStatusesTaskInput... inputArray) {

			TwitterStatuses contentFeed = null;
			ModifyStatusesTaskInput input = inputArray[0];
			Twitter twitter = getTwitterInstance();
			Integer outputValue = null;
			
			if (twitter != null) {
				
				switch (input.mStatusesType) {
					
					case SET_FAVORITE: {
						boolean favorite = input.mValue == 1 ? true : false;
						
						if (input.mStatuses != null) {
							
							contentFeed = new TwitterStatuses();
							
							for (int i = 0; i < input.mStatuses.getStatusCount(); i++) {
								TwitterStatus twitterStatus = input.mStatuses.getStatus(i);
								if (twitterStatus.mIsFavorited != favorite) {
									try {
										org.twitter4j.Status status;
										if (favorite) {
											status = twitter.createFavorite(twitterStatus.mId);
										} else {
											status = twitter.destroyFavorite(twitterStatus.mId);
										}
										
										// Yuck: See the comment for TwitterStatus.setFavorite() for reasons for this
										twitterStatus = new TwitterStatus(status);
										twitterStatus.setFavorite(favorite);
										
										contentFeed.add(twitterStatus);
										outputValue = input.mValue;
										
									} catch (TwitterException e) {
										// we might get errors setting the favorite state to the same value again.
										// Just ignore those ones...
									}
								}
							}
							
						} 
						
						break;
					}
					
				}
			}

			return new ModifyStatusesTaskOutput(input.mCallbackHandle, contentFeed, outputValue);
		}

		@Override
		protected void onPostExecute(ModifyStatusesTaskOutput output) {
			
			FinishedCallback callback = getModifyStatusesCallback(output.mCallbackHandle);
			if (callback != null) {
				callback.finished(true, output.mFeed, output.mValue);
				removeModifyStatusesCallback(callback);
			}

			super.onPostExecute(output);
		}
	}
	
}
