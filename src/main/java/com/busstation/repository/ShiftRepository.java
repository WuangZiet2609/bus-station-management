package com.busstation.repository;

import com.busstation.entity.Shift;
import com.busstation.entity.ShiftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID> {

    List<Shift> findByEmployeeId(UUID employeeId);

    Page<Shift> findByEmployeeId(UUID employeeId, Pageable pageable);

    List<Shift> findByTripId(UUID tripId);

    List<Shift> findByEmployeeIdAndShiftDateBetween(UUID employeeId, LocalDate start, LocalDate end);

    List<Shift> findByShiftDateAndStatus(LocalDate date, ShiftStatus status);

    @Query("SELECT s FROM Shift s WHERE s.employee.id = :employeeId AND s.shiftDate = :date AND s.status IN :statuses")
    List<Shift> findByEmployeeAndDateAndStatuses(@Param("employeeId") UUID employeeId, @Param("date") LocalDate date, @Param("statuses") List<ShiftStatus> statuses);

    @Query("SELECT s FROM Shift s WHERE s.shiftDate BETWEEN :start AND :end AND s.status = 'SCHEDULED'")
    List<Shift> findUpcomingShifts(@Param("start") LocalDate start, @Param("end") LocalDate end);
}