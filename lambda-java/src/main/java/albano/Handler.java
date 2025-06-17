package albano;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.Map;

public class Handler implements
        RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String TABLA = System.getenv("TABLA_SALUDOS");
    private final DynamoDbClient ddb = DynamoDbClient.builder()
            .region(Region.US_EAST_1)           // ⬅ usa constante o lee AWS_REGION
            .build();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event,
            Context context) {

        // Lee ?nombre=...  (o usa "Mundo" por defecto)
        String nombre = "Mundo";
        if (event.getQueryStringParameters() != null &&
                event.getQueryStringParameters().get("nombre") != null) {
            nombre = event.getQueryStringParameters().get("nombre");
        }

        ddb.putItem(PutItemRequest.builder()
                .tableName(TABLA)
                .item(Map.of(
                        "pk", AttributeValue.fromS("saludo"),
                        "sk", AttributeValue.fromS(Instant.now().toString()),
                        "nombre", AttributeValue.fromS(nombre)))
                .build());

        // Construye la respuesta HTTP 200
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody("Hola " + nombre);
    }
}
