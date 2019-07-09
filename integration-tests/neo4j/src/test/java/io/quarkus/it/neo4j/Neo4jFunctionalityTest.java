package io.quarkus.it.neo4j;

import static org.hamcrest.Matchers.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Test connecting via Neo4j Java-Driver to Neo4j.
 * Can quickly start a matching database with:
 *
 * <pre>
 *     docker run --publish=7474:7474 --publish=7687:7687 -e 'NEO4J_AUTH=neo4j/music' neo4j:3.5.3
 * </pre>
 */
@QuarkusTest
public class Neo4jFunctionalityTest {

    @Test
    public void testBlockingNeo4jFunctionality() {
        RestAssured.given().when().get("/neo4j/blocking").then().body(is("OK"));
    }

    @Test
    public void testAsynchronousNeo4jFunctionality() {
        RestAssured.given()
                .when().get("/neo4j/asynchronous")
                .then().statusCode(200)
                .body(is(equalTo(Stream.of(1, 2, 3).map(i -> i.toString()).collect(Collectors.joining(",", "[", "]")))));
    }

    @Test
    @Disabled // Works only with Neo4j 4.0
    public void testReactiveNeo4jFunctionality() {
        RestAssured.given()
                .when().get("/neo4j/reactive")
                .prettyPeek()
                .then().statusCode(200)
                .contentType("text/event-stream");
    }
}
