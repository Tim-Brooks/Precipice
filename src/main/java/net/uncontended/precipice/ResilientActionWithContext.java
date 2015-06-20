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

package net.uncontended.precipice;

/**
 * Created by timbrooks on 6/19/15.
 */
class ResilientActionWithContext<T, C> implements ResilientAction<T> {
    public C context;
    private final ResilientPatternAction<T, C> action;

    public ResilientActionWithContext(ResilientPatternAction<T, C> action) {
        this.action = action;
    }

    @Override
    public T run() throws Exception {
        return action.run(context);
    }
}
