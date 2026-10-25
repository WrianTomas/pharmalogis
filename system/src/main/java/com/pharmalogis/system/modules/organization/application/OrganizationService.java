package com.pharmalogis.system.modules.organization.application;

import com.pharmalogis.system.modules.organization.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {
    private final OrganizationRepository repository;

    public Department save(Department value) {
        if (value.id() != null)
            repository.department(value.id()).orElseThrow(() -> new BusinessException("Departamento inexistente."));
        String name = value.name().strip();
        if (repository.departments().stream()
                .anyMatch(d -> !Objects.equals(d.id(), value.id()) && d.name().equalsIgnoreCase(name)))
            throw new BusinessException("Ya existe un departamento con ese nombre.");
        return repository.save(new Department(value.id(), name, value.description().strip()));
    }

    public Shift save(Shift value) {
        if (value.id() != null)
            repository.shift(value.id()).orElseThrow(() -> new BusinessException("Turno inexistente."));
        if (!value.endTime().isAfter(value.startTime()))
            throw new BusinessException(
                    "La salida debe ser posterior a la entrada. Esta versión admite turnos del mismo día.");
        if (value.toleranceMinutes() < 0 || value.toleranceMinutes() > 60)
            throw new BusinessException("La tolerancia debe estar entre 0 y 60 minutos.");
        String name = value.name().strip();
        if (repository.shifts().stream()
                .anyMatch(s -> !Objects.equals(s.id(), value.id()) && s.name().equalsIgnoreCase(name)))
            throw new BusinessException("Ya existe un turno con ese nombre.");
        return repository
                .save(new Shift(value.id(), name, value.startTime(), value.endTime(), value.toleranceMinutes()));
    }

    public Employee save(Employee value) {
        if (value.id() != null)
            repository.employee(value.id()).orElseThrow(() -> new BusinessException("Empleado inexistente."));
        repository.department(value.departmentId())
                .orElseThrow(() -> new BusinessException("Seleccione un departamento existente."));
        repository.shift(value.shiftId()).orElseThrow(() -> new BusinessException("Seleccione un turno existente."));
        String email = value.email().strip().toLowerCase(java.util.Locale.ROOT);
        if (repository.employees().stream().anyMatch(e -> !Objects.equals(e.id(), value.id())
                && (e.document().equals(value.document()) || e.email().equalsIgnoreCase(email))))
            throw new BusinessException("El documento o correo ya pertenece a otro empleado.");
        return repository.save(new Employee(value.id(), value.document(), value.fullName().strip(), email,
                value.departmentId(), value.shiftId(), value.active()));
    }

    public void deleteDepartment(Long id) {
        repository.department(id).orElseThrow(() -> new BusinessException("Departamento inexistente."));
        if (repository.employees().stream().anyMatch(e -> e.departmentId().equals(id)))
            throw new BusinessException("No se puede eliminar un departamento con empleados asignados.");
        repository.deleteDepartment(id);
    }

    public void deleteShift(Long id) {
        repository.shift(id).orElseThrow(() -> new BusinessException("Turno inexistente."));
        if (repository.employees().stream().anyMatch(e -> e.shiftId().equals(id)))
            throw new BusinessException("No se puede eliminar un turno con empleados asignados.");
        repository.deleteShift(id);
    }

    public void deleteEmployee(Long id) {
        repository.employee(id).orElseThrow(() -> new BusinessException("Empleado inexistente."));
        repository.deleteEmployee(id);
    }
}
