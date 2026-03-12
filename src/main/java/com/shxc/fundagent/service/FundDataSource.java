package com.shxc.fundagent.service;

import com.shxc.fundagent.dto.external.FundBasicInfoDTO;
import com.shxc.fundagent.dto.external.FundHistoryDataDTO;
import com.shxc.fundagent.dto.external.FundRealTimeDataDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 基金数据源接口
 * 定义从不同数据源获取基金数据的标准接口
 */
public interface FundDataSource {

    /**
     * 获取数据源名称
     */
    String getSourceName();

    /**
     * 获取基金实时数据
     *
     * @param fundCode 基金代码
     * @return 实时数据，如果获取失败返回null
     */
    FundRealTimeDataDTO fetchRealTimeData(String fundCode);

    /**
     * 批量获取基金实时数据
     *
     * @param fundCodes 基金代码列表
     * @return 实时数据列表，失败的数据项为null
     */
    List<FundRealTimeDataDTO> batchFetchRealTimeData(List<String> fundCodes);

    /**
     * 获取基金历史数据
     *
     * @param fundCode 基金代码
     * @param days     获取最近多少天的数据，0表示获取所有数据
     * @return 历史数据，如果获取失败返回null
     */
    FundHistoryDataDTO fetchHistoryData(String fundCode, int days);

    FundHistoryDataDTO fetchHistoryData(String fundCode, LocalDate startDate, LocalDate endDate);

    /**
     * 获取基金基本信息
     *
     * @param fundCode 基金代码
     * @return 基本信息，如果获取失败返回null
     */
    FundBasicInfoDTO fetchFundBasicInfo(String fundCode);

    /**
     * 批量获取基金基本信息
     *
     * @param fundCodes 基金代码列表
     * @return 基本信息列表，失败的数据项为null
     */
    List<FundBasicInfoDTO> batchFetchFundBasicInfo(List<String> fundCodes);

    /**
     * 检查数据源是否可用
     *
     * @return true如果数据源可用，false如果不可用
     */
    boolean isAvailable();

    /**
     * 获取数据源的健康状态
     *
     * @return 健康状态描述
     */
    String getHealthStatus();

    /**
     * 获取最后错误信息
     *
     * @return 最后错误信息，如果没有错误返回null
     */
    String getLastError();

    /**
     * 获取支持的基金类型列表
     *
     * @return 支持的基金类型列表
     */
    List<String> getSupportedFundTypes();

    /**
     * 获取数据源调用频率限制（次/分钟）
     *
     * @return 调用频率限制
     */
    int getRateLimitPerMinute();

    /**
     * 验证基金代码是否有效
     *
     * @param fundCode 基金代码
     * @return true如果基金代码有效，false如果无效
     */
    boolean validateFundCode(String fundCode);

    /**
     * 获取数据源优先级（数值越小优先级越高）
     *
     * @return 优先级
     */
    int getPriority();

    /**
     * 获取数据更新时间
     *
     * @param fundCode 基金代码
     * @return 数据更新时间，如果未知返回null
     */
    LocalDate getLastUpdateTime(String fundCode);
}