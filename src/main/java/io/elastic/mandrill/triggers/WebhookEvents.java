package io.elastic.mandrill.triggers;

import io.elastic.api.*;
import io.elastic.mandrill.Constants;
import io.elastic.sailor.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.*;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

public class WebhookEvents implements Module {

    private static final Logger logger = LoggerFactory.getLogger(WebhookEvents.class);

    private static final String ID = "id";

    @Override
    public JsonObject startup(final StartupParameters parameters) {
        final JsonObject configuration = parameters.getConfiguration();
        final String apiKey = configuration.getString(Constants.CONFIGURATION_API_KEY);

        if (apiKey == null) {
            throw new IllegalStateException(Constants.CONFIGURATION_API_KEY + " is required");
        }

        final String webhookUri = Utils.getEnvVar(Constants.ENV_VAR_WEBHOOK_URI);

        if (webhookUri == null) {
            throw new IllegalStateException("Environmental variable " + Constants.ENV_VAR_WEBHOOK_URI + " is required");
        }

        logger.info("About to add the webhook for uri={}", webhookUri);

        final JsonObject requestBody = Json.createObjectBuilder()
                .add("key", apiKey)
                .add("url", webhookUri)
                .build();

        final JsonObject webhook = ClientBuilder.newClient()
                .target(getMandrillApiBaseURL())
                .path(Constants.MANDRILL_API_WEBBHOOKS_ADD_PATH)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(requestBody, MediaType.APPLICATION_JSON))
                .readEntity(JsonObject.class);

        logger.info("Successfully added webhook with id={}", webhook.getString(ID));

        return webhook;
    }

    @Override
    public void shutdown(final ShutdownParameters parameters) {

        final JsonString id = parameters.getState().getJsonString(ID);

        logger.info("About to delete the webhook with id={}", id);

        final JsonObject configuration = parameters.getConfiguration();
        final String apiKey = configuration.getString(Constants.CONFIGURATION_API_KEY);

        if (apiKey == null) {
            throw new IllegalStateException(Constants.CONFIGURATION_API_KEY + " is required");
        }

        final JsonObject requestBody = Json.createObjectBuilder()
                .add("key", apiKey)
                .add(ID, id)
                .build();

        final JsonObject webhook = ClientBuilder.newClient()
                .target(getMandrillApiBaseURL())
                .path(Constants.MANDRILL_API_WEBBHOOKS_DELETE_PATH)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(requestBody, MediaType.APPLICATION_JSON))
                .readEntity(JsonObject.class);

        logger.info("Successfully deleted the webhook with id={}", id);
    }

    @Override
    public void execute(final ExecutionParameters parameters) {
        final JsonObject body = parameters.getMessage().getBody();

        logger.info(parameters.getMessage().toString());

        if (!!body.containsValue("mandrill_events")) {
            logger.info("Message has not mandrill events. Skipping it.");
            return;
        }

        final JsonArray events = JSON.parseArray(body.getString("mandrill_events"));

        final int eventsCount = events.size();

        logger.info("Got {} events", eventsCount);

        for (JsonValue next : events) {
            final Message msg = new Message.Builder()
                    .body((JsonObject) next)
                    .build();

            parameters.getEventEmitter().emitData(msg);
        }

        logger.info("Emitted {} messages. Finishing execution.", eventsCount);
    }

    private static final String getMandrillApiBaseURL() {

        final String result = Utils.getOptionalEnvVar(Constants.ENV_VAR_MANDRILL_API_BASE_URL);

        if (result != null) {
            return result;
        }

        return Constants.MANDRILL_API_BASE_URL;
    }
}
