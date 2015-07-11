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

package net.uncontended.precipice.core.test_utils;

import net.uncontended.precipice.core.ResilientAction;

import java.util.concurrent.CountDownLatch;

public class TestActions {

    public static ResilientAction<String> blockedAction(final CountDownLatch blockingLatch) {
        return new ResilientAction<String>() {

            @Override
            public String run() throws Exception {
                blockingLatch.await();
                return "Success";
            }
        };
    }

    public static ResilientAction<String> successAction(final long waitTime) {
        return successAction(waitTime, "Success");
    }

    public static ResilientAction<String> successAction(final long waitTime, final String result) {
        return new ResilientAction<String>() {
            @Override
            public String run() throws Exception {
                if (waitTime != 0) {
                    Thread.sleep(waitTime);
                }
                return result;
            }
        };
    }

    public static ResilientAction<String> erredAction(final Exception exception) {
        return new ResilientAction<String>() {
            @Override
            public String run() throws Exception {
                throw exception;
            }
        };
    }
}
