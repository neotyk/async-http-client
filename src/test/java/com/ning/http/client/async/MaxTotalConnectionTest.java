/*
* Copyright 2010 Ning, Inc.
*
* Ning licenses this file to you under the Apache License, version 2.0
* (the "License"); you may not use this file except in compliance with the
* License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package com.ning.http.client.async;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.Response;
import com.ning.http.client.logging.LogManager;
import com.ning.http.client.logging.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MaxTotalConnectionTest extends AbstractBasicTest {
    protected final Logger log = LogManager.getLogger(AbstractBasicTest.class);

    @Test
    public void testMaxTotalConnectionsExceedingException() {
        String[] urls = new String[]{
                "http://google.com",
                "http://github.com/"};

        AsyncHttpClient client = new AsyncHttpClient(
                new AsyncHttpClientConfig.Builder()
                        .setConnectionTimeoutInMs(1000)
                        .setRequestTimeoutInMs(5000)
                        .setKeepAlive(false)
                        .setMaximumConnectionsTotal(1)
                        .setMaximumConnectionsPerHost(1)
                        .build()
        );

        boolean caughtError = false;
        for (int i = 0; i < urls.length; i++) {
            try {
                client.prepareGet(urls[i]).execute();
            } catch (IOException e) {
                // assert that 2nd request fails, because maxTotalConnections=1
                Assert.assertEquals(1, i);
                caughtError = true;
            }
        }
        Assert.assertTrue(caughtError);
    }

    @Test
    public void testMaxTotalConnections() {
        String[] urls = new String[]{
                "http://google.com",
                "http://lenta.ru"};

        AsyncHttpClient client = new AsyncHttpClient(
                new AsyncHttpClientConfig.Builder()
                        .setConnectionTimeoutInMs(1000)
                        .setRequestTimeoutInMs(5000)
                        .setKeepAlive(false)
                        .setMaximumConnectionsTotal(2)
                        .setMaximumConnectionsPerHost(1)
                        .build()
        );

        for (String url : urls) {
            try {
                client.prepareGet(url).execute();
            } catch (IOException e) {
                Assert.fail("Smth wrong with connections handling!");
            }
        }
    }


    @Test
    public void testMaxTotalConnectionsCorrectExceptionHandling() {
        String[] urls = new String[]{
                "http://google.com",
                "http://github.com/"};

        AsyncHttpClient client = new AsyncHttpClient(
                new AsyncHttpClientConfig.Builder()
                        .setConnectionTimeoutInMs(1000)
                        .setRequestTimeoutInMs(5000)
                        .setKeepAlive(false)
                        .setMaximumConnectionsTotal(1)
                        .setMaximumConnectionsPerHost(1)
                        .build()
        );

        List<Future> futures = new ArrayList<Future>();
        boolean caughtError = false;
        for (int i = 0; i < urls.length; i++) {
            try {
                Future<Response> future = client.prepareGet(urls[i]).execute();
                if (future != null) {
                    futures.add(future);
                }
            } catch (IOException e) {
                // assert that 2nd request fails, because maxTotalConnections=1
                Assert.assertEquals(i, 1);
                caughtError = true;
            }
        }
        Assert.assertTrue(caughtError);

        // get results of executed requests
        for (Future future : futures) {
            try {
                Object res = future.get();
            } catch (InterruptedException e) {
                log.error("Error!", e);
            } catch (ExecutionException e) {
                log.error("Error!", e);
            }
        }

        // try to execute once again, expecting that 1 connection is released
        caughtError = false;
        for (int i = 0; i < urls.length; i++) {
            try {
                client.prepareGet(urls[i]).execute();
            } catch (IOException e) {
                // assert that 2nd request fails, because maxTotalConnections=1
                Assert.assertEquals(i, 1);
                caughtError = true;
            }
        }
        Assert.assertTrue(caughtError);
    }
}


