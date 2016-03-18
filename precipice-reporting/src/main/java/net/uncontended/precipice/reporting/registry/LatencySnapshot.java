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

package net.uncontended.precipice.reporting.registry;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.HdrHistogram.RecordedValuesIterator;

public class LatencySnapshot {
    public static final LatencySnapshot DEFAULT_SNAPSHOT = new LatencySnapshot(-1, -1, -1, -1, -1, -1, -1, -1.0, -1, -1);

    public final long latencyMax;
    public final long latency50;
    public final long latency90;
    public final long latency99;
    public final long latency999;
    public final long latency9999;
    public final long latency99999;
    public final double latencyMean;

    public final long startTime;
    public final long endTime;

    public LatencySnapshot(long latency50, long latency90, long latency99, long latency999, long latency9999,
                           long latency99999, long latencyMax, double latencyMean, long startTime, long endTime) {
        this.latency50 = latency50;
        this.latency90 = latency90;
        this.latency99 = latency99;
        this.latency999 = latency999;
        this.latency9999 = latency9999;
        this.latency99999 = latency99999;
        this.latencyMax = latencyMax;
        this.latencyMean = latencyMean;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static LatencySnapshot fromHistogram(Histogram histogram) {
        long latency50 = histogram.getValueAtPercentile(50.0);
        long latency90 = histogram.getValueAtPercentile(90.0);
        long latency99 = histogram.getValueAtPercentile(99.0);
        long latency999 = histogram.getValueAtPercentile(99.9);
        long latency9999 = histogram.getValueAtPercentile(99.99);
        long latency99999 = histogram.getValueAtPercentile(99.999);
        long latencyMax = histogram.getMaxValue();
        double latencyMean = histogram.getMean();
        return new LatencySnapshot(latency50, latency90, latency99, latency999, latency9999, latency99999, latencyMax,
                latencyMean, histogram.getStartTimeStamp(), histogram.getEndTimeStamp());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LatencySnapshot that = (LatencySnapshot) o;

        if (latencyMax != that.latencyMax) return false;
        if (latency50 != that.latency50) return false;
        if (latency90 != that.latency90) return false;
        if (latency99 != that.latency99) return false;
        if (latency999 != that.latency999) return false;
        if (latency9999 != that.latency9999) return false;
        if (latency99999 != that.latency99999) return false;
        if (Double.compare(that.latencyMean, latencyMean) != 0) return false;
        if (startTime != that.startTime) return false;
        return endTime == that.endTime;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (int) (latencyMax ^ latencyMax >>> 32);
        result = 31 * result + (int) (latency50 ^ latency50 >>> 32);
        result = 31 * result + (int) (latency90 ^ latency90 >>> 32);
        result = 31 * result + (int) (latency99 ^ latency99 >>> 32);
        result = 31 * result + (int) (latency999 ^ latency999 >>> 32);
        result = 31 * result + (int) (latency9999 ^ latency9999 >>> 32);
        result = 31 * result + (int) (latency99999 ^ latency99999 >>> 32);
        temp = Double.doubleToLongBits(latencyMean);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        result = 31 * result + (int) (startTime ^ startTime >>> 32);
        result = 31 * result + (int) (endTime ^ endTime >>> 32);
        return result;
    }

    @Override
    public String toString() {
        return "LatencySnapshot{" +
                "latencyMax=" + latencyMax +
                ", latency50=" + latency50 +
                ", latency90=" + latency90 +
                ", latency99=" + latency99 +
                ", latency999=" + latency999 +
                ", latency9999=" + latency9999 +
                ", latency99999=" + latency99999 +
                ", latencyMean=" + latencyMean +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
