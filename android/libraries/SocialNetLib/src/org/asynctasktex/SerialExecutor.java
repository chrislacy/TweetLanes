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

import java.util.ArrayDeque;

class SerialExecutor implements BaseExecutor {
    private final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
    private Runnable mActive;

    @Override
    public synchronized void execute(int priority, Runnable r) {
        execute(r);
    }

    public synchronized void execute(final Runnable r) {
        mTasks.offer(new Runnable() {
            public void run() {
                try {
                    r.run();
                } finally {
                    scheduleNext();
                }
            }
        });
        if (mActive == null) {
            scheduleNext();
        }
    }

    synchronized void scheduleNext() {
        if ((mActive = mTasks.poll()) != null) {
            AsyncTaskEx.THREAD_POOL_EXECUTOR.execute(mActive);
        }
    }

}