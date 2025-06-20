package albano;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.Map;

public class AdiosHandler implements
        RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

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

        String body = "Adios " + nombre;


        // Construye la respuesta HTTP 200
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Map.of("Content-Type", "text/plain"))
                .withBody(body);
    }
}
