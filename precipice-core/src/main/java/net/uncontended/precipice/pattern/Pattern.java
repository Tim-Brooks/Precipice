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

package net.uncontended.precipice.pattern;

import net.uncontended.precipice.RejectedActionException;
import net.uncontended.precipice.ResilientAction;
import net.uncontended.precipice.SuperImpl;
import net.uncontended.precipice.metrics.ActionMetrics;

/**
 * A group of services that actions can be run on. Different implementations can
 * have different strategies for how to actions are distributed across the services.
 * This class receives {@link ResilientPatternAction} opposed to {@link ResilientAction}.
 * <p/>
 * The {@link ResilientPatternAction} {@code run} method is passed a context
 * specific to the service on which it is run.
 *
 */
public interface Pattern {

    ActionMetrics<SuperImpl> getActionMetrics();

    /**
     * Attempts to shutdown all the services. Actions after this
     * call will throw a {@link RejectedActionException}. Implementations
     * may differ on if pending or executing actions are cancelled.
     */
    void shutdown();
}
