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

package net.uncontended.precipice;

import net.uncontended.precipice.concurrent.ResilientFuture;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by timbrooks on 11/16/14.
 */
public class ExampleRequest implements Runnable {

    private final Service service;

    public ExampleRequest(Service service) {
        this.service = service;
    }

    public void run() {
        for (; ; ) {
            List<ResilientFuture<String>> futures = new ArrayList<>();
            for (int i = 0; i < 1; ++i) {
                try {
                    ResilientFuture<String> result = service.submitAction(new ResilientAction<String>() {
                        @Override
                        public String run() throws Exception {
                            new Random().nextBoolean();
//                            Thread.sleep(3);
                            String result = null;
                            InputStream response = new URL("http://localhost:6001/").openStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(response));
                            result = reader.readLine();
                            reader.close();
                            response.close();
                            return result;
                        }
                    }, 10);
                    futures.add(result);
                } catch (RuntimeException e) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }

            long start = System.currentTimeMillis();
            for (ResilientFuture<String> result : futures) {
                try {
                    result.get();
                } catch (Exception e) {
//            e.printStackTrace();
                }
            }

        }
    }
}
