/*
 * Copyright (c) 2015. Rick Hightower, Geoff Chandler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * QBit - The Microservice lib for Java : JSON, WebSocket, REST. Be The Web!
 */

package io.advantageous.qbit.vertx.http;

import io.advantageous.boon.core.Sys;
import io.advantageous.qbit.annotation.RequestMapping;
import io.advantageous.qbit.annotation.RequestMethod;
import io.advantageous.qbit.client.Client;
import io.advantageous.qbit.client.ClientBuilder;
import io.advantageous.qbit.http.client.HttpClient;
import io.advantageous.qbit.http.client.HttpClientBuilder;
import io.advantageous.qbit.http.request.HttpRequest;
import io.advantageous.qbit.http.request.HttpRequestBuilder;
import io.advantageous.qbit.http.request.HttpTextReceiver;
import io.advantageous.qbit.reactive.Callback;
import io.advantageous.qbit.server.EndpointServerBuilder;
import io.advantageous.qbit.server.ServiceEndpointServer;
import io.advantageous.qbit.service.ServiceProxyUtils;
import io.advantageous.qbit.test.TimedTesting;
import io.advantageous.qbit.util.PortUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.advantageous.boon.core.Exceptions.die;
import static io.advantageous.boon.core.IO.puts;


public class FullIntegrationTest extends TimedTesting {

    static volatile int port = 7777;
    Client client;
    ServiceEndpointServer server;
    HttpClient httpClient;
    ClientServiceInterface clientProxy;
    AtomicInteger callCount = new AtomicInteger();
    AtomicReference<String> pongValue;
    boolean ok;
    AtomicInteger returnCount = new AtomicInteger();

    /**
     * Holds on to Boon cache so we don't have to recreate reflected gak.
     */
    Object context = Sys.contextToHold();


    @Test
    public void testWebSocket() throws Exception {


        //Sys.sleep(100);
        clientProxy.ping(s -> {
            puts(s);
            pongValue.set(s);
        }, "hi");

        ServiceProxyUtils.flushServiceProxy(clientProxy);

        waitForTrigger(20, o -> this.pongValue.get() != null);


        final String pongValue = this.pongValue.get();
        ok = pongValue.equals("hi pong") || die();

    }

    @Test
    public void testWebSocketSend10() throws Exception {


        final Callback<String> callback = s -> {
            returnCount.incrementAndGet();

            puts("                     PONG");
            pongValue.set(s);
        };

        for (int index = 0; index < 20; index++) {

            clientProxy.ping(callback, "hi");

        }
        Sys.sleep(100);
        ServiceProxyUtils.flushServiceProxy(clientProxy);
        Sys.sleep(100);
        ServiceProxyUtils.flushServiceProxy(clientProxy);
        Sys.sleep(100);


        client.flush();
        Sys.sleep(100);


        waitForTrigger(5, o -> returnCount.get() == callCount.get());


        puts("HERE                        ", callCount, returnCount);

        ok = returnCount.get() >= callCount.get() - 1 || die(returnCount, callCount);


    }

    @Test
    public void testRestCallSimple() throws Exception {

        final HttpRequest request = new HttpRequestBuilder()
                .setUri("/services/mockservice/ping")
                .setJsonBodyForPost("\"hello\"")
                .setTextReceiver((code, mimeType, body) -> {

                    System.out.println(body);

                    if (code == 200) {
                        pongValue.set(body);
                    } else {
                        pongValue.set("ERROR " + body);
                        throw new RuntimeException("ERROR " + code + " " + body);

                    }
                })
                .build();

        httpClient.sendHttpRequest(request);

        httpClient.flush();

        waitForTrigger(20, o -> this.pongValue.get() != null);


        final String pongValue = this.pongValue.get();
        ok = pongValue.equals("\"hello pong\"") || die(pongValue);

    }


    @Before
    public synchronized void setup() throws Exception {

        super.setupLatch();

        port = PortUtils.findOpenPortStartAt(7000);
        pongValue = new AtomicReference<>();
        returnCount.set(0);

        httpClient = new HttpClientBuilder().setPort(port).build();

        puts("PORT", port);

        client = new ClientBuilder().setPort(port).build();
        server = new EndpointServerBuilder().setPort(port).build();

        server.initServices(new MockService());

        server.start();

        Sys.sleep(200);

        clientProxy = client.createProxy(ClientServiceInterface.class, "mockService");
        Sys.sleep(100);
        httpClient.startClient();
        Sys.sleep(100);
        client.start();

        callCount.set(0);
        pongValue.set(null);

        Sys.sleep(200);


    }

    @After
    public void teardown() throws Exception {


        if (!ok) {
            die("NOT OK");
        }

        Sys.sleep(200);
        server.stop();
        Sys.sleep(200);
        client.stop();
        httpClient.stop();
        Sys.sleep(200);
        server = null;
        client = null;
        System.gc();
        Sys.sleep(1000);

    }

    class MockService {

        @RequestMapping(method = RequestMethod.POST)
        public String ping(String ping) {
            callCount.incrementAndGet();
            return ping + " pong";
        }
    }
}
