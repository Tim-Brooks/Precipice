package net.uncontended.beehive;

import net.uncontended.beehive.concurrent.ResilientFuture;

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

    private final ServiceExecutor serviceExecutor;

    public ExampleRequest(ServiceExecutor serviceExecutor) {
        this.serviceExecutor = serviceExecutor;
    }

    public void run() {
        for (; ; ) {
            List<ResilientFuture<String>> futures = new ArrayList<>();
            for (int i = 0; i < 1; ++i) {
                try {
                    ResilientFuture<String> result = serviceExecutor.submitAction(new ResilientAction<String>() {
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
