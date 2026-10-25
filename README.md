# PharmaLogis — Avance del Proyecto 3

Sistema de asistencia: **Thymeleaf → Spring MVC → servicios → JPA → MySQL**.

Implementa login, CRUD de departamentos/turnos/empleados, importación manual de marcaciones simuladas, cálculo diario e indicadores de asistencia. Conserva la API JWT inicial.

## Abrir la demostración

Requisitos: Java 21. Maven Wrapper descarga Maven y dependencias cuando sea necesario.

```powershell
cd system
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Abrir http://localhost:8080. En VS Code también se puede ejecutar **PharmaLogis: demostración (H2)** desde Ejecutar y depurar.

Cuenta exclusiva de demostración: `admin@pharmalogis.pe` / `DemoPharma2026!`.

El perfil `demo` utiliza **H2 en memoria**; sus datos se eliminan al detener la aplicación. Carga cuatro empleados y seis marcaciones ficticias para la fecha actual de Lima. En Asistencias: importar marcaciones y después calcular. Resultado: una asistencia puntual, una tardanza, una incompleta y una falta.

## Instalar y configurar MySQL en Windows

Para iniciar el sistema con la configuración local de MySQL, ejecutar desde la raíz del proyecto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tools/Start-MySQL.ps1
```

La primera preparación se realiza con `tools/Configure-MySQL.ps1`. Los scripts guardan credenciales cifradas con DPAPI en `.local/`, excluida de la entrega y del control de versiones. Solo puede descifrarlas la misma cuenta de Windows. En otro equipo ejecutar la preparación nuevamente. Si el puerto 8080 está ocupado por otra ejecución, detener esa ejecución antes de arrancar.

Desde VS Code: Terminal → Ejecutar tarea → **PharmaLogis: iniciar con MySQL**. La tarea carga las credenciales cifradas; la configuración de depuración H2 permanece disponible por separado.

Para una instalación nueva:

1. Descargar [MySQL Community Server 8.4 LTS](https://dev.mysql.com/downloads/mysql/8.4.html?os=3), opción **Windows (x86, 64-bit), MSI Installer**. Instalar y configurar puerto `3306`, contraseña propia de `root` y servicio de Windows.
2. Instalar [MySQL Workbench](https://dev.mysql.com/downloads/workbench/). Crear conexión a `localhost:3306` como `root`.
3. En Workbench abrir y ejecutar `database/01-schema.sql`. En una base existente, respaldar y revisar las tablas antes de aplicar cambios: `IF NOT EXISTS` no migra estructuras antiguas.
4. Crear un usuario de aplicación. Sustituir la contraseña de ejemplo antes de ejecutar:

```sql
CREATE USER 'pharmalogis'@'localhost' IDENTIFIED BY 'CAMBIAR_POR_UNA_CLAVE_PROPIA';
GRANT SELECT, INSERT, UPDATE, DELETE ON pharmalogis_iam.* TO 'pharmalogis'@'localhost';
```

5. Opcionalmente ejecutar `database/02-demo-data.sql` en la base de demostración. Cambiar `@demo_date` por la fecha de exposición.
6. En PowerShell, dentro de la carpeta `system`, configurar las variables. Introducir las contraseñas localmente:

```powershell
$env:DB_USERNAME = 'pharmalogis'
$dbCredential = Get-Credential -UserName 'pharmalogis' -Message 'Contraseña del usuario MySQL'
$env:DB_PASSWORD = $dbCredential.GetNetworkCredential().Password
$env:BOOTSTRAP_ADMIN_EMAIL = 'admin@pharmalogis.pe'
$adminSecret = Read-Host 'Contraseña inicial del administrador (mínimo 12 caracteres)' -AsSecureString
$env:BOOTSTRAP_ADMIN_PASSWORD = [Net.NetworkCredential]::new('', $adminSecret).Password
$jwtBytes = New-Object byte[] 48
$jwtRng = [Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtBytes)
$jwtRng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
.\mvnw.cmd spring-boot:run
```

El primer arranque crea la cuenta si no existe. Las ejecuciones posteriores no cambian su contraseña. Quitar `BOOTSTRAP_ADMIN_PASSWORD` después de crearla. No activar `demo` cuando se quiera usar MySQL. El modo normal valida el esquema y no elimina ni recrea tablas.

Para otro servidor, configurar `DB_URL`, por ejemplo `jdbc:mysql://localhost:3306/pharmalogis_iam?serverTimezone=America/Lima`.

Alternativa si ya se dispone de Docker: definir `MYSQL_ROOT_PASSWORD` y `DB_PASSWORD` localmente y ejecutar `docker compose up -d`. El volumen conserva los datos; el script inicial solo se aplica al crear un volumen nuevo. Docker no es necesario para la instalación con MSI.

## Verificación y entrega

```powershell
cd system
.\mvnw.cmd test
.\mvnw.cmd package
java -jar target/system-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

Se aprobaron 22 pruebas con H2 y una adicional con MySQL real. Se comprobó el flujo MVC, seguridad, persistencia JPA, validaciones, CRUD e idempotencia; además, las consultas y el reinicio de una instancia temporal acreditaron la conservación de datos en MySQL. Los resultados y capturas se encuentran en `docs/evidencias/`. MySqlIntegrationTests se activa con RUN_MYSQL_TESTS=true y credenciales locales; se omite en ejecuciones sin MySQL.

- Informe: [Word del avance 3](docs/PharmaLogis_Informe_Integrado_Avance3.docx).
- Especificaciones: [docs/casos-de-uso.md](docs/casos-de-uso.md).
- UML: [docs/diagramas](docs/diagramas).
- Pruebas y pasos para MySQL: [docs/pruebas.md](docs/pruebas.md).
- Evidencias: [docs/evidencias](docs/evidencias).

## Trabajo en equipo

Cada integrante debe clonar el repositorio y crear su propia configuración de MySQL. Las credenciales locales no se incluyen. Para probar sin instalar MySQL se puede usar el perfil `demo` indicado arriba.

Antes de cambiar el código, crear una rama con `git switch -c nombre-del-cambio`. Subir la rama y abrir un pull request para revisar los cambios antes de incorporarlos a `main`.

## Reglas de esta iteración

DNI de 8 dígitos y correo únicos; departamentos y turnos con nombre único; turnos del mismo día y tolerancia de 0 a 60 minutos. Una falta implica ausencia de marcaciones; una cantidad impar implica asistencia incompleta. La tardanza se determina después de la tolerancia y se cuenta desde el inicio del turno. El tiempo trabajado suma pares entrada/salida, sin incluir descansos entre pares. No se generan horas extras.

Importar y recalcular no duplica resultados. Los empleados inactivos se excluyen de nuevos cálculos; su historial permanece. Se impide borrar departamentos/turnos asignados y empleados con marcaciones/asistencias. Todos los cambios web requieren CSRF y rol ADMIN o RRHH. Las credenciales se almacenan con BCrypt.

Pendiente de futuras iteraciones: turnos nocturnos, calendario laboral, vacaciones/feriados, fecha de alta y baja, justificaciones, historial de asignaciones, auditoría, paginación, proveedor biométrico real y automatización. Los cálculos históricos usan la asignación actual; la demo académica no implementa nómina.
