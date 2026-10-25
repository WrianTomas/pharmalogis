package com.pharmalogis.system;

import com.pharmalogis.system.modules.organization.application.OrganizationService;
import com.pharmalogis.system.modules.organization.domain.*;
import com.pharmalogis.system.modules.integration.application.ImportMarkationsHandler;
import com.pharmalogis.system.modules.integration.infrastructure.*;
import com.pharmalogis.system.modules.attendance.application.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "debug=false")
@ActiveProfiles("mysql-verification")
@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_TESTS", matches = "true")
@Transactional
class MySqlIntegrationTests {
    @Autowired
    OrganizationService service;
    @Autowired
    OrganizationRepository organization;
    @Autowired
    ExternalMarkationJpaRepository external;
    @Autowired
    ImportMarkationsHandler importer;
    @Autowired
    CalculateAttendanceHandler calculator;
    @Autowired
    GetAttendanceHandler query;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void realMySqlCrudImportAndCalculation() {
        assertTrue(jdbc.queryForObject("SELECT VERSION()", String.class).startsWith("8."));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var d = service.save(new Department(null, "Verificación " + suffix, "Prueba real MySQL"));
        var s = service.save(new Shift(null, "Verificación " + suffix, LocalTime.of(8, 0), LocalTime.of(17, 0), 10));
        String document;
        do {
            document = String.valueOf(java.util.concurrent.ThreadLocalRandom.current().nextInt(80000000, 89999999));
        } while (jdbc.queryForObject("SELECT COUNT(*) FROM employees WHERE document=?", Integer.class, document) > 0);
        var e = service.save(
                new Employee(null, document, "Prueba MySQL", "verify-" + suffix + "@test.pe", d.id(), s.id(), true));
        service.save(new Employee(e.id(), document, "Prueba editada", "verify-" + suffix + "@test.pe", d.id(), s.id(),
                true));
        assertEquals("Prueba editada",
                jdbc.queryForObject("SELECT full_name FROM employees WHERE id=?", String.class, e.id()));
        var date = LocalDate.now(ZoneId.of("America/Lima")).minusDays(1);
        for (var time : new String[] { "08:25", "17:00" }) {
            var m = new ExternalMarkationEntity();
            m.employeeDocument = document;
            m.occurredAt = date.atTime(LocalTime.parse(time));
            external.saveAndFlush(m);
        }
        importer.handle(new ImportMarkationsHandler.Command(date));
        importer.handle(new ImportMarkationsHandler.Command(date));
        assertEquals(2,
                jdbc.queryForObject("SELECT COUNT(*) FROM markations WHERE employee_id=?", Integer.class, e.id()));
        calculator.handle(new CalculateAttendanceHandler.Command(date));
        calculator.handle(new CalculateAttendanceHandler.Command(date));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM attendances WHERE employee_id=? AND work_date=?",
                Integer.class, e.id(), date));
        var row = query.handle(new GetAttendanceHandler.Query(date, e.id(), "")).getFirst();
        assertEquals("TARDANZA", row.status());
        assertEquals(25, row.lateMinutes());
        assertEquals(515, row.workedMinutes());
        // Eliminación real de un empleado sin historial y sus catálogos, en la misma
        // transacción de prueba.
        var deletable = service.save(new Employee(null, "9" + document.substring(1), "Eliminar MySQL",
                "delete-" + suffix + "@test.pe", d.id(), s.id(), false));
        service.deleteEmployee(deletable.id());
        assertTrue(organization.employee(deletable.id()).isEmpty());
        // JUnit revierte todos los datos creados y los resúmenes de esta fecha al
        // finalizar.
    }
}
