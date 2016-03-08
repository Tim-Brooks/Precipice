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
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ThreadPoolTimeoutTest {

    @Test
    public void taskCancelledIfTimeout() {
        CancellableTask<TimeoutableResult, String> task = mock(CancellableTask.class);
        ThreadPoolTimeout<String> timeout =  new ThreadPoolTimeout<>(task);

        timeout.timeout();

        verify(task).cancel(eq(TimeoutableResult.TIMEOUT), any(PrecipiceTimeoutException.class));
    }

}
