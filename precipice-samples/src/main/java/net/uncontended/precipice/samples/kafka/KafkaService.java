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

import net.uncontended.precipice.*;
import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.timeout.ActionTimeoutException;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.TimeoutException;

public class KafkaService<K, V> extends AbstractService implements AsyncService {

    private final KafkaProducer<K, V> producer;
    private final ActionMetrics<Status> actionMetrics;

    public KafkaService(String name, ServiceProperties properties, KafkaProducer<K, V> producer) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.latencyMetrics(),
                properties.semaphore());
        this.producer = producer;
        actionMetrics = (ActionMetrics<Status>) properties.actionMetrics();
    }

    public PrecipiceFuture<Status, RecordMetadata> sendRecordAction(ProducerRecord<K, V> record) {
        KafkaAction<RecordMetadata, K, V> action = new RecordMetadataAction<>(record);
        return submit(action, -1);
    }

    @Override
    public <T> PrecipiceFuture<Status, T> submit(ResilientAction<T> action, long millisTimeout) {
        final Eventual<Status, T> eventual = new Eventual<>();
        complete(action, eventual, millisTimeout);
        return eventual;
    }

    @Override
    public <T> void complete(ResilientAction<T> action, final PrecipicePromise<Status, T> promise, long millisTimeout) {
        acquirePermitOrGetRejectedReason();

        final KafkaAction<T, K, V> kafkaAction = (KafkaAction<T, K, V>) action;
        producer.send(kafkaAction.getRecord(), new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                try {
                    handleResult(promise, kafkaAction, metadata, exception);
                } finally {
                    semaphore.releasePermit(1);
                }
            }
        });
    }

    @Override
    public Controller<Status> controller() {
        return null;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        producer.close();
    }

    private <T> void handleResult(PrecipicePromise<Status, T> promise, KafkaAction<T, K, V> kafkaAction,
                                  RecordMetadata metadata, Exception exception) {
        if (exception == null) {
            kafkaAction.setRecordMetadata(metadata);

            try {
                T result = kafkaAction.run();
                actionMetrics.incrementMetricCount(Status.SUCCESS);
                promise.complete(Status.SUCCESS, result);
            } catch (ActionTimeoutException e) {
                actionMetrics.incrementMetricCount(Status.TIMEOUT);
                promise.completeExceptionally(Status.TIMEOUT, e);
            } catch (Exception e) {
                actionMetrics.incrementMetricCount(Status.ERROR);
                promise.completeExceptionally(Status.ERROR, e);
            }
        } else {
            if (exception instanceof TimeoutException) {
                actionMetrics.incrementMetricCount(Status.TIMEOUT);
                promise.completeExceptionally(Status.ERROR, exception);
            } else {
                actionMetrics.incrementMetricCount(Status.ERROR);
                promise.completeExceptionally(Status.ERROR, exception);
            }
        }
    }

    private static class RecordMetadataAction<K, V> extends KafkaAction<RecordMetadata, K, V> {
        private RecordMetadataAction(ProducerRecord<K, V> record) {
            super(record);
        }

        @Override
        public RecordMetadata run() throws Exception {
            return recordMetadata;
        }
    }
}
