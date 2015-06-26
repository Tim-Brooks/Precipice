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

package net.uncontended.precipice.example;

import net.uncontended.precipice.ActionTimeoutException;
import net.uncontended.precipice.RunService;
import net.uncontended.precipice.Services;

public class RunExample {

    public static void main(String[] args) {
        String serviceName = "Identity Service";
        int concurrencyLevel = 100;
        RunService service = Services.runService(serviceName, concurrencyLevel);

        try {
            // Should return 64
            Integer result = service.run(Actions.successAction());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Throws RuntimeException
            Integer result = service.run(Actions.errorAction());
        } catch (Exception e) {
            // Should be "Action Failed."
            e.getMessage();
        }

        try {
            // Throws RuntimeException
            Integer result = service.run(Actions.runTimeoutAction());
        } catch (ActionTimeoutException e) {
            // Should be Action timeout.
            e.getMessage();
        } catch (Exception e) {
            e.getMessage();
        }
    }
}
