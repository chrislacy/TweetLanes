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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.twitter4j.DirectMessage;
import org.twitter4j.ResponseList;
import org.twitter4j.User;

public class TwitterDirectMessages {

	class Conversation implements Comparable<Conversation>{

		/*
		 * 
		 */
		public Conversation(Long otherUserId) {
			mMessages = new ArrayList<TwitterDirectMessage>();
			mOtherUserId = otherUserId;
		}
		
		/*
		 * 
		 */
		void add(TwitterDirectMessage message) {
			mMessages.add(message);
		}

		/*
		 * 
		 */
		public TwitterDirectMessage getNewest() {
			if (mMessages != null && mMessages.size() > 0) {
				return mMessages.get(0);
			}
			
			return null;
		}
		
		/*
		 * 
		 */
		public TwitterDirectMessage getOldest() {
			if (mMessages != null && mMessages.size() > 0) {
				return mMessages.get(mMessages.size()-1);
			}
			
			return null;
		}
		
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(Conversation other){
		      
			TwitterDirectMessage thisMostRecent = getNewest();
			TwitterDirectMessage otherMostRecent = other.getNewest();
			
	        if (thisMostRecent != null && otherMostRecent != null && thisMostRecent.getCreatedAt().before(otherMostRecent.getCreatedAt())) {
	        	return 1;
	        }
	        return -1;
		}
		
		/*
		 * 
		 */
		void sort() {
			Collections.sort(mMessages);
		}
		
		/*
		 * 
		 */
		void removeDuplicates() {
			int size = mMessages.size();
			if (size != 0) {
				TwitterDirectMessage previous = null;
				for (Iterator<TwitterDirectMessage> it = mMessages.iterator(); it.hasNext(); ) {
					TwitterDirectMessage current = it.next();
					if (previous != null && current.getId() == previous.getId()) {
						it.remove();
					} else {
						previous = current;
					}
				}
			}
		}
		
		/*
		 * 
		 */
		public ArrayList<TwitterDirectMessage> getMessages() {
			return mMessages;
		}
		
		/*
		 * 
		 */
		private ArrayList<TwitterDirectMessage> mMessages;
		Long mOtherUserId;
	}

	
	/*
	 * 
	 */
	public interface AddUserCallback {
		
		public void addUser(User user);
	}
	
	/*
	 * 
	 */
	public TwitterDirectMessages(long messageOwnerId) {
		mConversations = new ArrayList<Conversation>();
		mConversationHandle = new TwitterDirectMessagesHandle(messageOwnerId, null);
		mMessageOwnerId = messageOwnerId;
	}
	
	public TwitterDirectMessages(TwitterDirectMessages another, Long otherUserId) {
		mConversations = new ArrayList<Conversation>(another.mConversations);
		mConversationHandle = new TwitterDirectMessagesHandle(another.mMessageOwnerId, otherUserId);
		mMessageOwnerId = another.mMessageOwnerId;
	}
	
	/*
	 * 
	 */
	public void add(ResponseList<DirectMessage> sentDirectMessages, 
					ResponseList<DirectMessage> receivedDirectMessages, 
					AddUserCallback addUserCallback) {
		
		for (int i = 0; i < 2; i++) {
		
			ResponseList<DirectMessage> directMessages = i == 0 ? sentDirectMessages : receivedDirectMessages; 
			//Log.d("TweetALib", i == 0 ? "SENT MESSAGES" : "RECEIVED MESSAGES");
			for (DirectMessage directMessage : directMessages) {
			    
				User otherUser = getOtherParty(directMessage);
				if (otherUser != null) {
				    if (addUserCallback != null) {
				    	if (otherUser != null) {
				    		addUserCallback.addUser(otherUser);
				    	}
				    }
				    
				    add(new TwitterDirectMessage(directMessage, otherUser));
				}
	        }
		}
		
		// Sort the messages in the conversation first up
		for (Conversation conversation : mConversations) {
			conversation.sort();
			conversation.removeDuplicates();
		}
		
		sort();
	}
	
	/*
	 * 
	 */
	public void add(DirectMessage directMessage) {
		User otherUser = getOtherParty(directMessage);
		if (otherUser != null) {
		    add(new TwitterDirectMessage(directMessage, otherUser));
		}
	}
	
	/*
	 * 
	 */
	public void add(TwitterDirectMessage message) {
		
		Conversation conversation = getConversationForMessage(message);
		if (conversation == null) {
			conversation = new Conversation(message.getOtherUserId());
			mConversations.add(conversation);
		}
		
		conversation.add(message);
		//Log.d("TweetALib", "Message type: " + (message.getMessageType() == MessageType.SENT ? "Sent" : "received") + ", message: " + message.getText());
	}
	
	/*
	 * 
	 */
	Conversation getConversationForMessage(TwitterDirectMessage message) {
		
		Conversation result = null;
		
		for (Conversation conversation : mConversations) {
			TwitterDirectMessage mostRecent = conversation.getNewest();
			if (mostRecent != null) {
				if (mostRecent.getOtherUserId() == message.getOtherUserId()) {
					result = conversation;
					break;
				}
			}
			
		}
		
		return result;
	}
	
	/*
	 * 
	 */
	private User getOtherParty(DirectMessage directMessage) {
		if (mMessageOwnerId != directMessage.getRecipientId()) {
			return directMessage.getRecipient();
		}
		
		return directMessage.getSender();
	}
	
	/*
	 * 
	 */
	public ArrayList<TwitterDirectMessage> getList(TwitterDirectMessagesHandle handle) {
		
		if (handle.getKey().equals(mConversationHandle.getKey()) && handle.mOtherUserId == null) {
			
			ArrayList<TwitterDirectMessage> topMessages = new ArrayList<TwitterDirectMessage>();
			for (int i = 0; i < mConversations.size(); i++) {
				TwitterDirectMessage message = mConversations.get(i).getNewest();
				if (message != null) {
					topMessages.add(message);
				}
			}
			return topMessages;
		}
		
		for (Conversation conversation : mConversations) {
			if (conversation.mOtherUserId.longValue() == handle.mOtherUserId.longValue()) {
				return conversation.getMessages();
			}
		}
		
		return null;
	}
	
	/*
	 * 
	 */
	public TwitterDirectMessage getFirstMessage(TwitterDirectMessagesHandle handle) {
		ArrayList<TwitterDirectMessage> messages = getList(handle);
		if (messages != null) {
			return messages.get(0);
		}
		
		return null;
	}
	
	/*
	 * 
	 */
	public TwitterDirectMessage getMessage(TwitterDirectMessagesHandle handle, int position) {
		ArrayList<TwitterDirectMessage> messages = getList(handle);
		if (messages != null && position < messages.size()) {
			return messages.get(position);
		}
		
		return null;
	}
	
	/*
	 * 
	 */
	public int getConversationCount() {
		if (mConversations != null) {
			return mConversations.size();
		}
		
		return 0;
	}
	
	/*
	 * 
	 */
	Conversation getConversation(int index) {
		if (mConversations != null) {
			return mConversations.get(index);
		}
		return null;
	}
	
	/*
	 * 
	 */
	public Long getNewestDirectMessageId() {
		if (mConversations != null && mConversations.size() > 0) {
			return mConversations.get(0).getNewest().getId();
		}
		return null;
	}
	
	/*
	 * 
	 */
	public Long getOldestDirectMessageId() {
		if (mConversations != null && mConversations.size() > 0) {
			return mConversations.get(mConversations.size()-1).getOldest().getId();
		}
		
		return null;
	}

	/*
	 * 
	 */
	public void sort() {
		Collections.sort(mConversations);
	}
	
	/*
	 * 
	 */
	private long mMessageOwnerId;
	private ArrayList<Conversation> mConversations;
	private TwitterDirectMessagesHandle mConversationHandle;
}
