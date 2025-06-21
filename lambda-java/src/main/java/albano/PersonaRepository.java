package albano;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Optional;
import java.util.List;

public class PersonaRepository {

    private final DynamoDbTable<Persona> tabla;

    public PersonaRepository(DynamoDbClient ddb, String nombreTabla) {
        DynamoDbEnhancedClient enhanced = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(ddb)
                .build();
        this.tabla = enhanced.table(nombreTabla, TableSchema.fromBean(Persona.class));
    }

    /* ---------- C  R  E  A  T  E ---------- */
    public void create(Persona p) {
        Expression cond = Expression.builder()
                .expression("attribute_not_exists(id)")
                .build();
        PutItemEnhancedRequest<Persona> req = PutItemEnhancedRequest.builder(Persona.class)
                .item(p)
                .conditionExpression(cond)
                .build();
        tabla.putItem(req);
    }

    /* ---------- R  E  A  D ---------- */
    public Optional<Persona> read(String id) {
        Persona p = tabla.getItem(Key.builder().partitionValue(id).build());
        return Optional.ofNullable(p);
    }

    /* ---------- L  I  S  T ---------- */
    public List<Persona> list() {
        return tabla.scan().items().stream().toList();
    }

    /* ---------- U  P  D  A  T  E ---------- */
    public Persona update(Persona p) {
        Expression cond = Expression.builder()
                .expression("attribute_exists(id)")
                .build();
        UpdateItemEnhancedRequest<Persona> req = UpdateItemEnhancedRequest.builder(Persona.class)
                .item(p)
                .conditionExpression(cond)
                .build();
        return tabla.updateItem(req);
    }

    /* ---------- D  E  L  E  T  E ---------- */
    public void delete(String id) {
        Expression cond = Expression.builder()
                .expression("attribute_exists(id)")
                .build();
        DeleteItemEnhancedRequest req = DeleteItemEnhancedRequest.builder()
                .key(k -> k.partitionValue(id))
                .conditionExpression(cond)
                .build();
        tabla.deleteItem(req);
    }
}
