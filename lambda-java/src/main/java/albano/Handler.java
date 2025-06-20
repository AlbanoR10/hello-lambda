package albano;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;

import java.time.Instant;
import java.util.Map;

public class Handler implements
        RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLA = System.getenv("TABLA_SALUDOS");

    private static final Region REGION =
            Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1"));

    private final DynamoDbClient ddb = DynamoDbClient.builder()
            .region(REGION)           // ⬅ usa constante o lee AWS_REGION
            .build();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event,
            Context context) {

        var logger = context.getLogger();
        logger.log("🟢 Inicio de ejecución – Región: " + REGION.id());

        // Lee ?nombre=...  (o usa "Mundo" por defecto)
        String nombre = "Mundo";
        if (event.getQueryStringParameters() != null &&
                event.getQueryStringParameters().get("nombre") != null) {
            nombre = event.getQueryStringParameters().get("nombre");
        }
        logger.log("📥 Parámetro nombre = " + nombre);

        try {
            logger.log("💾 Insertando ítem en DynamoDB…");

            PutItemResponse resp =  ddb.putItem(PutItemRequest.builder()
                    .tableName(TABLA)
                    .item(Map.of(
                            "pk", AttributeValue.fromS("saludo"),
                            "sk", AttributeValue.fromS(Instant.now().toString()),
                            "nombre", AttributeValue.fromS(nombre)))
                    .build());

            var http = resp.sdkHttpResponse();
            logger.log("🔹 Dynamo status=" + http.statusCode() +
                    ", requestId=" + http.firstMatchingHeader("x-amzn-RequestId").orElse("N/A"));

            logger.log("✅ Inserción completada");

            /* 3) Construir respuesta HTTP 200 */
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("Hola " + nombre);

            /* 4) Errores específicos de DynamoDB */
        } catch (DynamoDbException e) {
            logger.log("❌ Error de DynamoDB: " + e.getMessage());
            return errorResponse();

            /* 5) Otros errores del SDK AWS */
        } catch (SdkException e) {
            logger.log("❌ Error del SDK AWS: " + e.getMessage());
            return errorResponse();

            /* 6) Cualquier excepción inesperada */
        } catch (Exception e) {
            logger.log("❌ Error inesperado: " + e.getMessage());
            return errorResponse();
        }
    }

    private static APIGatewayProxyResponseEvent errorResponse() {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withBody("Error interno");
    }
}
