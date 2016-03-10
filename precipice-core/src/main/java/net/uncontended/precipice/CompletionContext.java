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

public class CompletionContext<Result extends Failable, V> implements Completable<Result, V>, ExecutionContext,
        ResultView<Result, V> {

    private final long permits;
    private final long startTime;
    private final Completable<Result, V> wrappedCompletable;
    private PrecipiceFunction<Result, ExecutionContext> internalCallback;
    private boolean isCompleted = false;
    private Result result;
    private V value;
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

    public CompletionContext(long startTime, Completable<Result, V> wrappedCompletable) {
        this(1L, startTime, wrappedCompletable);
    }

    public CompletionContext(long permits, long startTime, Completable<Result, V> wrappedCompletable) {
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
    public boolean complete(Result result, V value) {
        this.result = result;
        this.value = value;
        if (!this.isCompleted && internalCallback != null) {
            internalCallback.apply(result, this);
            if (wrappedCompletable != null) {
                wrappedCompletable.complete(result, value);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean completeExceptionally(Result result, Throwable exception) {
        this.result = result;
        this.exception = exception;
        if (!this.isCompleted && internalCallback != null) {
            internalCallback.apply(result, this);
            if (wrappedCompletable != null) {
                wrappedCompletable.completeExceptionally(result, exception);
            }
            return true;
        }
        return false;
    }

    @Override
    public ResultView<Result, V> resultView() {
        return this;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public Throwable getError() {
        return exception;
    }

    @Override
    public Result getResult() {
        return result;
    }

    public void internalOnComplete(PrecipiceFunction<Result, ExecutionContext> fn) {
        internalCallback = fn;
    }
}
