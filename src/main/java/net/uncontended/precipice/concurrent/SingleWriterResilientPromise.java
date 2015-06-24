/*
 * Copyright 2014 Timothy Brooks
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

package net.uncontended.precipice.concurrent;

import net.uncontended.precipice.ResilientAction;
import net.uncontended.precipice.RunService;
import net.uncontended.precipice.SubmissionService;
import net.uncontended.precipice.Status;

/**
 * A class wrapping the result of an action. It is similar to {@link ResilientFuture}.
 * However, unlike a future, a promise can be written to. A promise can only
 * completed once. Once it is completed, all further attempts to update the status
 * should fail.
 * <p/>
 * This class is designed to be written to by ONE thread only. It primarily exists to
 * avoid expensive compareAndSet calls that are required in the multiple writer scenario.
 * This promise is used in synchronous {@link RunService#run(ResilientAction) run)}
 * calls.
 *
 * @param <T> the result returned by the action
 */
public class SingleWriterResilientPromise<T> extends AbstractResilientPromise<T> {
    @Override
    public boolean deliverResult(T result) {
        this.result = result;
        status.set(Status.SUCCESS);
        latch.countDown();
        return true;
    }

    @Override
    public boolean deliverError(Exception error) {
        this.error = error;
        status.set(Status.ERROR);
        latch.countDown();
        return true;
    }

    @Override
    public boolean setTimedOut() {
        status.set(Status.TIMEOUT);
        latch.countDown();
        return true;
    }

}
