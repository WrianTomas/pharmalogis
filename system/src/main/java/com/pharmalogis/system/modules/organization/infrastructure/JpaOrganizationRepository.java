package com.pharmalogis.system.modules.organization.infrastructure;

import com.pharmalogis.system.modules.organization.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaOrganizationRepository implements OrganizationRepository {
    private final DepartmentJpaRepository departments;
    private final ShiftJpaRepository shifts;
    private final EmployeeJpaRepository employees;

    private Department map(DepartmentEntity e) {
        return new Department(e.id, e.name, e.description);
    }

    private Shift map(ShiftEntity e) {
        return new Shift(e.id, e.name, e.startTime, e.endTime, e.toleranceMinutes);
    }

    private Employee map(EmployeeEntity e) {
        return new Employee(e.id, e.document, e.fullName, e.email, e.department.id, e.shift.id, e.active);
    }

    public List<Department> departments() {
        return departments.findAll().stream().map(this::map).sorted(Comparator.comparing(Department::name)).toList();
    }

    public List<Shift> shifts() {
        return shifts.findAll().stream().map(this::map).sorted(Comparator.comparing(Shift::name)).toList();
    }

    public List<Employee> employees() {
        return employees.findAll().stream().map(this::map).sorted(Comparator.comparing(Employee::fullName)).toList();
    }

    public Optional<Department> department(Long id) {
        return departments.findById(id).map(this::map);
    }

    public Optional<Shift> shift(Long id) {
        return shifts.findById(id).map(this::map);
    }

    public Optional<Employee> employee(Long id) {
        return employees.findById(id).map(this::map);
    }

    @Transactional
    public Department save(Department v) {
        var e = new DepartmentEntity();
        e.id = v.id();
        e.name = v.name();
        e.description = v.description();
        return map(departments.saveAndFlush(e));
    }

    @Transactional
    public Shift save(Shift v) {
        var e = new ShiftEntity();
        e.id = v.id();
        e.name = v.name();
        e.startTime = v.startTime();
        e.endTime = v.endTime();
        e.toleranceMinutes = v.toleranceMinutes();
        return map(shifts.saveAndFlush(e));
    }

    @Transactional
    public Employee save(Employee v) {
        var e = new EmployeeEntity();
        e.id = v.id();
        e.document = v.document();
        e.fullName = v.fullName();
        e.email = v.email();
        e.active = v.active();
        e.department = departments.getReferenceById(v.departmentId());
        e.shift = shifts.getReferenceById(v.shiftId());
        return map(employees.saveAndFlush(e));
    }

    @Transactional
    public void deleteDepartment(Long id) {
        departments.deleteById(id);
        departments.flush();
    }

    @Transactional
    public void deleteShift(Long id) {
        shifts.deleteById(id);
        shifts.flush();
    }

    @Transactional
    public void deleteEmployee(Long id) {
        employees.deleteById(id);
        employees.flush();
    }
}