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

package net.uncontended.precipice.core.samples.kafka;

import net.uncontended.precipice.core.AbstractService;
import net.uncontended.precipice.core.ResilientAction;
import net.uncontended.precipice.core.ServiceProperties;
import net.uncontended.precipice.core.SubmissionService;
import net.uncontended.precipice.core.concurrent.PrecipiceFuture;
import net.uncontended.precipice.core.concurrent.PrecipicePromise;
import org.apache.kafka.clients.producer.KafkaProducer;

public class KafkaService<K, V> extends AbstractService implements SubmissionService {

    private final KafkaProducer<K, V> producer;

    public KafkaService(String name, ServiceProperties properties, KafkaProducer<K, V> producer) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.semaphore());
        this.producer = producer;
    }

    @Override
    public <T> PrecipiceFuture<T> submit(ResilientAction<T> action, long millisTimeout) {
        return null;
    }

    @Override
    public <T> void complete(ResilientAction<T> action, PrecipicePromise<T> promise, long millisTimeout) {

    }

    @Override
    public void shutdown() {
        isShutdown.set(true);
    }
}
