package io.elastic.mandrill.triggers

import com.github.restdriver.clientdriver.ClientDriverRequest
import com.github.restdriver.clientdriver.ClientDriverRule
import io.elastic.api.*
import io.elastic.mandrill.Constants
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

import javax.json.Json

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo
import static org.hamcrest.Matchers.equalToIgnoringCase

class WebhookEventsSpecification extends Specification {

    @ClassRule
    @Shared
    public ClientDriverRule driver = new ClientDriverRule(11111);

    def setupSpec() {

        System.setProperty(Constants.ENV_VAR_WEBHOOK_URI, "http://example/webhook-url")
        System.setProperty(Constants.ENV_VAR_MANDRILL_API_BASE_URL, driver.getBaseUrl())

        def webhookResponse = Json.createObjectBuilder()
                .add("id", "42")
                .add("url", "http://example/webhook-url")
                .build();

        driver.addExpectation(
                onRequestTo(Constants.MANDRILL_API_WEBBHOOKS_ADD_PATH)
                        .withMethod(ClientDriverRequest.Method.POST)
                        .withBody(equalToIgnoringCase('{"key":"super-secret","url":"http://example/webhook-url"}'), "application/json"),
                giveResponse(
                        JSON.stringify(webhookResponse),
                        'application/json')
                        .withStatus(200)).anyTimes()

        driver.addExpectation(
                onRequestTo(Constants.MANDRILL_API_WEBBHOOKS_DELETE_PATH)
                        .withMethod(ClientDriverRequest.Method.POST)
                        .withBody(equalToIgnoringCase('{"key":"super-secret","id":"105"}'), "application/json"),
                giveResponse(
                        JSON.stringify(webhookResponse),
                        'application/json')
                        .withStatus(200)).anyTimes()
    }

    def "startup"() {

        setup:
        def events = new WebhookEvents()

        def configuration = Json.createObjectBuilder()
                .add(Constants.CONFIGURATION_API_KEY, "super-secret")
                .build();

        def startupParameters = new StartupParameters.Builder()
                .configuration(configuration)
                .build()


        when:
        def result = events.startup(startupParameters)

        then:
        JSON.stringify(result) == "{\"id\":\"42\",\"url\":\"http://example/webhook-url\"}"
    }

    def "shutdown"() {

        setup:
        def events = new WebhookEvents()

        def configuration = Json.createObjectBuilder()
                .add(Constants.CONFIGURATION_API_KEY, "super-secret")
                .build();

        def state = Json.createObjectBuilder()
                .add("id", "105")
                .build();

        def shutdownParameters = new ShutdownParameters.Builder()
                .configuration(configuration)
                .state(state)
                .build();


        when:
        events.shutdown(shutdownParameters)

        then:
        true
    }

    def "execute"() {

        setup:
        def events = new WebhookEvents()

        def configuration = Json.createObjectBuilder()
                .add(Constants.CONFIGURATION_API_KEY, "super-secret")
                .build();

        EventEmitter.Callback errorCallback = Mock(EventEmitter.Callback)
        EventEmitter.Callback snapshotCallback = Mock(EventEmitter.Callback)
        EventEmitter.Callback dataCallback = Mock(EventEmitter.Callback)
        EventEmitter.Callback reboundCallback = Mock(EventEmitter.Callback)
        EventEmitter.Callback updateKeysCallback = Mock(EventEmitter.Callback)
        EventEmitter.Callback httpReplyCallback = Mock(EventEmitter.Callback)
        EventEmitter emitter = new EventEmitter(errorCallback, dataCallback, snapshotCallback,
                reboundCallback, updateKeysCallback, httpReplyCallback)

        def mandrillEvents = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("foo", "bar").build())
                .build()

        def body = Json.createObjectBuilder()
                .add("mandrill_events", mandrillEvents)
                .build()

        def incomingMessage = new Message.Builder().body(body).build()

        def parameters = new ExecutionParameters.Builder(incomingMessage, emitter)
                .configuration(configuration)
                .build();


        when:
        events.execute(parameters)

        then:
        then:
        1 * dataCallback.receive({
            JSON.stringify(it.getBody()) == '{"foo":"bar"}'
        })
        0 * errorCallback.receive(_)
        0 * snapshotCallback.receive(_)
        0 * reboundCallback.receive(_)
        0 * updateKeysCallback.receive(_)
        0 * httpReplyCallback.receive(_)
    }
}
