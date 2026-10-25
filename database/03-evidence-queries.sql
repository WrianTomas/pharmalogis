USE pharmalogis_iam;
SELECT VERSION() AS mysql_version, DATABASE() AS database_name;
SHOW TABLES;
SELECT e.id,e.document,e.full_name,d.name AS department,s.name AS shift,e.active
FROM employees e JOIN departments d ON d.id=e.department_id JOIN shifts s ON s.id=e.shift_id ORDER BY e.id;
SELECT * FROM markations_from_external_provider ORDER BY id;
SELECT m.id,e.document,m.external_id,m.occurred_at FROM markations m JOIN employees e ON e.id=m.employee_id ORDER BY m.id;
SELECT a.work_date,e.document,e.full_name,a.status,a.late_minutes,a.worked_minutes,a.incidents
FROM attendances a JOIN employees e ON e.id=a.employee_id ORDER BY a.work_date,e.full_name;
-- Deben devolver CERO filas: restricciones de unicidad e idempotencia.
SELECT employee_id,work_date,COUNT(*) FROM attendances GROUP BY employee_id,work_date HAVING COUNT(*)>1;
SELECT employee_id,occurred_at,COUNT(*) FROM markations GROUP BY employee_id,occurred_at HAVING COUNT(*)>1;
-- Repetir las consultas tras reiniciar Spring Boot para acreditar persistencia.
