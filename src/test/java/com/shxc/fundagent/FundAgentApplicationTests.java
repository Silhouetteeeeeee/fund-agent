package com.shxc.fundagent;

import com.shxc.fundagent.repository.FundTransactionRecordRepository;
import com.shxc.fundagent.scheduling.FundTaskScheduler;
import com.shxc.fundagent.service.FundDataService;
import com.shxc.fundagent.service.FundDataSource;
import com.shxc.fundagent.service.YieldCalculationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

@SpringBootTest
class FundAgentApplicationTests {

    @Autowired
    FundDataService fundDataService;

    @Autowired
    YieldCalculationService yieldCalculationService;

    @Autowired
    FundDataSource fundDataSource;

    @Autowired
    FundTransactionRecordRepository fundTransactionRecordRepository;

    @Autowired
    FundTaskScheduler fundTaskScheduler;

    @Test
    void contextLoads() {

//        fundDataService.getHistoryData("011095", 1);
//        yieldCalculationService.calculateFundYield("011095", null);
//        fundDataSource.fetchHistoryData("011095", 7);
        System.out.println(fundDataSource.fetchHistoryData("011095", LocalDate.now().minusDays(2), LocalDate.now().minusDays(1)));
//        System.out.println(fundTransactionRecordRepository.findActiveTransactionRecord("013566"));
//        fundTaskScheduler.recalculateHoldingCostPrice();

    }

}
