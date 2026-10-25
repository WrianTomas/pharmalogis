package com.pharmalogis.system.modules.organization.presentation;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class EmployeeForm {
    private Long id;
    @NotBlank
    @Pattern(regexp = "[0-9]{8}", message = "El DNI debe tener 8 dígitos.")
    private String document;
    @NotBlank(message = "Ingrese los nombres.")
    @Size(max = 150)
    private String fullName;
    @NotBlank
    @Email(message = "Ingrese un correo válido.")
    @Size(max = 150)
    private String email;
    @NotNull(message = "Seleccione un departamento.")
    private Long departmentId;
    @NotNull(message = "Seleccione un turno.")
    private Long shiftId;
    private boolean active = true;
}