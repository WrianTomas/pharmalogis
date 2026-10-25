package com.pharmalogis.system;

import com.pharmalogis.system.modules.attendance.domain.AttendancePolicy;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AttendancePolicyTests {
    private final LocalDate date = LocalDate.of(2026, 10, 22);

    private AttendancePolicy.Result result(String... times) {
        return new AttendancePolicy().calculate(date, LocalTime.of(8, 0), LocalTime.of(17, 0), 10,
                java.util.Arrays.stream(times).map(t -> date.atTime(LocalTime.parse(t))).toList());
    }

    @Test
    void noMarksMeansAbsence() {
        assertEquals("FALTA", result().status());
    }

    @Test
    void toleranceIsInclusive() {
        assertEquals("PRESENTE", result("08:10", "17:00").status());
        assertEquals("TARDANZA", result("08:10:01", "17:00").status());
    }

    @Test
    void lateCountsFromShiftStart() {
        var r = result("08:25", "17:00");
        assertEquals("TARDANZA", r.status());
        assertEquals(25, r.lateMinutes());
    }

    @Test
    void oneMarkMeansIncomplete() {
        var r = result("08:00");
        assertEquals("INCOMPLETA", r.status());
        assertNull(r.lastOut());
        assertEquals(0, r.workedMinutes());
    }

    @Test
    void breakIsNotCountedAsWork() {
        assertEquals(480, result("08:00", "12:00", "13:00", "17:00").workedMinutes());
    }

    @Test
    void unorderedDuplicatesAndOtherDatesAreIgnored() {
        var r = new AttendancePolicy().calculate(date, LocalTime.of(8, 0), LocalTime.of(17, 0), 10,
                List.of(date.atTime(17, 0), date.atTime(8, 0), date.atTime(8, 0), date.plusDays(1).atTime(9, 0)));
        assertEquals("PRESENTE", r.status());
        assertEquals(540, r.workedMinutes());
    }

    @Test
    void oddNumberOfMarksIsIncomplete() {
        assertEquals("INCOMPLETA", result("08:00", "12:00", "13:00").status());
    }

    @Test
    void earlyExitCreatesIncident() {
        assertTrue(result("08:00", "15:00").incidents().contains("Salida anticipada"));
    }
}
