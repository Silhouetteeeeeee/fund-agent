package com.shxc.fundagent.service;

import com.shxc.fundagent.entity.FundDailyData;
import com.shxc.fundagent.entity.FundInfo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 基金数据服务接口
 * 提供基金数据的获取、存储、查询等功能
 */
public interface FundDataService {

    /**
     * 获取基金实时数据
     *
     * @param fundCode 基金代码
     * @return 实时数据，如果获取失败返回null
     */
    FundDailyData getRealTimeData(String fundCode);

    /**
     * 批量获取基金实时数据
     *
     * @param fundCodes 基金代码列表
     * @return 实时数据列表
     */
    List<FundDailyData> batchGetRealTimeData(List<String> fundCodes);

    List<FundDailyData> getHistoryData(String fundCode, LocalDate sDate, LocalDate eDate);

    /**
     * 获取基金历史数据
     *
     * @param fundCode 基金代码
     * @param days     获取最近多少天的数据
     * @return 历史数据列表
     */
    List<FundDailyData> getHistoryData(String fundCode, int days);

    /**
     * 获取基金基本信息
     *
     * @param fundCode 基金代码
     * @return 基金基本信息
     */
    FundInfo getFundBasicInfo(String fundCode);

    /**
     * 更新基金基础信息
     *
     * @param fundCode 基金代码
     * @return 更新后的基金信息
     */
    FundInfo updateFundBasicInfo(String fundCode);

    /**
     * 同步基金数据到数据库
     *
     * @param fundCode 基金代码
     * @param date     日期
     * @return 同步结果
     */
    boolean syncFundData(String fundCode, LocalDate date);

    /**
     * 批量同步基金数据
     *
     * @param fundCodes 基金代码列表
     * @param date      日期
     * @return 成功同步的数量
     */
    int batchSyncFundData(List<String> fundCodes, LocalDate date);

    /**
     * 获取最新交易日
     *
     * @return 最新交易日
     */
    LocalDate getLatestTradeDate();

    /**
     * 获取基金当前价格
     *
     * @param fundCode 基金代码
     * @return 当前价格（优先使用实时估值，其次使用最新净值）
     */
    BigDecimal getCurrentPrice(String fundCode);

    BigDecimal getAbsoluteCurrentPrice(String fundCode);

    /**
     * 获取基金日涨跌幅
     *
     * @param fundCode 基金代码
     * @return 日涨跌幅（百分比）
     */
    BigDecimal getDailyChangeRate(String fundCode);

    /**
     * 获取基金周涨跌幅
     *
     * @param fundCode 基金代码
     * @return 周涨跌幅（百分比）
     */
    BigDecimal getWeeklyChangeRate(String fundCode);

    /**
     * 获取基金月涨跌幅
     *
     * @param fundCode 基金代码
     * @return 月涨跌幅（百分比）
     */
    BigDecimal getMonthlyChangeRate(String fundCode);

    /**
     * 检查基金数据是否最新
     *
     * @param fundCode 基金代码
     * @return true如果数据是最新的，false如果不是
     */
    boolean isDataUpToDate(String fundCode);

    /**
     * 获取数据质量报告
     *
     * @param fundCode 基金代码
     * @return 数据质量报告
     */
    Map<String, Object> getDataQualityReport(String fundCode);

    /**
     * 清理过期数据
     *
     * @param beforeDate 清理此日期之前的数据
     * @return 清理的记录数
     */
    int cleanUpExpiredData(LocalDate beforeDate);

    /**
     * 获取数据源健康状态
     *
     * @return 各数据源健康状态
     */
    Map<String, String> getDataSourceHealthStatus();

    /**
     * 手动触发数据采集
     *
     * @param fundCode 基金代码
     * @param dataType 数据类型：REAL_TIME, HISTORY, BASIC_INFO
     * @return 采集结果
     */
    boolean triggerDataCollection(String fundCode, String dataType);

    /**
     * 验证基金代码
     *
     * @param fundCode 基金代码
     * @return true如果基金代码有效，false如果无效
     */
    boolean validateFundCode(String fundCode);

    /**
     * 搜索基金
     *
     * @param keyword 关键词（基金代码或名称）
     * @return 匹配的基金列表
     */
    List<FundInfo> searchFunds(String keyword);

    /**
     * 获取基金数据统计
     *
     * @return 统计信息
     */
    Map<String, Object> getDataStatistics();

    /**
     * 修复数据问题
     *
     * @param fundCode 基金代码
     * @param date     日期
     * @return true如果修复成功，false如果失败
     */
    boolean fixDataIssue(String fundCode, LocalDate date);
}