package com.pharmalogis.system.modules.organization.presentation;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalTime;
import lombok.Data;

@Data
public class ShiftForm {
    private Long id;
    @NotBlank(message = "Ingrese el nombre.")
    @Size(max = 100)
    private String name;
    @NotNull(message = "Ingrese la hora de entrada.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;
    @NotNull(message = "Ingrese la hora de salida.")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;
    @NotNull
    @Min(0)
    @Max(60)
    private Integer toleranceMinutes = 10;
}