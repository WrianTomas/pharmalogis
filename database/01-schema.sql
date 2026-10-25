-- PharmaLogis / Avance 3. Ejecutar una vez en una base nueva (MySQL 8+).
CREATE DATABASE IF NOT EXISTS pharmalogis_iam CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pharmalogis_iam;

CREATE TABLE IF NOT EXISTS iam_users (
 id BINARY(16) PRIMARY KEY,
 email VARCHAR(255) NOT NULL UNIQUE,
 encoded_password VARCHAR(255) NOT NULL,
 role VARCHAR(30) NOT NULL,
 status VARCHAR(30) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 CONSTRAINT chk_user_role CHECK (role IN ('ADMIN','RRHH','EMPLOYEE')),
 CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE','INACTIVE'))
);
CREATE TABLE IF NOT EXISTS departments (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL UNIQUE,
 description VARCHAR(250) NOT NULL
);
CREATE TABLE IF NOT EXISTS shifts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(100) NOT NULL UNIQUE,
 start_time TIME(0) NOT NULL,
 end_time TIME(0) NOT NULL,
 tolerance_minutes INT NOT NULL,
 CONSTRAINT chk_shift_hours CHECK (end_time > start_time),
 CONSTRAINT chk_shift_tolerance CHECK (tolerance_minutes BETWEEN 0 AND 60)
);
CREATE TABLE IF NOT EXISTS employees (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 document VARCHAR(8) NOT NULL UNIQUE,
 full_name VARCHAR(150) NOT NULL,
 email VARCHAR(150) NOT NULL UNIQUE,
 department_id BIGINT NOT NULL,
 shift_id BIGINT NOT NULL,
 active BOOLEAN NOT NULL,
 CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE RESTRICT,
 CONSTRAINT fk_employee_shift FOREIGN KEY (shift_id) REFERENCES shifts(id) ON DELETE RESTRICT,
 CONSTRAINT chk_employee_document CHECK (document REGEXP '^[0-9]{8}$')
);
CREATE TABLE IF NOT EXISTS markations_from_external_provider (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 employee_document VARCHAR(8) NOT NULL,
 occurred_at DATETIME(6) NOT NULL,
 INDEX ix_external_date (occurred_at)
);
CREATE TABLE IF NOT EXISTS markations (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 external_id BIGINT NOT NULL UNIQUE,
 employee_id BIGINT NOT NULL,
 occurred_at DATETIME(6) NOT NULL,
 CONSTRAINT uq_employee_mark UNIQUE (employee_id,occurred_at),
 CONSTRAINT fk_mark_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT,
 CONSTRAINT fk_mark_source FOREIGN KEY (external_id) REFERENCES markations_from_external_provider(id) ON DELETE RESTRICT
);
CREATE TABLE IF NOT EXISTS attendances (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 employee_id BIGINT NOT NULL,
 work_date DATE NOT NULL,
 first_in DATETIME(6),
 last_out DATETIME(6),
 status VARCHAR(20) NOT NULL,
 late_minutes INT NOT NULL,
 worked_minutes INT NOT NULL,
 incidents VARCHAR(250) NOT NULL,
 CONSTRAINT uq_employee_attendance UNIQUE (employee_id,work_date),
 CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE RESTRICT,
 CONSTRAINT chk_attendance_status CHECK (status IN ('PRESENTE','TARDANZA','FALTA','INCOMPLETA')),
 CONSTRAINT chk_attendance_minutes CHECK (late_minutes >= 0 AND worked_minutes >= 0),
 INDEX ix_attendance_date (work_date)
);
