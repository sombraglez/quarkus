package io.quarkus.websockets.next.test.broadcast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;

public class BroadcastOnMessageTest {

    @TestHTTPResource("up")
    URI upUri;

    @TestHTTPResource("up-blocking")
    URI upBlockingUri;

    @TestHTTPResource("up-multi-bidi")
    URI upMultiBidiUri;

    @RegisterExtension
    public static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(root -> {
                root.addClasses(Up.class, UpBlocking.class, UpMultiBidi.class);
            });

    @Test
    public void testUp() throws Exception {
        assertBroadcast(upUri);
    }

    @Test
    public void testUpBlocking() throws Exception {
        assertBroadcast(upBlockingUri);
    }

    @Test
    public void testUpMultiBidi() throws Exception {
        assertBroadcast(upMultiBidiUri);
    }

    public void assertBroadcast(URI testUri) throws Exception {
        Vertx vertx = Vertx.vertx();
        WebSocketClient client1 = vertx.createWebSocketClient();
        WebSocketClient client2 = vertx.createWebSocketClient();
        try {
            CountDownLatch connectedLatch = new CountDownLatch(2);
            CountDownLatch onMessageLatch = new CountDownLatch(2);
            AtomicReference<WebSocket> ws1 = new AtomicReference<>();

            List<String> messages = new CopyOnWriteArrayList<>();
            client1
                    .connect(testUri.getPort(), testUri.getHost(), testUri.getPath() + "/1")
                    .onComplete(r -> {
                        if (r.succeeded()) {
                            WebSocket ws = r.result();
                            ws.textMessageHandler(msg -> {
                                messages.add(msg);
                                onMessageLatch.countDown();
                            });
                            // We will use this socket to write a message later on
                            ws1.set(ws);
                            connectedLatch.countDown();
                        } else {
                            throw new IllegalStateException(r.cause());
                        }
                    });
            client2
                    .connect(testUri.getPort(), testUri.getHost(), testUri.getPath() + "/2")
                    .onComplete(r -> {
                        if (r.succeeded()) {
                            WebSocket ws = r.result();
                            ws.textMessageHandler(msg -> {
                                messages.add(msg);
                                onMessageLatch.countDown();
                            });
                            connectedLatch.countDown();
                        } else {
                            throw new IllegalStateException(r.cause());
                        }
                    });
            assertTrue(connectedLatch.await(5, TimeUnit.SECONDS));
            ws1.get().writeTextMessage("hello");
            assertTrue(onMessageLatch.await(5, TimeUnit.SECONDS));
            assertEquals(2, messages.size());
            // Both messages come from the first client
            assertEquals("1:HELLO", messages.get(0));
            assertEquals("1:HELLO", messages.get(1));
        } finally {
            client1.close().toCompletionStage().toCompletableFuture().get();
            client2.close().toCompletionStage().toCompletableFuture().get();
        }
    }

}
