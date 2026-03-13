package com.shxc.fundagent.service.impl;

import com.shxc.fundagent.repository.HolidayCalendarRepository;
import com.shxc.fundagent.service.HolidayCalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class HolidayCalendarServiceImpl implements HolidayCalendarService {

    @Autowired
    private HolidayCalendarRepository holidayCalendarRepository;

    @Override
    public boolean isTradeDay(LocalDate day) {
        // 非节假日 非周六周日
        return day.getDayOfWeek() != DayOfWeek.SATURDAY
                && day.getDayOfWeek() != DayOfWeek.SUNDAY
                && !holidayCalendarRepository.existsByDate(day);
    }

}
