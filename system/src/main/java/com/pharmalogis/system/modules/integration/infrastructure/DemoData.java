package com.pharmalogis.system.modules.integration.infrastructure;

import com.pharmalogis.system.modules.organization.domain.*;
import com.pharmalogis.system.modules.organization.application.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import java.time.*;

@Configuration
@Profile("demo")
@RequiredArgsConstructor
public class DemoData {
    private final OrganizationService organization;
    private final ExternalMarkationJpaRepository external;

    @Bean
    ApplicationRunner seedDemo() {
        return args -> {
            var logistics = organization.save(new Department(null, "Logística", "Distribución farmacéutica"));
            var quality = organization.save(new Department(null, "Calidad", "Control y trazabilidad"));
            var shift = organization
                    .save(new Shift(null, "Administrativo", LocalTime.of(8, 0), LocalTime.of(17, 0), 10));
            String[] names = { "Ana Torres", "Luis Mendoza", "Carla Rojas", "Diego Flores" };
            for (int i = 0; i < names.length; i++)
                organization.save(
                        new Employee(null, "7000000" + (i + 1), names[i], "empleado" + (i + 1) + "@pharmalogis.pe",
                                i % 2 == 0 ? logistics.id() : quality.id(), shift.id(), true));
            LocalDate date = LocalDate.now(ZoneId.of("America/Lima"));
            String[][] marks = { { "70000001", "08:00" }, { "70000001", "17:00" }, { "70000002", "08:25" },
                    { "70000002", "17:00" }, { "70000003", "08:05" }, { "99999999", "08:00" } };
            for (var mark : marks) {
                var row = new ExternalMarkationEntity();
                row.employeeDocument = mark[0];
                row.occurredAt = date.atTime(LocalTime.parse(mark[1]));
                external.save(row);
            }
        };
    }
}
