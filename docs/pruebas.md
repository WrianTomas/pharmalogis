# Pruebas y evidencias

Fecha de ejecución: 23/10/2026, zona America/Lima. Entorno: Windows, Java 21, Spring Boot 4.1.1, H2 2.4.240 y MySQL 8.4.13. Las pruebas usan la aplicación real con Spring Security, Thymeleaf y repositorios JPA. MySqlIntegrationTests valida el esquema real y revierte sus datos temporales al finalizar.

## Resultado automático

23 pruebas aprobadas, 0 fallos, 0 errores: 22 con H2 y 1 con MySQL real, ejecutadas en sus respectivos perfiles.

| Grupo | Cantidad | Cobertura |
|---|---|---|
| AttendancePolicyTests | 8 | Falta; límite de tolerancia; tardanza; marca única; pausas; desorden/duplicados; número impar; salida anticipada |
| WorkflowTests | 13 | Renderización; autenticación; roles; CSRF; CRUD de tres catálogos; validaciones; referencias; importación; recálculo; filtros; cuenta JWT; errores |
| SystemApplicationTests | 1 | Arranque del contexto con persistencia de pruebas |
| MySqlIntegrationTests | 1 | Esquema real, CRUD, importación y cálculo idempotentes sobre MySQL 8.4.13 |

Los archivos originales de Surefire están en `system/target/surefire-reports`. Se adjunta una copia de los resultados de texto en `docs/evidencias/pruebas-automatizadas.txt`. Reproducir mediante `system/mvnw.cmd test`.

## Evidencias visuales

Las capturas principales de `docs/evidencias/mysql/` muestran el sistema conectado a **MySQL 8.4.13**, tomadas en Chrome con la cuenta administrativa configurada localmente. Las capturas de la carpeta superior corresponden a la comprobación inicial con H2. `tools/CaptureEvidence.ps1 -MySql` reproduce login, registro, edición, validación del servidor, eliminación, importación, cálculo y vista móvil usando la credencial local cifrada. Las capturas no incluyen contraseñas.

| Archivo | Evidencia |
|---|---|
| 01-login.png | Acceso al sistema |
| 02-departamentos.png | Catálogo y formulario |
| 03-turnos.png | Horario y tolerancia |
| 04-registro-listado.png | Registro y listado del empleado de prueba |
| 05-edicion.png | Formulario con datos precargados |
| 06-validaciones.png | Respuesta de validación del servidor |
| 07-eliminacion.png | Confirmación y ausencia del empleado eliminado |
| 08-asistencias.png | Resultados de importar y calcular |
| 09-movil.png | Adaptación a pantalla de 390 píxeles |

## Resultado de MySQL

Se comprobó el servicio MySQL84 en ejecución y el puerto 3306 accesible. El usuario instaló MySQL Server y Workbench y preparó el esquema mediante Configure-MySQL.ps1. La aplicación arrancó con ddl-auto=validate, sin perfil demo. La prueba integrada aprobó el flujo con datos temporales y la prueba de Chrome registró, editó y eliminó un empleado real antes de importar/calcular los datos de ejemplo.

`mysql-resultados.txt` acredita MySQL 8.4.13, la base pharmalogis_iam, siete tablas, cuatro empleados, seis registros externos, cinco internos y cuatro asistencias. Las consultas de duplicados devolvieron cero filas. `mysql-reinicio.txt` acredita recuperación de los resultados después de detener y volver a iniciar una instancia independiente del backend en 8081, conservando la aplicación del usuario en 8080.

Para repetir:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/Start-MySQL.ps1 -EvidenceOnly
powershell -NoProfile -ExecutionPolicy Bypass -File tools/Verify-MySQL-Restart.ps1
```

Para ejecutar MySqlIntegrationTests: definir DB_USERNAME y DB_PASSWORD localmente, establecer RUN_MYSQL_TESTS=true y ejecutar `system/mvnw.cmd -Dtest=MySqlIntegrationTests test`. Esta prueba es opcional en la ejecución general y se omite cuando no está definida esa variable; usa transacciones con rollback para no conservar los datos de verificación.

## Protocolo reproducible de comprobación MySQL

Después de instalar:

1. Ejecutar los scripts 01 y 02. Elegir fecha de demostración.
2. Configurar DB_USERNAME, DB_PASSWORD y la cuenta inicial como indica el README.
3. Arrancar sin perfil `demo`. El arranque debe validar el esquema con éxito.
4. Iniciar sesión, crear un departamento y turno temporales y registrar un empleado con ambos. Ejecutar consultas de evidencia para demostrar las FK.
5. Editar el empleado y verificar el cambio con SELECT. Intentar DNI/correo duplicado y comprobar el mensaje.
6. Eliminar el empleado sin historial y después sus catálogos; verificar su ausencia con SELECT. Intentar eliminar un catálogo asignado y comprobar el bloqueo.
7. Importar para la fecha del SQL. Esperado: 5 importadas y 1 DNI desconocido; segunda ejecución: 0 nuevas y 5 duplicadas. El registro desconocido continúa omitido.
8. Calcular. Esperado: Ana PRESENTE (540 min); Luis TARDANZA (25 min, 515 trabajados); Carla INCOMPLETA; Diego FALTA. Recalcular y comprobar que siguen existiendo 4 filas para la fecha.
9. Ejecutar `database/03-evidence-queries.sql`. Guardar resultados como `mysql-resultados.txt` y capturas de Workbench.
10. Detener y reiniciar Spring Boot sin perfil `demo`. Repetir SELECT y consultar el panel. Los registros deben permanecer.
11. Guardar capturas y resultados con fecha y versión del servidor. La entrega actual ya incluye la ejecución indicada.

No deben usarse resultados H2 como evidencia de MySQL. Los minutos reflejan los datos ficticios indicados; si se modifica el turno, las marcas o la fecha, recalcular los esperados.
