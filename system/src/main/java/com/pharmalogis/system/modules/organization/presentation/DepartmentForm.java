package com.pharmalogis.system.modules.organization.presentation;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DepartmentForm {
    private Long id;
    @NotBlank(message = "Ingrese el nombre.")
    @Size(max = 100)
    private String name;
    @NotNull
    @Size(max = 250)
    private String description = "";
}