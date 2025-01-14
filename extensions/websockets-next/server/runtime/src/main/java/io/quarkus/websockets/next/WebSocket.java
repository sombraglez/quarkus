package io.quarkus.websockets.next;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.smallrye.common.annotation.Experimental;

/**
 * Denotes a WebSocket endpoint.
 */
@Retention(RUNTIME)
@Target(TYPE)
@Experimental("This API is experimental and may change in the future")
public @interface WebSocket {

    /**
     * The path of the endpoint.
     * <p>
     * It is possible to match path parameters. The placeholder of a path parameter consists of the parameter name surrounded by
     * curly brackets. The actual value of a path parameter can be obtained using
     * {@link WebSocketConnection#pathParam(String)}. For example, the path <code>/foo/{bar}</code> defines the path
     * parameter {@code bar}.
     *
     * @see WebSocketConnection#pathParam(String)
     */
    public String path();

    /**
     * The execution mode used to process incoming messages for a specific connection.
     */
    public ExecutionMode executionMode() default ExecutionMode.SERIAL;

    /**
     * Defines the execution mode used to process incoming messages for a specific connection.
     *
     * @see WebSocketConnection
     */
    enum ExecutionMode {

        /**
         * Messages are processed serially, ordering is guaranteed.
         */
        SERIAL,

        /**
         * Messages are processed concurrently, there are no ordering guarantees.
         */
        CONCURRENT,

    }

}
