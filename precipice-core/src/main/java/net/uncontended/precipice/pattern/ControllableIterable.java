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

package net.uncontended.precipice.pattern;

import java.util.Iterator;

class ControllableIterable<C> implements Iterable<C>, Iterator<C> {

    private final C[] children;
    private int index = 0;

    private int count = 0;

    public ControllableIterable(C[] children) {
        this.children = children;
    }

    @Override
    public boolean hasNext() {
        return index != count;
    }

    @Override
    public C next() {
        int j = index;
        ++index;
        return children[j];
    }

    @Override
    public Iterator<C> iterator() {
        return this;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public void add(C child) {
        children[count++] = child;
    }

    public void reset() {
        index = 0;
        count = 0;
    }
}
