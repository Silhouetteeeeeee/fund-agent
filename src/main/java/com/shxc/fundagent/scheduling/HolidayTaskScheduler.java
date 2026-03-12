package com.shxc.fundagent.scheduling;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shxc.fundagent.entity.HolidayCalendar;
import com.shxc.fundagent.repository.HolidayCalendarRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class HolidayTaskScheduler {

    private static final String baseUrl = "http://tool.bitefu.net/jiari/?d=";

    private final HolidayCalendarRepository holidayCalendarRepository;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        syncYear(LocalDate.now().getYear());
        syncYear(LocalDate.now().getYear() - 1);
        syncYear(LocalDate.now().getYear() - 2);
    }

    // 定时任务注解，每年1月1日0点10分执行，同步当年节假日数据
    @Scheduled(cron = "0 10 0 1 1 ?")
    public void syncHolidayData() {
        int currentYear = LocalDate.now().getYear();
        syncYear(currentYear);
    }

    private void syncYear(int currentYear) {
        if (holidayCalendarRepository.existsByDate(LocalDate.of(currentYear, 1, 1))) {
            log.info("{} 年节假日数据已存在，跳过同步...", currentYear);
            return;
        }
        log.info("开始同步 {} 年节假日数据...", currentYear);
        try {
            String url = baseUrl + currentYear;
            URI uri = UriComponentsBuilder.fromHttpUrl(url).build().toUri();
            Request request = new Request.Builder()
                    .url(uri.toString())
                    .build();
            List<HolidayCalendar> holidays = new ArrayList<>();
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    Map<Integer, Map<String, String>> rawData = objectMapper.readValue(jsonData,
                            new TypeReference<>() {}
                    );
                    if (rawData.containsKey(currentYear)) {
                        rawData.get(currentYear).forEach((date, type) -> {
                            HolidayCalendar holidayCalendar = new HolidayCalendar();
                            holidayCalendar.setHolidayDate(LocalDate.parse(currentYear + date
                                    , DateTimeFormatter.ofPattern("yyyyMMdd")));
                            holidays.add(holidayCalendar);
                        });
                    }
                }
            } catch (IOException e) {
                log.warn("同步 {} 年节假日数据失败: {}", currentYear, e.getMessage());
            }
            if (!holidays.isEmpty()) {
                log.info("开始保存 {} 年节假日数据...", currentYear);
                List<HolidayCalendar> res = holidayCalendarRepository.saveAll(holidays);
                log.info("保存 {} 年节假日数据成功，共 {} 条数据", currentYear, res.size());
            }
        } catch (Exception e) {
            log.warn("同步 {} 年节假日数据失败: {}", currentYear, e.getMessage());
        }
    }


}
