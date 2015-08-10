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
import net.uncontended.precipice.core.AsyncService;
import net.uncontended.precipice.core.concurrent.Eventual;
import net.uncontended.precipice.core.concurrent.PrecipiceFuture;
import net.uncontended.precipice.core.concurrent.PrecipicePromise;
import net.uncontended.precipice.core.metrics.Metric;
import net.uncontended.precipice.core.timeout.ActionTimeoutException;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.TimeoutException;

public class KafkaService<K, V> extends AbstractService implements AsyncService {

    private final KafkaProducer<K, V> producer;

    public KafkaService(String name, ServiceProperties properties, KafkaProducer<K, V> producer) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.semaphore());
        this.producer = producer;
    }

    public <T> PrecipiceFuture<T> sendRecordAction(KafkaAction<T, K, V> action) {
        return submit(action, -1);
    }

    @Override
    public <T> PrecipiceFuture<T> submit(ResilientAction<T> action, long millisTimeout) {
        final Eventual<T> eventual = new Eventual<>();
        complete(action, eventual, millisTimeout);
        return eventual;
    }

    @Override
    public <T> void complete(ResilientAction<T> action, final PrecipicePromise<T> promise, long millisTimeout) {
        acquirePermitOrRejectIfActionNotAllowed();

        final KafkaAction<T, K, V> kafkaAction = (KafkaAction<T, K, V>) action;
        producer.send(kafkaAction.getRecord(), new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                try {
                    handleResult(promise, kafkaAction, metadata, exception);
                } finally {
                    semaphore.releasePermit();
                }
            }
        });
    }

    @Override
    public void shutdown() {
        isShutdown.set(true);
    }

    private <T> void handleResult(PrecipicePromise<T> promise, KafkaAction<T, K, V> kafkaAction, RecordMetadata
            metadata, Exception exception) {
        if (exception == null) {
            kafkaAction.setRecordMetadata(metadata);

            try {
                T result = kafkaAction.run();
                actionMetrics.incrementMetricCount(Metric.SUCCESS);
                promise.complete(result);
            } catch (ActionTimeoutException e) {
                actionMetrics.incrementMetricCount(Metric.TIMEOUT);
                promise.completeWithTimeout();
            } catch (Exception e) {
                actionMetrics.incrementMetricCount(Metric.ERROR);
                promise.completeExceptionally(e);
            }
        } else {
            if (exception instanceof TimeoutException) {
                actionMetrics.incrementMetricCount(Metric.TIMEOUT);
                promise.completeWithTimeout();
            } else {
                actionMetrics.incrementMetricCount(Metric.ERROR);
                promise.completeExceptionally(exception);
            }
        }
    }
}
