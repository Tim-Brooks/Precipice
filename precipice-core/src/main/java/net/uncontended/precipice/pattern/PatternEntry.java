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

public class PatternEntry<C, P> {
    public final PairIterator<C, P> iterator = new PairIterator<>();
    public final P patternCompletable;

    PatternEntry(P patternCompletable) {
        this.patternCompletable = patternCompletable;
    }

    public Iterable<NewEntry<C, P>> interable() {
        return iterator;
    }

    private static class PairIterator<C, P> implements Iterable<NewEntry<C, P>>, Iterator<NewEntry<C, P>> {
        private final NewEntry<C, P>[] controllableArray = (NewEntry<C, P>[]) new Object[0];
        private int i = 0;

        @Override
        public boolean hasNext() {
            return controllableArray[i] != null;
        }

        @Override
        public NewEntry<C, P> next() {
            int j = i;
            ++i;
            return controllableArray[j];
        }

        @Override
        public Iterator<NewEntry<C, P>> iterator() {
            return this;
        }
    }
}
