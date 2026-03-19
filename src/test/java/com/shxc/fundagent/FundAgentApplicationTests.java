package com.shxc.fundagent;

import com.shxc.fundagent.agent.impl.FundAnalysisAgent;
import com.shxc.fundagent.agent.model.AgentResult;
import com.shxc.fundagent.entity.FundDailyData;
import com.shxc.fundagent.entity.FundHolding;
import com.shxc.fundagent.entity.FundInfo;
import com.shxc.fundagent.repository.FundDailyDataRepository;
import com.shxc.fundagent.repository.FundHoldingRepository;
import com.shxc.fundagent.repository.FundInfoRepository;
import com.shxc.fundagent.repository.FundTransactionRecordRepository;
import com.shxc.fundagent.scheduling.FundTaskScheduler;
import com.shxc.fundagent.service.*;
import dev.ai4j.openai4j.Json;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
class FundAgentApplicationTests {

    @Autowired
    FundDataService fundDataService;

    @Autowired
    YieldCalculationService yieldCalculationService;

    @Autowired
    FundInfoRepository fundInfoRepository;

    @Autowired
    FundDataSource fundDataSource;

    @Autowired
    FundTransactionRecordRepository fundTransactionRecordRepository;

    @Autowired
    TransactionService transactionService;

    @Autowired
    FundTaskScheduler fundTaskScheduler;

    @Autowired
    FundAnalysisAgent fundAnalysisAgent;

    @Autowired
    FundHoldingRepository fundHoldingRepository;

    @Autowired
    FundDailyDataRepository fundDailyDataRepository;

    @Autowired
    RegularInvestmentPlanService regularInvestmentPlanService;

    @Test
    void contextLoads() {

//        fundDataService.getHistoryData("011095", 1);
//        yieldCalculationService.calculateFundYield("011095", null);
//        fundDataSource.fetchHistoryData("011095", 7);
//        System.out.println(fundDataSource.fetchHistoryData("011095", LocalDate.now().minusDays(2), LocalDate.now().minusDays(1)));
//        System.out.println(fundTransactionRecordRepository.findActiveTransactionRecord("013566"));
//        fundTaskScheduler.recalculateHoldingCostPrice();

//        transactionService.createBuyTransaction("013566", BigDecimal.valueOf(50)
//                , LocalDateTime.of(2025, 11, 26, 0, 12), new BigDecimal("0"));
//        transactionService.createSellTransaction("013566", new BigDecimal("44.07")
//                , LocalDateTime.of(2025, 12, 30, 22, 13), new BigDecimal("0"));

//        fundTaskScheduler.recalculateHoldingCostPrice();

//        List<FundHolding> fundHoldings = fundHoldingRepository.findAllActiveHoldings();
//        BigDecimal totalHoldingValue = fundHoldings.stream().map(FundHolding::getHoldingValue).reduce(BigDecimal.ZERO, BigDecimal::add);
//        List<Map<String, Object>> funds = new ArrayList<>();
//        BigDecimal totalCost = BigDecimal.ZERO;
//        for (FundHolding fundHolding : fundHoldings) {
//            String fundCode = fundHolding.getFundCode();
//            String fundName = "";
//            int riskLevel = 0;
//            FundInfo info = fundInfoRepository.findByFundCode(fundCode).orElse(null);
//            if (info != null) {
//                fundName = info.getFundName();
//                riskLevel = info.getRiskLevel();
//            }
//            FundDailyData dailyData = fundDailyDataRepository.findLatestByFundCode(fundCode).orElse(null);
//            BigDecimal netValue = null;
//            BigDecimal changPercent = null;
//            if (dailyData != null) {
//                netValue = dailyData.getEffectivePrice();
//                changPercent = dailyData.getChangeRate();
//            } else {
//                continue;
//            }
//            totalCost = totalCost.add(fundHolding.getTotalCost());
//            Map<String, Object> fundInfo = Map.ofEntries(
//                    Map.entry("fundCode", fundCode),
//                    Map.entry("fundName", fundName),
//                    Map.entry("netValue", netValue),
//                    Map.entry("changePercent", changPercent),
//                    Map.entry("riskLevel", riskLevel),
//                    // 持仓信息
//                    Map.entry("holdShares", fundHolding.getHoldShare()),
//                    Map.entry("holdAmount", fundHolding.getHoldingValue()),
//                    Map.entry("avgCost", fundHolding.getCostPrice()),
//                    Map.entry("costAmount", fundHolding.getTotalCost()),
//                    Map.entry("profit", fundHolding.getHoldProfit(netValue)),
//                    Map.entry("profitRate", fundHolding.getHoldProfitRate(netValue)),
//                    Map.entry("position", fundHolding.getHoldingValue().divide(totalHoldingValue, 2, RoundingMode.HALF_UP)),
//                    Map.entry("holdDays", fundHolding.getHoldDays())
//            );
//            funds.add(fundInfo);
//        }
//        Map<String, Object> context = new HashMap<>();
//        context.put("funds", funds);
//        context.put("totalAssets", totalHoldingValue);
//        context.put("totalCost", totalCost);
//        context.put("totalProfit", totalHoldingValue.subtract(totalCost));
//        context.put("totalProfitRate", totalHoldingValue.subtract(totalCost).divide(totalCost, 2, RoundingMode.HALF_UP));
//        context.put("availableCash", 100000);
//        context.put("targetPosition", new BigDecimal("0.5"));
//        AgentResult result = fundAnalysisAgent.process("fund_analysis", context);
//        System.out.println(Json.toJson(result));
//        fundAnalysisAgent.process("fund_analysis", null);

        fundTaskScheduler.syncWeekendData();
    }

    @Test
    public void initFundTransaction() {
//        transactionService.createBuyTransaction("011095", BigDecimal.valueOf(10000)
//                , LocalDateTime.parse("2025-09-13T16:57:50"), new BigDecimal("0.1"));
//        transactionService.createBuyTransaction("011095", BigDecimal.valueOf(100)
//            , LocalDateTime.parse("2025-09-24T20:06:09"), new BigDecimal("0.1"));
//        transactionService.createBuyTransaction("011095", BigDecimal.valueOf(100)
//            , LocalDateTime.parse("2025-09-26T21:11:56"), new BigDecimal("0.1"));
//        transactionService.createBuyTransaction("011095", BigDecimal.valueOf(300)
//            , LocalDateTime.parse("2025-11-29T14:17:15"), new BigDecimal("0.1"));
//
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-03-13T10:56:48"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-03-12T11:01:44"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-03-11T10:59:53"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-03-10T11:03:20"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-03-09T11:14:31"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-03-06T11:04:47"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-03-03T10:57:15"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-03-02T11:10:24"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-02-27T10:57:00"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-02-26T11:03:07"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-02-05T21:16:03"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(50), LocalDateTime.parse("2026-02-05T09:52:25"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-02-02T10:55:50"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-01-30T10:42:43"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-01-29T10:49:14"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-01-28T10:48:34"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-01-27T10:48:31"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017731", BigDecimal.valueOf(100), LocalDateTime.parse("2026-01-26T11:01:28"), BigDecimal.ZERO);
//
//        transactionService.createBuyTransaction("014143", BigDecimal.valueOf(50), LocalDateTime.parse("2026-03-09T09:34:39"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("014143", BigDecimal.valueOf(500), LocalDateTime.parse("2026-02-25T21:10:03"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("014143", BigDecimal.valueOf(400), LocalDateTime.parse("2026-02-12T10:02:04"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("014143", BigDecimal.valueOf(100), LocalDateTime.parse("2026-02-02T21:47:25"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("014143", BigDecimal.valueOf(100), LocalDateTime.parse("2026-01-29T21:44:32"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("014143", BigDecimal.valueOf(200), LocalDateTime.parse("2026-01-27T12:55:26"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("014143", BigDecimal.valueOf(100), LocalDateTime.parse("2026-01-21T22:32:13"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("014143", BigDecimal.valueOf(50), LocalDateTime.parse("2026-01-20T12:54:47"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("014143", BigDecimal.valueOf(50), LocalDateTime.parse("2026-01-16T00:18:10"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("014143", BigDecimal.valueOf(100), LocalDateTime.parse("2026-01-08T00:29:43"), BigDecimal.ZERO);
//
//
//        transactionService.createBuyTransaction("000968", BigDecimal.valueOf(500), LocalDateTime.parse("2026-02-13T21:08:36"), new BigDecimal("0.12"));
//        transactionService.createBuyTransaction("000968", BigDecimal.valueOf(100), LocalDateTime.parse("2026-02-05T21:21:53"), new BigDecimal("0.12"));
//        transactionService.createBuyTransaction("000968", BigDecimal.valueOf(100), LocalDateTime.parse("2026-01-27T21:17:43"), new BigDecimal("0.12"));
//        transactionService.createBuyTransaction("000968", BigDecimal.valueOf(40), LocalDateTime.parse("2026-01-17T00:35:19"), new BigDecimal("0.12"));
//        transactionService.createBuyTransaction("000968", BigDecimal.valueOf(100), LocalDateTime.parse("2026-01-16T00:16:47"), new BigDecimal("0.12"));
//        transactionService.createBuyTransaction("000968", BigDecimal.valueOf(70), LocalDateTime.parse("2026-01-10T12:39:40"), new BigDecimal("0.12"));
//        transactionService.createBuyTransaction("000968", BigDecimal.valueOf(600), LocalDateTime.parse("2026-01-08T00:32:28"), new BigDecimal("0.12"));
//        transactionService.createSellTransaction("000968", BigDecimal.valueOf(500), LocalDateTime.parse("2026-02-27T13:42:01"), new BigDecimal("0.5"));
//
//        transactionService.createBuyTransaction("012734", BigDecimal.valueOf(1000), LocalDateTime.parse("2026-02-27T13:49:35"), BigDecimal.ZERO);
//
//        transactionService.createBuyTransaction("013566", BigDecimal.valueOf(1000), LocalDateTime.parse("2026-03-01T23:19:54"), BigDecimal.ZERO);
//
//        transactionService.createBuyTransaction("017437", BigDecimal.valueOf(100), LocalDateTime.parse("2026-02-27T13:37:51"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017437", BigDecimal.valueOf(500), LocalDateTime.parse("2026-01-24T13:54:56"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017437", BigDecimal.valueOf(50), LocalDateTime.parse("2025-12-17T19:47:24"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017437", BigDecimal.valueOf(50), LocalDateTime.parse("2025-10-13T22:35:37"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("017437", BigDecimal.valueOf(50), LocalDateTime.parse("2025-09-25T23:35:13"), BigDecimal.ZERO);
//
//        transactionService.createBuyTransaction("011840", BigDecimal.valueOf(1000), LocalDateTime.parse("2026-02-12T18:51:54"), BigDecimal.ZERO);
//        transactionService.createSellTransaction("011840", new BigDecimal("207.56"), LocalDateTime.parse("2026-03-02T22:14:53"), BigDecimal.ZERO);
//
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-03-09T10:03:20"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-03-06T09:51:02"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-03-05T09:56:03"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-03-04T09:53:08"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-03-03T09:54:49"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-03-02T10:06:42"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-02-27T09:51:29"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-02-26T09:55:49"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-02-25T09:52:49"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-02-24T09:42:49"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-02-13T09:52:52"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-02-12T09:57:34"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-02-11T09:51:55"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-02-10T09:53:00"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-02-09T10:03:31"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-02-06T09:51:52"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-02-05T09:56:09"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-02-04T09:53:28"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-02-03T09:54:37"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-02-02T10:07:54"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-01-30T09:52:00"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-01-29T09:56:35"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-01-28T09:54:22"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-01-27T09:53:05"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-01-26T10:02:25"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-01-23T09:52:53"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-01-22T09:57:26"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-01-21T09:54:54"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-01-20T09:57:19"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-01-16T10:30:08"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-01-15T10:35:18"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2026-01-14T10:35:08"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2025-12-31T09:55:37"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2025-12-30T09:58:24"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2025-12-29T10:08:47"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2025-12-26T09:57:40"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2025-12-24T10:15:15"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2025-12-23T10:18:27"), BigDecimal.ZERO);
//        transactionService.createBuyTransaction("006479", BigDecimal.valueOf(10), LocalDateTime.parse("2025-12-21T13:59:52"), BigDecimal.ZERO);
//        fundTaskScheduler.fallback();
//        fundTaskScheduler.calculateHoldingYields();
        fundTaskScheduler.syncWeekendData();
    }

    @Test
    public void testInvestPlan() {
//        regularInvestmentPlanService
//                .createPlan("017731", new BigDecimal(100),
//                        "DAILY", null, LocalDate.of(2026, 1, 26), null,
//                        BigDecimal.ZERO, "美股定投");
        fundTaskScheduler.executeRegularInvestmentPlans();
    }

}
