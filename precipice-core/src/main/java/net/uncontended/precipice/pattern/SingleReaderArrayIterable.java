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

public class SingleReaderArrayIterable implements Iterator<Integer>, Iterable<Integer> {

    private int size;
    private Integer[] indices;
    private int index = 0;

    public SingleReaderArrayIterable(int size) {
        this.indices = new Integer[size];
        this.size = size;
    }

    public int size() {
        return indices.length;
    }

    @Override
    public boolean hasNext() {
        return index != size;
    }

    @Override
    public Iterator<Integer> iterator() {
        return this;
    }

    @Override
    public Integer next() {
        return indices[index++];
    }

    public Integer[] getIndices() {
        return this.indices;
    }

    public void reset() {
        index = 0;
    }
}
