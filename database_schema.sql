-- ============================================
-- FundAgent 基金理财智能 Agent 系统数据库脚本
-- 生成时间: 2026-03-08
-- ============================================

-- 1. fund_info 表 - 基金基础信息表
CREATE TABLE IF NOT EXISTS fund_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码，唯一标识',
    fund_name VARCHAR(100) NOT NULL COMMENT '基金名称',
    fund_type VARCHAR(20) NOT NULL COMMENT '基金类型',
    risk_level INT DEFAULT 3 COMMENT '风险等级 1-5（1最低，5最高）',
    fund_company VARCHAR(50) COMMENT '基金公司',
    established_date DATE COMMENT '成立日期',
    manager VARCHAR(50) COMMENT '基金经理',
    fund_size DOUBLE COMMENT '基金规模（亿元）',
    management_fee DOUBLE COMMENT '管理费率（%）',
    custody_fee DOUBLE COMMENT '托管费率（%）',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用监控',
    remark VARCHAR(200) COMMENT '备注信息',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_fund_code (fund_code),
    KEY idx_fund_type (fund_type),
    KEY idx_risk_level (risk_level),
    KEY idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基金基础信息表';

-- ============================================

-- 2. fund_holding 表 - 持仓信息表
CREATE TABLE IF NOT EXISTS fund_holding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码，关联基金基础信息',
    cost_price DECIMAL(10,4) NOT NULL COMMENT '持仓成本价',
    holding_amount DECIMAL(16,2) NOT NULL COMMENT '持仓份额',
    holding_value DECIMAL(16,2) COMMENT '持仓市值（冗余字段，便于查询）',
    purchase_date DATE NOT NULL COMMENT '购买日期',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '持仓状态：ACTIVE-持有中，SOLD-已卖出',
    sell_date DATE COMMENT '卖出日期',
    sell_price DECIMAL(10,4) COMMENT '卖出价格',
    sell_profit DECIMAL(16,2) COMMENT '卖出收益',
    remark VARCHAR(200) COMMENT '备注信息',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    KEY idx_fund_code (fund_code),
    KEY idx_status (status),
    KEY idx_purchase_date (purchase_date),
    KEY idx_fund_code_status (fund_code, status),
    KEY idx_purchase_date_range (purchase_date, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='持仓信息表';

-- ============================================

-- 3. fund_daily_data 表 - 每日基金数据表
CREATE TABLE IF NOT EXISTS fund_daily_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    trade_date DATE NOT NULL COMMENT '交易日',
    net_value DECIMAL(10,4) COMMENT '单位净值',
    estimate_value DECIMAL(10,4) COMMENT '实时估值',
    change_rate DECIMAL(6,2) COMMENT '日涨跌幅（%）',
    turnover DECIMAL(16,2) COMMENT '成交额（万元）',
    turnover_rate DECIMAL(6,2) COMMENT '换手率（%）',
    pe_ratio DECIMAL(10,2) COMMENT '市盈率',
    pb_ratio DECIMAL(10,2) COMMENT '市净率',
    nav_date DATE COMMENT '净值日期',
    data_source VARCHAR(50) DEFAULT 'tianTianFund' COMMENT '数据来源',
    data_quality VARCHAR(10) DEFAULT 'MEDIUM' COMMENT '数据质量：HIGH-高质量，MEDIUM-中等，LOW-低质量',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_fund_trade_date (fund_code, trade_date),
    KEY idx_trade_date (trade_date),
    KEY idx_fund_code (fund_code),
    KEY idx_data_quality (data_quality),
    KEY idx_fund_trade_date_desc (fund_code, trade_date DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日基金数据表';

-- ============================================

-- 4. fund_strategy_log 表 - 策略日志表
CREATE TABLE IF NOT EXISTS fund_strategy_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    trade_date DATE NOT NULL COMMENT '交易日',
    yield_rate DECIMAL(6,2) NOT NULL COMMENT '当日收益率（%）',
    daily_change DECIMAL(6,2) COMMENT '日涨跌幅（%）',
    weekly_change DECIMAL(6,2) COMMENT '周涨跌幅（%）',
    monthly_change DECIMAL(6,2) COMMENT '月涨跌幅（%）',
    current_price DECIMAL(10,4) COMMENT '当前价格',
    cost_price DECIMAL(10,4) COMMENT '持仓成本',
    suggestion VARCHAR(20) NOT NULL COMMENT '操作建议',
    suggestion_reason VARCHAR(200) COMMENT '建议原因',
    triggered_rule VARCHAR(50) COMMENT '触发规则名称',
    rule_priority INT COMMENT '规则优先级',
    confidence DECIMAL(3,2) COMMENT '决策置信度（0-1）',
    is_notified BOOLEAN DEFAULT FALSE COMMENT '是否已通知',
    notify_time TIMESTAMP NULL COMMENT '通知时间',
    is_executed BOOLEAN DEFAULT FALSE COMMENT '是否已执行',
    execute_time TIMESTAMP NULL COMMENT '执行时间',
    execute_result VARCHAR(100) COMMENT '执行结果',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    KEY idx_fund_code (fund_code),
    KEY idx_trade_date (trade_date),
    KEY idx_suggestion (suggestion),
    KEY idx_is_notified (is_notified),
    KEY idx_is_executed (is_executed),
    KEY idx_fund_trade_date (fund_code, trade_date),
    KEY idx_create_time (create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='策略日志表';

-- ============================================

-- 5. message_log 表 - 消息推送记录表
CREATE TABLE IF NOT EXISTS message_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    message_id VARCHAR(50) UNIQUE COMMENT '消息业务ID（用于外部引用）',
    message_type VARCHAR(20) NOT NULL COMMENT '消息类型',
    message_title VARCHAR(100) NOT NULL COMMENT '消息标题',
    message_content LONGTEXT NOT NULL COMMENT '消息内容',
    recipient VARCHAR(100) COMMENT '接收者标识',
    channel VARCHAR(20) NOT NULL COMMENT '推送渠道',
    send_status INT NOT NULL DEFAULT 2 COMMENT '发送状态：0-失败，1-成功，2-发送中',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    max_retries INT DEFAULT 3 COMMENT '最大重试次数',
    error_message VARCHAR(500) COMMENT '错误信息',
    scheduled_time TIMESTAMP NULL COMMENT '计划发送时间',
    send_time TIMESTAMP NULL COMMENT '实际发送时间',
    expire_time TIMESTAMP NULL COMMENT '过期时间',
    priority INT DEFAULT 3 COMMENT '优先级：1-最高，2-高，3-中，4-低',
    is_urgent BOOLEAN DEFAULT FALSE COMMENT '是否紧急',
    related_id VARCHAR(50) COMMENT '相关数据ID（如基金代码、策略日志ID等）',
    related_type VARCHAR(50) COMMENT '相关类型',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_message_id (message_id),
    KEY idx_message_type (message_type),
    KEY idx_channel (channel),
    KEY idx_send_status (send_status),
    KEY idx_scheduled_time (scheduled_time),
    KEY idx_create_time (create_time DESC),
    KEY idx_related (related_type, related_id),
    KEY idx_expire_time (expire_time),
    KEY idx_is_urgent (is_urgent)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息推送记录表';

-- ============================================
-- 外键约束（如果需要）
-- ============================================

-- 注意：以下外键约束是可选的，根据实际需要启用

-- ALTER TABLE fund_holding
-- ADD CONSTRAINT fk_fund_holding_fund_info
-- FOREIGN KEY (fund_code) REFERENCES fund_info(fund_code)
-- ON DELETE CASCADE ON UPDATE CASCADE;

-- ALTER TABLE fund_daily_data
-- ADD CONSTRAINT fk_fund_daily_data_fund_info
-- FOREIGN KEY (fund_code) REFERENCES fund_info(fund_code)
-- ON DELETE CASCADE ON UPDATE CASCADE;

-- ALTER TABLE fund_strategy_log
-- ADD CONSTRAINT fk_fund_strategy_log_fund_info
-- FOREIGN KEY (fund_code) REFERENCES fund_info(fund_code)
-- ON DELETE CASCADE ON UPDATE CASCADE;

-- ============================================
-- 索引优化建议
-- ============================================

-- 1. 对于高频查询，可以考虑添加以下复合索引
-- CREATE INDEX idx_fund_info_search ON fund_info(fund_code, fund_name, is_active);
-- CREATE INDEX idx_fund_holding_analysis ON fund_holding(fund_code, status, purchase_date);
-- CREATE INDEX idx_fund_daily_data_analysis ON fund_daily_data(fund_code, trade_date DESC, net_value);

-- 2. 对于报表查询，可以添加以下索引
-- CREATE INDEX idx_fund_strategy_log_report ON fund_strategy_log(trade_date, suggestion, is_executed);
-- CREATE INDEX idx_message_log_report ON message_log(create_time, channel, send_status);

-- ============================================
-- 视图（可选）
-- ============================================

-- 基金持仓详情视图
CREATE OR REPLACE VIEW v_fund_holding_detail AS
SELECT
    h.id,
    h.fund_code,
    f.fund_name,
    f.fund_type,
    h.cost_price,
    h.holding_amount,
    h.holding_value,
    h.purchase_date,
    h.status,
    h.sell_date,
    h.sell_price,
    h.sell_profit,
    h.create_time,
    h.update_time,
    -- 计算持仓天数
    DATEDIFF(CURDATE(), h.purchase_date) AS hold_days,
    -- 计算持仓收益率（如果有当前价格）
    CASE
        WHEN h.holding_value IS NOT NULL AND h.holding_amount > 0 AND h.cost_price > 0
        THEN (h.holding_value / (h.cost_price * h.holding_amount) - 1) * 100
        ELSE NULL
    END AS yield_rate
FROM fund_holding h
LEFT JOIN fund_info f ON h.fund_code = f.fund_code
WHERE h.status = 'ACTIVE';

-- 基金最新数据视图
CREATE OR REPLACE VIEW v_fund_latest_data AS
SELECT
    d1.*
FROM fund_daily_data d1
INNER JOIN (
    SELECT fund_code, MAX(trade_date) AS latest_trade_date
    FROM fund_daily_data
    WHERE net_value IS NOT NULL
    GROUP BY fund_code
) d2 ON d1.fund_code = d2.fund_code AND d1.trade_date = d2.latest_trade_date;

-- ============================================
-- 存储过程示例（可选）
-- ============================================

-- 清理过期数据的存储过程
DELIMITER //
CREATE PROCEDURE sp_clean_old_data(IN days_ago INT)
BEGIN
    -- 清理指定天数前的策略日志
    DELETE FROM fund_strategy_log
    WHERE trade_date < DATE_SUB(CURDATE(), INTERVAL days_ago DAY);

    -- 清理指定天数前的消息日志（已发送且非紧急）
    DELETE FROM message_log
    WHERE send_status = 1
    AND is_urgent = FALSE
    AND create_time < DATE_SUB(CURDATE(), INTERVAL days_ago DAY);

    -- 清理指定天数前的每日数据（保留最近一年的数据）
    DELETE FROM fund_daily_data
    WHERE trade_date < DATE_SUB(CURDATE(), INTERVAL 365 DAY);

    SELECT ROW_COUNT() AS rows_deleted;
END //
DELIMITER ;

-- ============================================
-- 初始化数据示例（可选）
-- ============================================

-- 插入测试基金数据
-- INSERT INTO fund_info (fund_code, fund_name, fund_type, risk_level, fund_company) VALUES
-- ('000001', '华夏成长混合', 'STOCK', 3, '华夏基金'),
-- ('000002', '嘉实稳健混合', 'STOCK', 2, '嘉实基金'),
-- ('000003', '易方达蓝筹精选', 'STOCK', 4, '易方达基金');

-- ============================================
-- 使用说明
-- ============================================

-- 1. 执行整个脚本创建所有表：
--    mysql -u username -p database_name < database_schema.sql

-- 2. 只执行表创建（跳过外键、视图、存储过程）：
--    执行到第5个表创建结束即可

-- 3. 生产环境建议：
--    a. 根据数据量调整存储引擎参数
--    b. 定期执行存储过程清理历史数据
--    c. 监控表空间使用情况
--    d. 定期优化表和索引

-- 4. 开发环境建议：
--    a. 可以使用默认配置
--    b. 可以插入测试数据
--    c. 可以启用外键约束进行数据完整性测试