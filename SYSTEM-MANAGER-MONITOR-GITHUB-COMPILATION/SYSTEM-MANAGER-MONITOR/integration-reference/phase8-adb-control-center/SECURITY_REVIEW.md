# Revisión de seguridad y alcance

- No hay ejecución arbitraria de comandos shell desde texto introducido por el usuario.
- No se invoca `su`, no se intenta explotar ADB y no se modifica el estado de depuración.
- No se almacenan códigos de emparejamiento, contraseñas ni claves RSA.
- El permiso Shizuku se solicita solo después de una acción explícita.
- El estado Shizuku se vuelve a consultar al recibir/morir el Binder.
- Los comandos del catálogo son informativos y se ejecutan desde Platform Tools externo.
- No se solicita `WRITE_SECURE_SETTINGS`, `MANAGE_USERS`, `DUMP` ni permisos signature/privileged.
- La ejecución privilegiada futura debe implementarse con UserService AIDL, allowlist estricta,
  validación de argumentos, límites de salida/tiempo y registro de auditoría.
