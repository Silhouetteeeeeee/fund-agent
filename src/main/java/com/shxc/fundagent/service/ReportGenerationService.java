package com.shxc.fundagent.service;

import java.util.Map;

/**
 * 报告生成服务接口
 * 负责生成各种理财报告
 */
public interface ReportGenerationService {

    /**
     * 生成日报
     *
     * @param reportDate 报告日期（格式：yyyy-MM-dd）
     * @return 报告生成结果
     */
    ReportResult generateDailyReport(String reportDate);

    /**
     * 生成周报
     *
     * @param reportDate 报告日期（格式：yyyy-MM-dd）
     * @return 报告生成结果
     */
    ReportResult generateWeeklyReport(String reportDate);

    /**
     * 生成月报
     *
     * @param reportDate 报告日期（格式：yyyy-MM）
     * @return 报告生成结果
     */
    ReportResult generateMonthlyReport(String reportDate);

    /**
     * 生成投资组合报告
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 报告生成结果
     */
    ReportResult generatePortfolioReport(String startDate, String endDate);

    /**
     * 生成风险分析报告
     *
     * @param fundCodes 基金代码列表
     * @return 报告生成结果
     */
    ReportResult generateRiskAnalysisReport(String[] fundCodes);

    /**
     * 生成收益分析报告
     *
     * @param fundCode  基金代码
     * @param period    分析周期（days, weeks, months）
     * @return 报告生成结果
     */
    ReportResult generateYieldAnalysisReport(String fundCode, String period);

    /**
     * 生成报告模板
     *
     * @param templateId 模板ID
     * @param parameters 模板参数
     * @return 报告生成结果
     */
    ReportResult generateReportByTemplate(String templateId, Map<String, Object> parameters);

    /**
     * 导出报告到文件
     *
     * @param reportId   报告ID
     * @param format     格式（PDF, EXCEL, HTML）
     * @return 文件路径
     */
    String exportReportToFile(String reportId, String format);

    /**
     * 发送报告到指定渠道
     *
     * @param reportId     报告ID
     * @param channels     推送渠道
     * @param recipients   接收者
     * @return 发送结果
     */
    boolean sendReport(String reportId, String[] channels, String[] recipients);

    /**
     * 报告生成结果类
     */
    class ReportResult {
        private String reportId;
        private boolean success;
        private String message;
        private Map<String, Object> reportData;
        private String filePath;
        private long generationTimeMs;

        public ReportResult() {
        }

        public ReportResult(String reportId, boolean success, String message, Map<String, Object> reportData) {
            this.reportId = reportId;
            this.success = success;
            this.message = message;
            this.reportData = reportData;
            this.generationTimeMs = System.currentTimeMillis();
        }

        // Getter和Setter
        public String getReportId() { return reportId; }
        public void setReportId(String reportId) { this.reportId = reportId; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Map<String, Object> getReportData() { return reportData; }
        public void setReportData(Map<String, Object> reportData) { this.reportData = reportData; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }

        public long getGenerationTimeMs() { return generationTimeMs; }
        public void setGenerationTimeMs(long generationTimeMs) { this.generationTimeMs = generationTimeMs; }
    }
}