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

package net.uncontended.precipice.metrics.tools;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.assertTrue;

public class StrictRecorderTest {

    private final Executor executors = Executors.newCachedThreadPool();
    private final Recorder<BooleanHolder> recorder = new StrictRecorder<>();

    @Test
    public void testThatNoWritesHappenAfterFlip() throws InterruptedException {
        recorder.flip(new BooleanHolder());
        for (int j = 0; j < 5; ++j) {
            final CountDownLatch latch = new CountDownLatch(5);
            for (int i = 0; i < 10; ++i) {
                executors.execute(new Runnable() {
                    @Override
                    public void run() {
                        long permit = recorder.startRecord();
                        BooleanHolder active = recorder.active();
                        LockSupport.parkNanos(10000);
                        active.marker = false;
                        recorder.endRecord(permit);
                        latch.countDown();
                    }
                });
            }
            latch.await();
            BooleanHolder old = recorder.flip(new BooleanHolder());
            old.marker = true;
            LockSupport.parkNanos(1000000);

            assertTrue(old.marker);
        }

    }

    private static class BooleanHolder {
        private volatile boolean marker;
    }
}
