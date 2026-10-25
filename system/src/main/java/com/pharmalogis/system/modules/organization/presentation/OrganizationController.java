package com.pharmalogis.system.modules.organization.presentation;

import com.pharmalogis.system.modules.organization.application.*;
import com.pharmalogis.system.modules.organization.domain.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationRepository repository;
    private final OrganizationService service;

    @ModelAttribute
    void catalogs(Model m) {
        m.addAttribute("departments", repository.departments());
        m.addAttribute("shifts", repository.shifts());
    }

    @GetMapping("/departments")
    String departments(Model m) {
        return departmentForm(null, m);
    }

    @GetMapping("/departments/{id}/edit")
    String departmentForm(@PathVariable(required = false) Long id, Model m) {
        var f = new DepartmentForm();
        if (id != null) {
            var d = repository.department(id).orElseThrow(() -> new BusinessException("Departamento inexistente."));
            f.setId(id);
            f.setName(d.name());
            f.setDescription(d.description());
        }
        m.addAttribute("form", f);
        return "departments";
    }

    @PostMapping("/departments")
    String saveDepartment(@Valid @ModelAttribute("form") DepartmentForm f, BindingResult errors,
            RedirectAttributes flash) {
        if (!errors.hasErrors())
            try {
                service.save(new Department(f.getId(), f.getName(), f.getDescription()));
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors.reject("duplicate", "El nombre ya está registrado.");
            }
        if (errors.hasErrors())
            return "departments";
        flash.addFlashAttribute("success", "Departamento guardado.");
        return "redirect:/departments";
    }

    @GetMapping("/shifts")
    String shifts(Model m) {
        return shiftForm(null, m);
    }

    @GetMapping("/shifts/{id}/edit")
    String shiftForm(@PathVariable(required = false) Long id, Model m) {
        var f = new ShiftForm();
        if (id != null) {
            var s = repository.shift(id).orElseThrow(() -> new BusinessException("Turno inexistente."));
            f.setId(id);
            f.setName(s.name());
            f.setStartTime(s.startTime());
            f.setEndTime(s.endTime());
            f.setToleranceMinutes(s.toleranceMinutes());
        }
        m.addAttribute("form", f);
        return "shifts";
    }

    @PostMapping("/shifts")
    String saveShift(@Valid @ModelAttribute("form") ShiftForm f, BindingResult errors, RedirectAttributes flash) {
        if (!errors.hasErrors())
            try {
                service.save(
                        new Shift(f.getId(), f.getName(), f.getStartTime(), f.getEndTime(), f.getToleranceMinutes()));
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors.reject("duplicate", "El nombre ya está registrado.");
            }
        if (errors.hasErrors())
            return "shifts";
        flash.addFlashAttribute("success", "Turno guardado.");
        return "redirect:/shifts";
    }

    @GetMapping("/employees")
    String employees(Model m) {
        return employeeForm(null, m);
    }

    @GetMapping("/employees/{id}/edit")
    String employeeForm(@PathVariable(required = false) Long id, Model m) {
        var f = new EmployeeForm();
        if (id != null) {
            var e = repository.employee(id).orElseThrow(() -> new BusinessException("Empleado inexistente."));
            f.setId(id);
            f.setDocument(e.document());
            f.setFullName(e.fullName());
            f.setEmail(e.email());
            f.setDepartmentId(e.departmentId());
            f.setShiftId(e.shiftId());
            f.setActive(e.active());
        }
        m.addAttribute("form", f);
        m.addAttribute("employees", repository.employees());
        return "employees";
    }

    @PostMapping("/employees")
    String saveEmployee(@Valid @ModelAttribute("form") EmployeeForm f, BindingResult errors, Model m,
            RedirectAttributes flash) {
        if (!errors.hasErrors())
            try {
                service.save(new Employee(f.getId(), f.getDocument(), f.getFullName(), f.getEmail(),
                        f.getDepartmentId(), f.getShiftId(), f.isActive()));
            } catch (BusinessException ex) {
                errors.reject("business", ex.getMessage());
            } catch (DataIntegrityViolationException ex) {
                errors.reject("duplicate", "El documento o correo ya está registrado.");
            }
        if (errors.hasErrors()) {
            m.addAttribute("employees", repository.employees());
            return "employees";
        }
        flash.addFlashAttribute("success", "Empleado guardado.");
        return "redirect:/employees";
    }

    @PostMapping("/{catalog:departments|shifts|employees}/{id}/delete")
    String delete(@PathVariable String catalog, @PathVariable Long id, RedirectAttributes flash) {
        try {
            switch (catalog) {
            case "departments" -> service.deleteDepartment(id);
            case "shifts" -> service.deleteShift(id);
            default -> service.deleteEmployee(id);
            }
            flash.addFlashAttribute("success", "Registro eliminado.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error",
                    "El registro tiene marcaciones o asistencias asociadas. Puede desactivar al empleado.");
        }
        return "redirect:/" + catalog;
    }
}
