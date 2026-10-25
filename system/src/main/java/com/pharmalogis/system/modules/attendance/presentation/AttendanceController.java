package com.pharmalogis.system.modules.attendance.presentation;

import com.pharmalogis.system.modules.attendance.application.*;
import com.pharmalogis.system.modules.integration.application.ImportMarkationsHandler;
import com.pharmalogis.system.modules.organization.domain.OrganizationRepository;
import com.pharmalogis.system.modules.organization.application.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.*;

@Controller
@RequiredArgsConstructor
public class AttendanceController {
    private final GetAttendanceHandler query;
    private final CalculateAttendanceHandler calculate;
    private final ImportMarkationsHandler importer;
    private final OrganizationRepository organization;

    @GetMapping("/attendance")
    String list(@RequestParam(required = false) LocalDate date, @RequestParam(required = false) Long employeeId,
            @RequestParam(defaultValue = "") String status, Model model) {
        if (date == null)
            date = LocalDate.now(ZoneId.of("America/Lima"));
        var rows = query.handle(new GetAttendanceHandler.Query(date, employeeId, status));
        model.addAttribute("date", date);
        model.addAttribute("employeeId", employeeId);
        model.addAttribute("status", status);
        model.addAttribute("rows", rows);
        model.addAttribute("employees", organization.employees());
        model.addAttribute("present", rows.stream().filter(r -> r.status().equals("PRESENTE")).count());
        model.addAttribute("late", rows.stream().filter(r -> r.status().equals("TARDANZA")).count());
        model.addAttribute("absent", rows.stream().filter(r -> r.status().equals("FALTA")).count());
        model.addAttribute("incomplete", rows.stream().filter(r -> r.status().equals("INCOMPLETA")).count());
        return "attendance";
    }

    @PostMapping("/attendance/import")
    String importMarks(@RequestParam LocalDate date, RedirectAttributes flash) {
        try {
            var r = importer.handle(new ImportMarkationsHandler.Command(date));
            flash.addFlashAttribute("success", "Importadas: " + r.imported() + " · Duplicadas: " + r.duplicates()
                    + " · DNI desconocido: " + r.unknownEmployees());
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "Otra importación modificó los registros. Vuelva a intentar.");
        }
        return "redirect:/attendance?date=" + date;
    }

    @PostMapping("/attendance/calculate")
    String calculate(@RequestParam LocalDate date, RedirectAttributes flash) {
        try {
            int count = calculate.handle(new CalculateAttendanceHandler.Command(date));
            flash.addFlashAttribute("success",
                    "Asistencias calculadas: " + count + ". El recálculo actualiza los resultados sin duplicarlos.");
        } catch (BusinessException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "Se detectó un cálculo simultáneo. Vuelva a intentar.");
        }
        return "redirect:/attendance?date=" + date;
    }
}
