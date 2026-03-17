package com.shxc.fundagent.service;

import java.time.LocalDate;

public interface HolidayCalendarService {

    boolean isTradeDay(LocalDate day);

    LocalDate findLatestTradeDay(LocalDate day);


}
