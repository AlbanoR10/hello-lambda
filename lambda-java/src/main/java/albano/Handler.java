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

import java.time.Instant;
import java.util.Map;

public class Handler implements
        RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLA = System.getenv("TABLA_PERSONAS");

    private static final Region REGION =
            Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1"));

    private static final ObjectMapper JSON = new ObjectMapper();

    private final DynamoDbClient ddb = DynamoDbClient.builder()
            .region(REGION)           // ⬅ usa constante o lee AWS_REGION
            .build();

    private final PersonaRepository repo = new PersonaRepository(ddb, TABLA);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context ctx) {

        var logger = ctx.getLogger();
        logger.log("🟢 " + event.getHttpMethod() + " " + event.getPath());

        try {
            return switch (event.getHttpMethod()) {
                case "POST"   -> handleCreate(event);
                case "GET"    -> handleRead(event);
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

        repo.create(persona);
        return response(201, "Creada");
    }

    /* ---------- READ ---------- */
    private APIGatewayProxyResponseEvent handleRead(APIGatewayProxyRequestEvent ev) {
        NombreApellido p = path(ev);
        return repo.read(p.nombre(), p.apellido())
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

        Persona persona = JSON.readValue(ev.getBody(), Persona.class);
        persona.validar();
        Persona personaActualizada = repo.update(persona);
        return response(200, JSON.writeValueAsString(personaActualizada));
    }

    /* ---------- DELETE ---------- */
    private APIGatewayProxyResponseEvent handleDelete(APIGatewayProxyRequestEvent ev) {
        NombreApellido p = path(ev);
        repo.delete(p.nombre(), p.apellido());
        return response(204, "");
    }

    /* ---------- Helpers ---------- */
    private record NombreApellido(String nombre, String apellido) { }

    private NombreApellido path(APIGatewayProxyRequestEvent ev) {
        Map<String,String> p = ev.getPathParameters();
        return new NombreApellido(p.get("nombre"), p.get("apellido"));
    }


    private static APIGatewayProxyResponseEvent response(int code, String body) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(code)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(body);
    }

}
