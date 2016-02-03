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

package net.uncontended.precipice.samples.kafka;

import net.uncontended.precipice.Controllable;
import net.uncontended.precipice.Controller;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.TimeoutException;

public class KafkaService<K, V> implements Controllable<Status> {

    private final Controller<Status> controller;
    private final KafkaProducer<K, V> producer;

    public KafkaService(Controller<Status> controller, KafkaProducer<K, V> producer) {
        this.controller = controller;
        this.producer = producer;
    }

    public PrecipiceFuture<Status, RecordMetadata> sendRecordAction(ProducerRecord<K, V> record) {
        final PrecipicePromise<Status, RecordMetadata> promise = controller.acquirePermitAndGetPromise();

        producer.send(record, new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                if (exception == null) {
                    promise.complete(Status.SUCCESS, metadata);
                } else {
                    if (exception instanceof TimeoutException) {
                        promise.completeExceptionally(Status.TIMEOUT, exception);
                    } else {
                        promise.completeExceptionally(Status.ERROR, exception);
                    }
                }
            }
        });

        return promise.future();
    }

    @Override
    public Controller<Status> controller() {
        return controller;
    }

    public void shutdown() {
        shutdown(true);
    }

    public void shutdown(boolean shutdownClient) {
        controller.shutdown();
        if (shutdownClient) {
            producer.close();
        }
    }
}
