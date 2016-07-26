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

package net.uncontended.precipice.metrics.latency;

import org.HdrHistogram.Histogram;

public class ConcurrentHistogram<T extends Enum<T>> extends BaseHDRHistogram<T> {

    public ConcurrentHistogram(Class<T> clazz, long highestTrackableValue, int numberOfSignificantValueDigits) {
        super(clazz, new Histogram[clazz.getEnumConstants().length]);
        T[] enumConstants = clazz.getEnumConstants();
        for (int i = 0; i < enumConstants.length; ++i) {
            histograms[i] = new org.HdrHistogram.ConcurrentHistogram(highestTrackableValue, numberOfSignificantValueDigits);
        }
    }

}
