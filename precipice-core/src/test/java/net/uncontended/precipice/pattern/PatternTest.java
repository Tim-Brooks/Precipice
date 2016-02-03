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

import net.uncontended.precipice.Status;
import net.uncontended.precipice.threadpool.ThreadPoolService;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

public class PatternTest {

    @Mock
    private PatternStrategy strategy;

    private Pattern<Status, ThreadPoolService> pattern;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        List<ThreadPoolService> controllables = new ArrayList<>();
        pattern = new Pattern<>(controllables, strategy);
    }
}
