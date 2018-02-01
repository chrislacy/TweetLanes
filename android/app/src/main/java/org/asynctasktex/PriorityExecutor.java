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

package org.asynctasktex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class PriorityExecutor implements BaseExecutor {

    /*
     *
	 */
    private static class PrioritizedRunnable {
        public final Runnable mRunnable;
        public final int mPriority;

        public PrioritizedRunnable(int priority, Runnable runnable) {
            mPriority = priority;
            mRunnable = runnable;
        }
    }

    /*
	 * 
	 */
    private static class PrioritizedRunnableComparator implements
            Comparator<PrioritizedRunnable> {
        @Override
        public int compare(PrioritizedRunnable left, PrioritizedRunnable right) {
            return left.mPriority < right.mPriority ? 1 : -1;
        }
    }

    private final ArrayList<PrioritizedRunnable> mTasks = new ArrayList<PrioritizedRunnable>();
    private PrioritizedRunnable mActive;

    /*
     * (non-Javadoc)
     * 
     * @see org.asynctasktex.BaseExecutor#execute(int, java.lang.Runnable)
     */
    public synchronized void execute(int priority, final Runnable r) {
        mTasks.add(new PrioritizedRunnable(priority, new Runnable() {
            public void run() {
                try {
                    r.run();
                } finally {
                    scheduleNext();
                }
            }
        }));

        Collections.sort(mTasks, new PrioritizedRunnableComparator());

        execute(r);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Executor#execute(java.lang.Runnable)
     */
    public synchronized void execute(final Runnable r) {

        if (mActive == null) {
            scheduleNext();
        }
    }

    /*
     * 
     */
    synchronized void scheduleNext() {

        if (mTasks.size() > 0) {
            mActive = mTasks.get(0);
            AsyncTaskEx.THREAD_POOL_EXECUTOR.execute(mActive.mRunnable);
            mTasks.remove(0);
        } else {
            mActive = null;
        }
    }
}