package vg_ms.flota.shared.security;

public class RoleConstants {

    public static final String ROLE_ADMIN            = "ADMIN";
    public static final String ROLE_ENTIDAD_ADMIN    = "ENTIDAD_ADMIN";
    public static final String ROLE_OPERADOR_ENTIDAD = "OPERADOR_ENTIDAD";
    public static final String ROLE_ANALISTA_ENTIDAD = "ANALISTA_ENTIDAD";

    public static final String READ_ROLES =
            "hasAnyRole('ADMIN','ENTIDAD_ADMIN','OPERADOR_ENTIDAD','ANALISTA_ENTIDAD')";

    public static final String WRITE_ROLES =
            "hasAnyRole('ADMIN','ENTIDAD_ADMIN','OPERADOR_ENTIDAD')";

    private RoleConstants() {}
}
