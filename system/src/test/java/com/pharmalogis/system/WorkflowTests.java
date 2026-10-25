package com.pharmalogis.system;

import com.pharmalogis.system.modules.organization.application.*;
import com.pharmalogis.system.modules.organization.domain.*;
import com.pharmalogis.system.modules.organization.infrastructure.*;
import com.pharmalogis.system.modules.attendance.application.*;
import com.pharmalogis.system.modules.attendance.infrastructure.*;
import com.pharmalogis.system.modules.integration.application.ImportMarkationsHandler;
import com.pharmalogis.system.modules.integration.infrastructure.*;
import com.pharmalogis.system.modules.iam.infraestructure.persistence.*;
import com.pharmalogis.system.modules.iam.domain.model.Role;
import com.pharmalogis.system.modules.iam.domain.model.UserStatus;
import com.pharmalogis.system.modules.iam.infraestructure.security.jwt.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WorkflowTests {
    @Autowired
    MockMvc mvc;
    @Autowired
    OrganizationService org;
    @Autowired
    OrganizationRepository organization;
    @Autowired
    ExternalMarkationJpaRepository external;
    @Autowired
    MarkationJpaRepository marks;
    @Autowired
    AttendanceJpaRepository attendance;
    @Autowired
    ImportMarkationsHandler importer;
    @Autowired
    CalculateAttendanceHandler calculator;
    @Autowired
    GetAttendanceHandler query;
    @Autowired
    JpaUserRepository users;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    JwtService jwt;
    Department department;
    Shift shift;
    Employee employee;
    LocalDate date = LocalDate.of(2026, 10, 22);

    @BeforeEach
    void setup() {
        department = org.save(new Department(null, "Logística", "Almacén"));
        shift = org.save(new Shift(null, "Diurno", LocalTime.of(8, 0), LocalTime.of(17, 0), 10));
        employee = org.save(
                new Employee(null, "12345678", "Ana Torres", "ana@example.com", department.id(), shift.id(), true));
    }

    void externalMark(String document, String time) {
        var e = new ExternalMarkationEntity();
        e.employeeDocument = document;
        e.occurredAt = date.atTime(LocalTime.parse(time));
        external.saveAndFlush(e);
    }

    @Test
    void anonymousRedirectAndApiUnauthorized() throws Exception {
        mvc.perform(get("/employees")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/api/private")).andExpect(status().isUnauthorized());
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Iniciar sesión")));
    }

    @Test
    void allViewsRenderForHr() throws Exception {
        for (String route : new String[] { "/employees", "/departments", "/shifts", "/attendance",
                "/employees/" + employee.id() + "/edit", "/departments/" + department.id() + "/edit",
                "/shifts/" + shift.id() + "/edit" })
            mvc.perform(get(route).with(user("hr").roles("RRHH"))).andExpect(status().isOk());
    }

    @Test
    void csrfRequiredAndEmployeeCannotManage() throws Exception {
        mvc.perform(
                post("/departments").with(user("hr").roles("RRHH")).param("name", "Ventas").param("description", ""))
                .andExpect(status().isForbidden());
        mvc.perform(get("/employees").with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());
    }

    @Test
    void realSessionLoginUsesBcryptAndRejectsWrongPassword() throws Exception {
        users.saveAndFlush(UserEntity.builder().id(UUID.randomUUID()).email("admin@test.pe")
                .encodedPassword(encoder.encode("StrongPassword123!")).role(Role.ADMIN).status(UserStatus.ACTIVE)
                .build());
        mvc.perform(post("/login").with(csrf()).param("email", "admin@test.pe").param("password", "incorrecta"))
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(post("/login").with(csrf()).param("email", "admin@test.pe").param("password", "StrongPassword123!"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void departmentCrudPersistsAndDeletes() throws Exception {
        mvc.perform(post("/departments").with(user("hr").roles("RRHH")).with(csrf()).param("name", "Ventas")
                .param("description", "Comercial")).andExpect(redirectedUrl("/departments"));
        var d = organization.departments().stream().filter(v -> v.name().equals("Ventas")).findFirst().orElseThrow();
        mvc.perform(post("/departments").with(user("hr").roles("RRHH")).with(csrf()).param("id", d.id().toString())
                .param("name", "Ventas regionales").param("description", "Comercial"))
                .andExpect(redirectedUrl("/departments"));
        assertEquals("Ventas regionales", organization.department(d.id()).orElseThrow().name());
        mvc.perform(post("/departments/" + d.id() + "/delete").with(user("hr").roles("RRHH")).with(csrf()))
                .andExpect(redirectedUrl("/departments"));
        assertTrue(organization.department(d.id()).isEmpty());
    }

    @Test
    void formValidationAndDuplicateAreVisible() throws Exception {
        mvc.perform(post("/employees").with(user("hr").roles("RRHH")).with(csrf()).param("fullName", "")
                .param("document", "12").param("email", "no-email")).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "document", "email", "fullName", "departmentId",
                        "shiftId"));
        mvc.perform(post("/departments").with(user("hr").roles("RRHH")).with(csrf()).param("name", "Logística")
                .param("description", "Duplicado")).andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("form"));
    }

    @Test
    void shiftAndEmployeeCrudThroughForms() throws Exception {
        mvc.perform(post("/shifts").with(user("hr").roles("RRHH")).with(csrf()).param("name", "Tarde")
                .param("startTime", "10:00").param("endTime", "18:00").param("toleranceMinutes", "5"))
                .andExpect(redirectedUrl("/shifts"));
        var s = organization.shifts().stream().filter(v -> v.name().equals("Tarde")).findFirst().orElseThrow();
        mvc.perform(post("/shifts").with(user("hr").roles("RRHH")).with(csrf()).param("id", s.id().toString())
                .param("name", "Tarde actualizada").param("startTime", "10:00").param("endTime", "19:00")
                .param("toleranceMinutes", "5")).andExpect(redirectedUrl("/shifts"));
        mvc.perform(post("/employees").with(user("hr").roles("RRHH")).with(csrf()).param("fullName", "Luis Rojas")
                .param("document", "87654321").param("email", "luis@test.pe")
                .param("departmentId", department.id().toString()).param("shiftId", s.id().toString())
                .param("active", "true")).andExpect(redirectedUrl("/employees"));
        var e = organization.employees().stream().filter(v -> v.document().equals("87654321")).findFirst()
                .orElseThrow();
        mvc.perform(post("/employees").with(user("hr").roles("RRHH")).with(csrf()).param("id", e.id().toString())
                .param("fullName", "Luis Rojas Editado").param("document", "87654321").param("email", "luis@test.pe")
                .param("departmentId", department.id().toString()).param("shiftId", s.id().toString())
                .param("active", "true")).andExpect(redirectedUrl("/employees"));
        assertEquals("Luis Rojas Editado", organization.employee(e.id()).orElseThrow().fullName());
        mvc.perform(post("/employees/" + e.id() + "/delete").with(user("hr").roles("RRHH")).with(csrf()))
                .andExpect(redirectedUrl("/employees"));
        assertTrue(organization.employee(e.id()).isEmpty());
        mvc.perform(post("/shifts/" + s.id() + "/delete").with(user("hr").roles("RRHH")).with(csrf()))
                .andExpect(redirectedUrl("/shifts"));
        assertTrue(organization.shift(s.id()).isEmpty());
    }

    @Test
    void assignedCatalogsAndDuplicateEmployeeAreRejected() {
        assertThrows(BusinessException.class, () -> org.deleteDepartment(department.id()));
        assertThrows(BusinessException.class, () -> org.deleteShift(shift.id()));
        assertThrows(BusinessException.class, () -> org
                .save(new Employee(null, "12345678", "Otro", "otro@test.pe", department.id(), shift.id(), true)));
    }

    @Test
    void reversedShiftIsRejected() {
        assertThrows(BusinessException.class,
                () -> org.save(new Shift(null, "Inválido", LocalTime.of(17, 0), LocalTime.of(8, 0), 10)));
    }

    @Test
    void importsAndRecalculatesWithoutDuplicatesAndFilters() throws Exception {
        externalMark("12345678", "08:25");
        externalMark("12345678", "17:00");
        externalMark("99999999", "08:00");
        externalMark("12345678", "08:25");
        var r = importer.handle(new ImportMarkationsHandler.Command(date));
        assertEquals(2, r.imported());
        assertEquals(1, r.duplicates());
        assertEquals(1, r.unknownEmployees());
        assertEquals(0, importer.handle(new ImportMarkationsHandler.Command(date)).imported());
        assertEquals(2, marks.count());
        calculator.handle(new CalculateAttendanceHandler.Command(date));
        calculator.handle(new CalculateAttendanceHandler.Command(date));
        assertEquals(1, attendance.count());
        var rows = query.handle(new GetAttendanceHandler.Query(date, employee.id(), "TARDANZA"));
        assertEquals(1, rows.size());
        assertEquals(25, rows.getFirst().lateMinutes());
        assertTrue(query.handle(new GetAttendanceHandler.Query(date, employee.id(), "FALTA")).isEmpty());
        mvc.perform(get("/attendance").param("date", date.toString()).with(user("hr").roles("RRHH")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("TARDANZA")));
    }

    @Test
    void noMarksAbsentAndInactiveSkippedAndFutureRejected() {
        org.save(new Employee(null, "22222222", "Inactivo", "inactive@test.pe", department.id(), shift.id(), false));
        assertEquals(1, calculator.handle(new CalculateAttendanceHandler.Command(date)));
        assertEquals("FALTA", query.handle(new GetAttendanceHandler.Query(date, null, "")).getFirst().status());
        assertThrows(BusinessException.class, () -> calculator
                .handle(new CalculateAttendanceHandler.Command(LocalDate.now(ZoneId.of("America/Lima")).plusDays(1))));
    }

    @Test
    void jwtContainsRealRoleAndRejectsTampering() {
        var id = UUID.randomUUID();
        users.saveAndFlush(
                UserEntity.builder().id(id).email("jwt@test.pe").encodedPassword(encoder.encode("StrongPassword123!"))
                        .role(Role.RRHH).status(UserStatus.ACTIVE).build());
        var token = jwt.generate(id.toString());
        assertEquals("ROLE_RRHH", jwt.extractRole(token));
        assertTrue(jwt.isValid(token));
        assertFalse(jwt.isValid("broken.token.value"));
    }

    @Test
    void malformedFilterAndMissingRecordAreHandled() throws Exception {
        mvc.perform(get("/attendance").param("date", "invalid").with(user("hr").roles("RRHH")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/employees/999999/edit").with(user("hr").roles("RRHH"))).andExpect(status().isNotFound());
    }
}
