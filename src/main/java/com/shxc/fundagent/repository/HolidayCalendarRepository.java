package com.shxc.fundagent.repository;


import com.shxc.fundagent.entity.HolidayCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayCalendarRepository extends JpaRepository<HolidayCalendar, Integer> {

    Optional<HolidayCalendar> findByHolidayDate(LocalDate holidayDate);

    @Query("SELECT h FROM HolidayCalendar h WHERE YEAR(h.holidayDate) = :year")
    List<HolidayCalendar> findByYear(int year);

    @Query("SELECT COUNT(h) > 0 FROM HolidayCalendar h WHERE h.holidayDate = :date")
    boolean existsByDate(LocalDate date);
}
