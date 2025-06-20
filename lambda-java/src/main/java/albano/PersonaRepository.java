package albano;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;
import java.util.Optional;

public class PersonaRepository {

    private final DynamoDbClient ddb;
    private final String tabla;

    public PersonaRepository(DynamoDbClient ddb, String tabla) {
        this.ddb = ddb;
        this.tabla = tabla;
    }

    /* ---------- C  R  E  A  T  E ---------- */
    public void create(Persona p) {
        var item = Map.of(
                "id",             AttributeValue.fromS(p.id()),
                "nombre",         AttributeValue.fromS(p.nombre()),
                "apellido",       AttributeValue.fromS(p.apellido()),
                "edad",           AttributeValue.fromN(Integer.toString(p.edad())),
                "equipoFavorito", AttributeValue.fromS(p.equipoFavorito())
        );

        ddb.putItem(PutItemRequest.builder()
                .tableName(tabla)
                .item(item)
                // evita sobrescribir si ya existe
                .conditionExpression("attribute_not_exists(id)")
                .build());
    }

    /* ---------- R  E  A  D ---------- */
    public Optional<Persona> read(String id) {
        var resp = ddb.getItem(GetItemRequest.builder()
                .tableName(tabla)
                .key(Map.of(
                        "id", AttributeValue.fromS(id)))
                .build());

        if (!resp.hasItem()) return Optional.empty();

        var i = resp.item();
        return Optional.of(new Persona(
                i.get("id").s(),
                i.get("nombre").s(),
                i.get("apellido").s(),
                Integer.parseInt(i.get("edad").n()),
                i.get("equipoFavorito").s()));
    }

    /* ---------- L  I  S  T ---------- */
    public java.util.List<Persona> list() {
        var resp = ddb.scan(ScanRequest.builder().tableName(tabla).build());
        return resp.items().stream()
                .map(i -> new Persona(
                        i.get("id").s(),
                        i.get("nombre").s(),
                        i.get("apellido").s(),
                        Integer.parseInt(i.get("edad").n()),
                        i.get("equipoFavorito").s()))
                .toList();
    }

    /* ---------- U  P  D  A  T  E ---------- */
    public Persona update(Persona p) {
        var resp = ddb.updateItem(UpdateItemRequest.builder()
                .tableName(tabla)
                .key(Map.of(
                        "id", AttributeValue.fromS(p.id())))
                .updateExpression("SET nombre = :n, apellido = :a, edad = :e, equipoFavorito = :q")
                .expressionAttributeValues(Map.of(
                        ":n", AttributeValue.fromS(p.nombre()),
                        ":a", AttributeValue.fromS(p.apellido()),
                        ":e", AttributeValue.fromN(Integer.toString(p.edad())),
                        ":q", AttributeValue.fromS(p.equipoFavorito())))
                .returnValues(ReturnValue.ALL_NEW)
                .build());

        var i = resp.attributes();
        return new Persona(
                i.get("id").s(),
                i.get("nombre").s(),
                i.get("apellido").s(),
                Integer.parseInt(i.get("edad").n()),
                i.get("equipoFavorito").s());
    }

    /* ---------- D  E  L  E  T  E ---------- */
    public void delete(String id) {
        ddb.deleteItem(DeleteItemRequest.builder()
                .tableName(tabla)
                .key(Map.of(
                        "id", AttributeValue.fromS(id)))
                .build());
    }
}
