package com.shxc.fundagent.utils;

import com.shxc.fundagent.enums.InvestmentFrequency;
import com.shxc.fundagent.service.HolidayCalendarService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class TradeDayUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        TradeDayUtils.applicationContext = applicationContext;
    }

    public static LocalDate findNextTradeDay(LocalDate day) {
        HolidayCalendarService holidayCalendarService = applicationContext.getBean(HolidayCalendarService.class);
        while(!holidayCalendarService.isTradeDay(day)) {
            day = day.plusDays(1);
        }
        return day;
    }
    public static LocalDate findNextTradeDay(LocalDate day, InvestmentFrequency frequency) {
        HolidayCalendarService holidayCalendarService = applicationContext.getBean(HolidayCalendarService.class);
        while(!holidayCalendarService.isTradeDay(day)) {
            day = getNextDay(day, frequency);
        }
        return day;
    }

    public static LocalDate getNextDay(LocalDate day, InvestmentFrequency frequency) {
        return switch (frequency) {
            case DAILY -> day.plusDays(1);
            case WEEKLY -> day.plusWeeks(1);
            case MONTHLY -> day.plusMonths(1);
            case QUARTERLY -> day.plusMonths(3);
            case YEARLY -> day.plusYears(1);
        };
    }

    public static boolean isTradeDay(LocalDate day) {
        HolidayCalendarService holidayCalendarService = applicationContext.getBean(HolidayCalendarService.class);
        return holidayCalendarService.isTradeDay(day);
    }


}
