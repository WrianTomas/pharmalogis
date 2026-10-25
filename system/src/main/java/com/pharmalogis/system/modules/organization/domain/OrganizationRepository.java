package com.pharmalogis.system.modules.organization.domain;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository {
    List<Department> departments();

    List<Shift> shifts();

    List<Employee> employees();

    Optional<Department> department(Long id);

    Optional<Shift> shift(Long id);

    Optional<Employee> employee(Long id);

    Department save(Department value);

    Shift save(Shift value);

    Employee save(Employee value);

    void deleteDepartment(Long id);

    void deleteShift(Long id);

    void deleteEmployee(Long id);
}
