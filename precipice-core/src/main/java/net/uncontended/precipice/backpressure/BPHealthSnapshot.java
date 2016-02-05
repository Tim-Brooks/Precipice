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

package net.uncontended.precipice.backpressure;

public class BPHealthSnapshot {

    public final long total;
    public final long failures;
    public final int failurePercentage;

    public BPHealthSnapshot(long total, long failures) {
        this.total = total;
        this.failures = failures;
        if (total != 0) {
            this.failurePercentage = (int) (100 * failures / total);
        } else {
            this.failurePercentage = 0;
        }
    }

    public int failurePercentage() {
        return failurePercentage;
    }
}
