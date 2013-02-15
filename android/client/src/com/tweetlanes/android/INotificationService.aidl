
package com.tweetlanes.android;

interface INotificationService {
	int getMentions(in long[] account_ids, in long[] max_ids);
	boolean isMentionsRefreshing();
	boolean hasActivatedTask();
	boolean test();
	boolean startAutoRefresh();
	void stopAutoRefresh();
	void shutdownService();
	void clearNotification(int id);
}
