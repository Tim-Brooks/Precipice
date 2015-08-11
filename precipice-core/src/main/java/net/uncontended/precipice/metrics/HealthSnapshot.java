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

package net.uncontended.precipice.metrics;

public class HealthSnapshot {
    public final long total;
    public final long failures;
    public final long rejections;

    public HealthSnapshot(long total, long failures, long rejections) {
        this.total = total;
        this.failures = failures;
        this.rejections = rejections;
    }

    public int failurePercentage() {
        if (total != 0) {
            return (int) ((100 * failures) / total);
        } else {
            return 0;
        }
    }

    public double rejectionPercentage() {
        if (total != 0) {
            return (int) ((100 * rejections) / total);
        } else {
            return 0;
        }
    }
}
