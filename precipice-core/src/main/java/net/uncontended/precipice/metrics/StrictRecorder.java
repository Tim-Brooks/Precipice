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

package net.uncontended.precipice.metrics;

import org.HdrHistogram.WriterReaderPhaser;

public class StrictRecorder<V> extends Recorder<V> {

    private final WriterReaderPhaser phaser = new WriterReaderPhaser();

    public StrictRecorder(V initialValue, long nanoTime) {
        activeHolder.object = initialValue;
        activeHolder.endNanos = nanoTime;
    }

    @Override
    public long startRecord() {
        return phaser.writerCriticalSectionEnter();
    }

    @Override
    public void endRecord(long permit) {
        phaser.writerCriticalSectionExit(permit);
    }

    @Override
    public synchronized V flip(long nanoTime, V newValue) {
        phaser.readerLock();

        try {
            Holder<V> old = this.activeHolder;
            Holder<V> newHolder = this.inactiveHolder;
            newHolder.object = newValue;
            newHolder.startNanos = nanoTime;
            old.endNanos = nanoTime;
            this.activeHolder = newHolder;
            inactiveHolder = old;
            phaser.flipPhase(500000L);
            return old.object;
        } finally {
            phaser.readerUnlock();
        }
    }

}
