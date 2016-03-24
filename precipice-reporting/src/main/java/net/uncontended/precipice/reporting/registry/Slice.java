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
 */

package net.uncontended.precipice.reporting.registry;

import net.uncontended.precipice.Failable;

public class Slice<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {
    public final Class<Result> resultClazz;
    public final Class<Rejected> rejectedClazz;

    public final long[] totalResultCounts;
    public final long[] resultCounts;

    public final long[] totalRejectedCounts;
    public final long[] rejectedCounts;

    public long startEpoch = -1;
    public long endEpoch = -1;

    public Slice(Class<Result> resultClazz, Class<Rejected> rejectedClazz) {
        this.resultClazz = resultClazz;
        this.rejectedClazz = rejectedClazz;
        int resultLength = resultClazz.getEnumConstants().length;
        int rejectedLength = rejectedClazz.getEnumConstants().length;

        this.totalResultCounts = new long[resultLength];
        this.resultCounts = new long[resultLength];
        this.totalRejectedCounts = new long[rejectedLength];
        this.rejectedCounts = new long[rejectedLength];
    }
}
