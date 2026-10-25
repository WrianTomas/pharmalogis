# Especificación de casos de uso — PharmaLogis

Actores: ADMIN y RRHH. Ambos pueden operar esta iteración. EMPLOYEE existe en IAM pero no tiene acceso al panel administrativo. El proveedor es una tabla simulada, no una API externa. Las operaciones son manuales.

## CU-IAM-01 — Iniciar sesión

- Objetivo: autenticar al personal autorizado.
- Precondiciones: cuenta existente, activa y contraseña BCrypt almacenada.
- Flujo: abrir `/login`; introducir correo y contraseña; validar CSRF; buscar cuenta; comprobar contraseña/estado; crear sesión; redirigir al panel.
- Alternativas: credenciales erróneas o cuenta inactiva → mensaje genérico; EMPLOYEE autenticado → acceso denegado al panel; sin sesión → redirección al login.
- Postcondición: sesión autenticada con rol ADMIN o RRHH. Cierre mediante POST `/logout` invalida la sesión.

## CU-RHH-01 — Gestionar turnos

- Objetivo: crear, listar, editar y eliminar jornadas.
- Precondición: sesión ADMIN o RRHH.
- Flujo: abrir Turnos; completar nombre, entrada, salida y tolerancia; validar; guardar; mostrar listado actualizado. Para editar, cargar por ID y guardar cambios. Para eliminar, confirmar y enviar POST.
- Alternativas: nombre duplicado, campo obligatorio vacío, tolerancia fuera de 0–60, salida no posterior a entrada o turno inexistente → error sin escritura. Turno asignado a empleados → eliminación bloqueada.
- Postcondición: jornada persistida o eliminación de un turno sin referencias.

## CU-RHH-02 — Gestionar departamentos

- Objetivo: mantener el catálogo organizacional.
- Precondición: sesión ADMIN o RRHH.
- Flujo: abrir Departamentos; registrar nombre y descripción; validar; persistir; listar. Para editar, seleccionar un registro. Para eliminar, confirmar.
- Alternativas: nombre vacío/duplicado, descripción superior a 250 caracteres, ID inexistente o departamento con empleados → error apropiado. El bloqueo evita registros huérfanos.
- Postcondición: departamento creado, modificado o eliminado de forma consistente.

## CU-RHH-03 — Gestionar empleados

- Objetivo: registrar empleados y asignar departamento y turno.
- Precondiciones: sesión autorizada y catálogos disponibles.
- Flujo: abrir Empleados; introducir nombres, DNI y correo; seleccionar departamento y turno; indicar activo; validar; guardar; listar. Editar recupera el registro por ID. Eliminar requiere confirmación.
- Alternativas: DNI distinto de 8 dígitos, correo inválido/duplicado, DNI duplicado, nombre vacío, asignación inexistente o ID inexistente → error. Empleado con historial → eliminación bloqueada; puede desactivarse.
- Postcondición: empleado y asignaciones persistidos; historial referencial intacto.

## CU-RHH-08 — Importar marcaciones

- Objetivo: transformar las marcaciones simuladas en marcaciones internas.
- Precondiciones: sesión autorizada, empleados registrados y fecha seleccionada. Puede no haber datos externos para la fecha.
- Flujo: pulsar Importar; consultar `markations_from_external_provider` en el intervalo [00:00 de la fecha, 00:00 del día siguiente); resolver empleado por DNI; omitir desconocidos; omitir ID externo o instante ya importado; persistir cada marcación dentro de una transacción; mostrar conteos.
- Alternativas: sin registros → cero importaciones; DNI desconocido → contar y omitir; duplicado → contar y omitir; conflicto concurrente → rollback y mensaje para reintentar.
- Postcondición: marcaciones internas sin duplicados, con trazabilidad al ID externo. Repetir la importación no agrega duplicados.

## CU-SYS-01 — Calcular asistencia diaria

- Objetivo: generar el resumen por empleado y día.
- Actor iniciador: ADMIN o RRHH (manual en esta versión).
- Precondiciones: fecha válida no futura y turno asignado; se recomienda importar primero.
- Flujo: pulsar Calcular; recorrer empleados activos; leer turno actual y marcaciones del día; ordenar y eliminar duplicados; aplicar `AttendancePolicy`; insertar o actualizar el registro único empleado/fecha; confirmar transacción; informar cantidad.
- Alternativas: sin marcas → FALTA; una marca o cantidad impar → INCOMPLETA; entrada posterior a tolerancia → TARDANZA; salida anterior al fin → incidencia; entrada dentro de tolerancia y pares completos → PRESENTE. Inactivos se omiten. Fecha futura → rechazo.
- Postcondición: un resumen por empleado activo y fecha, con minutos trabajados, minutos de tardanza e incidencias. Recálculo actualiza el mismo registro.
- Limitaciones explícitas: jornadas del mismo día, sin calendario laboral ni historial de asignaciones; recalcular fechas antiguas usa el turno actual.

## CU-RHH-05 — Consultar asistencias

- Objetivo: visualizar resultados y detectar incidencias.
- Precondiciones: sesión autorizada. Para obtener filas debe haberse calculado la fecha.
- Flujo: abrir panel; seleccionar fecha; opcionalmente filtrar empleado y estado; consultar; mostrar filas e indicadores.
- Alternativas: sin resultados → estado vacío orientativo; filtro con formato incorrecto → HTTP 400; sin sesión → login.
- Postcondición: consulta sin modificar datos. Los indicadores representan solo las filas filtradas.

## Trazabilidad

| Caso | Implementación principal | Prueba |
|---|---|---|
| CU-IAM-01 | DatabaseUserDetailsService, SecurityConfig, login.html | realSessionLoginUsesBcryptAndRejectsWrongPassword |
| CU-RHH-01 | OrganizationController, OrganizationService, shifts.html | shiftAndEmployeeCrudThroughForms; reversedShiftIsRejected |
| CU-RHH-02 | OrganizationController, OrganizationService, departments.html | departmentCrudPersistsAndDeletes |
| CU-RHH-03 | OrganizationController, OrganizationService, employees.html | shiftAndEmployeeCrudThroughForms; formValidationAndDuplicateAreVisible |
| CU-RHH-08 | ImportMarkationsHandler | importsAndRecalculatesWithoutDuplicatesAndFilters |
| CU-SYS-01 | CalculateAttendanceHandler, AttendancePolicy | AttendancePolicyTests; noMarksAbsentAndInactiveSkippedAndFutureRejected |
| CU-RHH-05 | GetAttendanceHandler, AttendanceController, attendance.html | allViewsRenderForHr; importsAndRecalculatesWithoutDuplicatesAndFilters |
