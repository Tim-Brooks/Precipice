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

import java.util.Iterator;

public class PatternResult<C, P> {
    final PairIterator<C, P> iterator;
    private P patternCompletable;

    public PatternResult(ChildContext<C, P>[] entryArray) {
        this.iterator = new PairIterator<>(entryArray);
    }

    public Iterable<ChildContext<C, P>> submissions() {
        return iterator;
    }

    public P getPatternCompletable() {
        return patternCompletable;
    }

    void setPatternCompletable(P patternCompletable) {
        this.patternCompletable = patternCompletable;
    }

    static class PairIterator<C, P> implements Iterable<ChildContext<C, P>>, Iterator<ChildContext<C, P>> {
        private final ChildContext<C, P>[] entryArray;
        private int index = 0;
        private int count = 0;

        PairIterator(ChildContext<C, P>[] entryArray) {
            this.entryArray = entryArray;
        }

        @Override
        public boolean hasNext() {
            return index != count;
        }

        @Override
        public ChildContext<C, P> next() {
            int j = index;
            ++index;
            return entryArray[j];
        }

        @Override
        public Iterator<ChildContext<C, P>> iterator() {
            return this;
        }

        public void incrementCount() {
            ++count;
        }

        public ChildContext<C, P> get(int i) {
            return entryArray[i];
        }

        public void reset() {
            index = 0;
            count = 0;
        }
    }
}
