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

package net.uncontended.precipice;

public class CompletionContext<S extends Failable, T> implements Completable<S, T>, ExecutionContext, Readable<S, T> {

    private final long permits;
    private final long startTime;
    private final Completable<S, T> wrappedCompletable;
    private PrecipiceFunction<S, ExecutionContext> internalCallback;
    private boolean isCompleted = false;
    private S status;
    private T result;
    private Throwable exception;

    public CompletionContext() {
        this(0L);
    }

    public CompletionContext(long permits) {
        this(permits, System.nanoTime());
    }

    public CompletionContext(long permits, long startTime) {
        this(permits, startTime, null);
    }

    public CompletionContext(long startTime, Completable<S, T> wrappedCompletable) {
        this(1L, startTime, wrappedCompletable);
    }

    public CompletionContext(long permits, long startTime, Completable<S, T> wrappedCompletable) {
        this.permits = permits;
        this.startTime = startTime;
        this.wrappedCompletable = wrappedCompletable;
    }

    @Override
    public long startNanos() {
        return startTime;
    }

    @Override
    public long permitCount() {
        return permits;
    }

    @Override
    public boolean complete(S status, T result) {
        this.status = status;
        this.result = result;
        if (!this.isCompleted && internalCallback != null) {
            internalCallback.apply(status, this);
            if (wrappedCompletable != null) {
                wrappedCompletable.complete(status, result);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean completeExceptionally(S status, Throwable exception) {
        this.status = status;
        this.exception = exception;
        if (!this.isCompleted && internalCallback != null) {
            internalCallback.apply(status, this);
            if (wrappedCompletable != null) {
                wrappedCompletable.completeExceptionally(status, exception);
            }
            return true;
        }
        return false;
    }

    @Override
    public Readable<S, T> readable() {
        return this;
    }

    @Override
    public T getResult() {
        return result;
    }

    @Override
    public Throwable getError() {
        return exception;
    }

    @Override
    public S getStatus() {
        return status;
    }

    public void internalOnComplete(PrecipiceFunction<S, ExecutionContext> fn) {
        internalCallback = fn;
    }
}
