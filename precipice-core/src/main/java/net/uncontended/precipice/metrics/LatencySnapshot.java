/*
 * Copyright 2015 Timothy Brooks
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

public class LatencySnapshot {

    public final long latencyMax;
    public final long latency50;
    public final long latency90;
    public final long latency99;
    public final long latency999;
    public final long latency9999;
    public final long latency99999;
    public final double latencyMean;

    public LatencySnapshot(long latency50, long latency90, long latency99, long latency999, long latency9999,
                           long latency99999, long latencyMax, double latencyMean) {
        this.latency50 = latency50;
        this.latency90 = latency90;
        this.latency99 = latency99;
        this.latency999 = latency999;
        this.latency9999 = latency9999;
        this.latency99999 = latency99999;
        this.latencyMax = latencyMax;
        this.latencyMean = latencyMean;
    }

}
