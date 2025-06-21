package albano;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/** Modelo de dominio – se serializa y deserializa como JSON. */
@DynamoDbBean
public class Persona {
    private String id;
    private String nombre;
    private String apellido;
    private int edad;
    private String equipoFavorito;

    public Persona() {}

    public Persona(String id, String nombre, String apellido, int edad, String equipoFavorito) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.edad = edad;
        this.equipoFavorito = equipoFavorito;
    }

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public String getEquipoFavorito() {
        return equipoFavorito;
    }

    public void setEquipoFavorito(String equipoFavorito) {
        this.equipoFavorito = equipoFavorito;
    }

    public Persona withId(String id) {
        return new Persona(id, nombre, apellido, edad, equipoFavorito);
    }

    public void validar() {
        if (nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("nombre vacío");
        if (apellido == null || apellido.isBlank())
            throw new IllegalArgumentException("apellido vacío");
        if (edad <= 0)
            throw new IllegalArgumentException("edad debe ser > 0");
    }
}
