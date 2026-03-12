package com.shxc.fundagent.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "holiday_calendar", uniqueConstraints = {
        @UniqueConstraint(name = "uk_holiday_calendar_date", columnNames = {"holiday_date"})
})
public class HolidayCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

}
