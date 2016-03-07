/*
 * Copyright 2016 Timothy Brooks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.uncontended.precipice.threadpool;

import net.uncontended.precipice.result.TimeoutableResult;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;
import net.uncontended.precipice.timeout.TimeoutTask;

public class ThreadPoolTimeoutTask<T> implements TimeoutTask {

    private CancellableTask<TimeoutableResult, T> cancellableTask;

    public ThreadPoolTimeoutTask(CancellableTask<TimeoutableResult, T> cancellableTask) {
        this.cancellableTask = cancellableTask;
    }

    @Override
    public void setTimedOut() {
        cancellableTask.cancel(TimeoutableResult.TIMEOUT, new PrecipiceTimeoutException());
    }
}
