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

package net.uncontended.precipice;

import net.uncontended.precipice.metrics.counts.WritableCounts;
import net.uncontended.precipice.metrics.latency.WritableLatency;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;

import java.util.ArrayList;
import java.util.List;

public class GuardRailProperties<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {

    public String name;
    public WritableCounts<Result> resultMetrics;
    public WritableCounts<Rejected> rejectedMetrics;
    public WritableLatency<Result> resultLatency;
    public ArrayList<BackPressure<Rejected>> backPressureList = new ArrayList<>();
    public boolean singleIncrementMetrics = false;
    public Clock clock = new SystemTime();
}
