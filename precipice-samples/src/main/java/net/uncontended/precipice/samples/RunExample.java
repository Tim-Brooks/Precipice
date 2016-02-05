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

package net.uncontended.precipice.samples;

import net.uncontended.precipice.CallService;
import net.uncontended.precipice.Controller;
import net.uncontended.precipice.ControllerProperties;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.LongSemaphore;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;

public class RunExample {

    public static void main(String[] args) {
        String name = "Identity Service";
        int concurrencyLevel = 100;
        ControllerProperties<Status> properties = new ControllerProperties<>(Status.class);
        properties.semaphore(new LongSemaphore(concurrencyLevel));
        CallService service = new CallService(new Controller<>(name, properties));

        try {
            // Should return 64
            Integer result = service.call(Callables.success());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Throws RuntimeException
            Integer result = service.call(Callables.error());
        } catch (Exception e) {
            // Should be "Action Failed."
            e.getMessage();
        }

        try {
            // Throws RuntimeException
            Integer result = service.call(Callables.timeoutException());
        } catch (PrecipiceTimeoutException e) {
            // Should be Action timeout.
            e.getMessage();
        } catch (Exception e) {
            e.getMessage();
        }
    }
}
