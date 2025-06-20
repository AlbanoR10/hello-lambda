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
                "nombre",         AttributeValue.fromS(p.nombre()),
                "apellido",       AttributeValue.fromS(p.apellido()),
                "edad",           AttributeValue.fromN(Integer.toString(p.edad())),
                "equipoFavorito", AttributeValue.fromS(p.equipoFavorito())
        );

        ddb.putItem(PutItemRequest.builder()
                .tableName(tabla)
                .item(item)
                // evita sobrescribir si ya existe
                .conditionExpression("attribute_not_exists(nombre)")
                .build());
    }

    /* ---------- R  E  A  D ---------- */
    public Optional<Persona> read(String nombre, String apellido) {
        var resp = ddb.getItem(GetItemRequest.builder()
                .tableName(tabla)
                .key(Map.of(
                        "nombre",   AttributeValue.fromS(nombre),
                        "apellido", AttributeValue.fromS(apellido)))
                .build());

        if (!resp.hasItem()) return Optional.empty();

        var i = resp.item();
        return Optional.of(new Persona(
                i.get("nombre").s(),
                i.get("apellido").s(),
                Integer.parseInt(i.get("edad").n()),
                i.get("equipoFavorito").s()));
    }

    /* ---------- U  P  D  A  T  E ---------- */
    public Persona update(Persona p) {
        var resp = ddb.updateItem(UpdateItemRequest.builder()
                .tableName(tabla)
                .key(Map.of(
                        "nombre",   AttributeValue.fromS(p.nombre()),
                        "apellido", AttributeValue.fromS(p.apellido())))
                .updateExpression("SET edad = :e, equipoFavorito = :q")
                .expressionAttributeValues(Map.of(
                        ":e", AttributeValue.fromN(Integer.toString(p.edad())),
                        ":q", AttributeValue.fromS(p.equipoFavorito())))
                .returnValues(ReturnValue.ALL_NEW)
                .build());

        var i = resp.attributes();
        return new Persona(
                i.get("nombre").s(),
                i.get("apellido").s(),
                Integer.parseInt(i.get("edad").n()),
                i.get("equipoFavorito").s());
    }

    /* ---------- D  E  L  E  T  E ---------- */
    public void delete(String nombre, String apellido) {
        ddb.deleteItem(DeleteItemRequest.builder()
                .tableName(tabla)
                .key(Map.of(
                        "nombre",   AttributeValue.fromS(nombre),
                        "apellido", AttributeValue.fromS(apellido)))
                .build());
    }
}
