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

import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.threadpool.test_utils.TestCallable;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ThreadPoolTaskTest {

    @Mock
    private Eventual<Status, String> eventual;

    private ThreadPoolTask<String> task;

    @Test
    public void ensureThatTimeoutsAreSetupCorrectly() {
        long millisRelativeTimeout = 10L;
        long nanosStart = 0L;
        task = new ThreadPoolTask<>(TestCallable.success("Success"), eventual, millisRelativeTimeout, nanosStart);

        assertEquals(millisRelativeTimeout, task.millisRelativeTimeout);
        assertEquals(millisRelativeTimeout, task.getMillisRelativeTimeout());
        assertEquals(nanosStart + TimeUnit.MILLISECONDS.toNanos(millisRelativeTimeout), task.nanosAbsoluteTimeout);
    }
}
