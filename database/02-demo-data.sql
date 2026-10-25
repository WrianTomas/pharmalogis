-- Datos ficticios. Ejecutar SOLO para la demostración académica, después de 01-schema.sql.
-- No sobrescribe registros existentes; los empleados son independientes de las cuentas IAM.
USE pharmalogis_iam;
INSERT INTO departments(name,description) SELECT 'Logística','Distribución farmacéutica'
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE name='Logística');
INSERT INTO departments(name,description) SELECT 'Calidad','Control y trazabilidad'
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE name='Calidad');
INSERT INTO shifts(name,start_time,end_time,tolerance_minutes) SELECT 'Administrativo','08:00','17:00',10
WHERE NOT EXISTS (SELECT 1 FROM shifts WHERE name='Administrativo');
INSERT INTO employees(document,full_name,email,department_id,shift_id,active)
SELECT '70000001','Ana Torres','empleado1@pharmalogis.pe',d.id,s.id,TRUE FROM departments d CROSS JOIN shifts s
WHERE d.name='Logística' AND s.name='Administrativo' AND NOT EXISTS (SELECT 1 FROM employees WHERE document='70000001');
INSERT INTO employees(document,full_name,email,department_id,shift_id,active)
SELECT '70000002','Luis Mendoza','empleado2@pharmalogis.pe',d.id,s.id,TRUE FROM departments d CROSS JOIN shifts s
WHERE d.name='Calidad' AND s.name='Administrativo' AND NOT EXISTS (SELECT 1 FROM employees WHERE document='70000002');
INSERT INTO employees(document,full_name,email,department_id,shift_id,active)
SELECT '70000003','Carla Rojas','empleado3@pharmalogis.pe',d.id,s.id,TRUE FROM departments d CROSS JOIN shifts s
WHERE d.name='Logística' AND s.name='Administrativo' AND NOT EXISTS (SELECT 1 FROM employees WHERE document='70000003');
INSERT INTO employees(document,full_name,email,department_id,shift_id,active)
SELECT '70000004','Diego Flores','empleado4@pharmalogis.pe',d.id,s.id,TRUE FROM departments d CROSS JOIN shifts s
WHERE d.name='Calidad' AND s.name='Administrativo' AND NOT EXISTS (SELECT 1 FROM employees WHERE document='70000004');
-- Cambiar @demo_date si la exposición se realiza otro día (hora de Lima).
SET @demo_date = '2026-10-23';
INSERT INTO markations_from_external_provider(employee_document,occurred_at)
SELECT v.document,TIMESTAMP(@demo_date,v.hour) FROM (
 SELECT '70000001' document,'08:00:00' hour UNION ALL SELECT '70000001','17:00:00'
 UNION ALL SELECT '70000002','08:25:00' UNION ALL SELECT '70000002','17:00:00'
 UNION ALL SELECT '70000003','08:05:00' UNION ALL SELECT '99999999','08:00:00'
) v WHERE NOT EXISTS (SELECT 1 FROM markations_from_external_provider m WHERE m.employee_document=v.document AND m.occurred_at=TIMESTAMP(@demo_date,v.hour));
