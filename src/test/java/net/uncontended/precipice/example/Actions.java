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

package net.uncontended.precipice.example;

import net.uncontended.precipice.ResilientAction;

import java.util.concurrent.CountDownLatch;

public class Actions {

    public static ResilientAction<Integer> successAction() {
        return new SuccessAction();
    }

    public static ResilientAction<Integer> errorAction() {
        return new ErrorAction();
    }

    public static ResilientAction<Integer> timeoutAction(CountDownLatch latch) {
        return new TimeoutAction(latch);
    }

    private static class SuccessAction implements ResilientAction<Integer> {

        @Override
        public Integer run() throws Exception {
            return 8 * 8;
        }
    }

    private static class ErrorAction implements ResilientAction<Integer> {

        @Override
        public Integer run() throws Exception {
            throw new RuntimeException("Action failed.");
        }
    }

    private static class TimeoutAction implements ResilientAction<Integer> {

        private final CountDownLatch latch;

        public TimeoutAction(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public Integer run() throws Exception {
            latch.await();
            return 1;
        }
    }
}
