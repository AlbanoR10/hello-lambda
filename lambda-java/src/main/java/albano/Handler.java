package albano;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.Map;

public class Handler implements
        RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLA = System.getenv("TABLA_PERSONAS");

    private static final Region REGION =
            Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1"));

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final DynamoDbClient ddb = DynamoDbClient.builder()
            .region(REGION)           // ⬅ usa constante o lee AWS_REGION
            .build();

    private static final PersonaRepository repo = new PersonaRepository(ddb, TABLA);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context ctx) {

        var logger = ctx.getLogger();
        logger.log("🟢 " + event.getHttpMethod() + " " + event.getPath());

        try {
            return switch (event.getHttpMethod()) {
                case "POST"   -> handleCreate(event);
                case "GET"    -> handleGet(event);
                case "PUT"    -> handleUpdate(event);
                case "DELETE" -> handleDelete(event);
                default       -> response(405, "Método no permitido");
            };

        } catch (IllegalArgumentException | com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.log("⚠️ 400: " + e.getMessage());
            return response(400, e.getMessage());

        } catch (DynamoDbException e) {
            logger.log("❌ Dynamo/SDK error: " + e.getMessage());
            return response(500, "Error interno");
        } catch (SdkException e) {
            logger.log("❌ SDK error: " + e.getMessage());
            return response(500, "Error interno");
        } catch (Exception e) {
            logger.log("❌ Error inesperado: " + e.getMessage());
            return response(500, "Error interno");
        }
    }

    /* ---------- CREATE ---------- */
    private APIGatewayProxyResponseEvent handleCreate(APIGatewayProxyRequestEvent ev)
            throws com.fasterxml.jackson.core.JsonProcessingException {

        Persona persona = JSON.readValue(ev.getBody(), Persona.class);
        persona.validar();
        String id = java.util.UUID.randomUUID().toString();
        Persona conId = persona.withId(id);

        repo.create(conId);
        return response(201, JSON.writeValueAsString(Map.of("id", id)));
    }

    /* ---------- LIST or READ ---------- */
    private APIGatewayProxyResponseEvent handleGet(APIGatewayProxyRequestEvent ev) {
        Map<String,String> p = ev.getPathParameters();
        if (p == null || !p.containsKey("id")) {
            // list all personas
            try {
                return response(200, JSON.writeValueAsString(repo.list()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return handleRead(ev);
    }

    /* ---------- READ ---------- */
    private APIGatewayProxyResponseEvent handleRead(APIGatewayProxyRequestEvent ev) {
        String id = pathId(ev);
        return repo.read(id)
                .<APIGatewayProxyResponseEvent>map(per ->
                {
                    try {
                        return response(200, JSON.writeValueAsString(per));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseGet(() -> response(404, "No encontrada"));
    }

    /* ---------- UPDATE ---------- */
    private APIGatewayProxyResponseEvent handleUpdate(APIGatewayProxyRequestEvent ev)
            throws com.fasterxml.jackson.core.JsonProcessingException {

        String id = pathId(ev);
        Persona persona = JSON.readValue(ev.getBody(), Persona.class);
        persona.validar();
        Persona personaActualizada = repo.update(persona.withId(id));
        return response(200, JSON.writeValueAsString(personaActualizada));
    }

    /* ---------- DELETE ---------- */
    private APIGatewayProxyResponseEvent handleDelete(APIGatewayProxyRequestEvent ev) {
        String id = pathId(ev);
        repo.delete(id);
        return response(204, "");
    }

    /* ---------- Helpers ---------- */
    private String pathId(APIGatewayProxyRequestEvent ev) {
        Map<String,String> p = ev.getPathParameters();
        if (p == null || !p.containsKey("id"))
            throw new IllegalArgumentException("id requerido");
        return p.get("id");
    }


    private static APIGatewayProxyResponseEvent response(int code, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(code)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(body);
    }

}
