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

import net.uncontended.precipice.Status;

/**
 * Created by timbrooks on 11/16/14.
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
    public boolean deliverError(Throwable error) {
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
