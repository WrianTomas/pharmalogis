package com.pharmalogis.system.modules.attendance.domain;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Regla para turnos diurnos de un mismo día; minutos completos, sin horas
 * extras.
 */
public final class AttendancePolicy {
    public record Result(LocalDateTime firstIn, LocalDateTime lastOut, String status, int lateMinutes,
            int workedMinutes, String incidents) {
    }

    public Result calculate(LocalDate date, LocalTime start, LocalTime end, int tolerance, List<LocalDateTime> marks) {
        var sorted = marks.stream().filter(m -> m.toLocalDate().equals(date)).distinct().sorted().toList();
        if (sorted.isEmpty())
            return new Result(null, null, "FALTA", 0, 0, "Sin marcaciones");
        var first = sorted.getFirst();
        int late = (int) Math.max(0, ChronoUnit.MINUTES.between(date.atTime(start), first));
        boolean delayed = first.isAfter(date.atTime(start).plusMinutes(tolerance));
        if (sorted.size() == 1)
            return new Result(first, null, "INCOMPLETA", delayed ? late : 0, 0, "Falta marcación de salida");
        var last = sorted.getLast();
        String incidents = (delayed ? "Tardanza; " : "")
                + (last.isBefore(date.atTime(end)) ? "Salida anticipada; " : "")
                + (sorted.size() % 2 != 0 ? "Cantidad impar de marcaciones; " : "");
        // Suma pares entrada/salida; no cuenta descansos entre pares.
        int worked = 0;
        for (int i = 0; i + 1 < sorted.size(); i += 2)
            worked += (int) ChronoUnit.MINUTES.between(sorted.get(i), sorted.get(i + 1));
        return new Result(first, last, sorted.size() % 2 != 0 ? "INCOMPLETA" : delayed ? "TARDANZA" : "PRESENTE",
                delayed ? late : 0, worked, incidents);
    }
}
