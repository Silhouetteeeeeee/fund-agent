-- ============================================
-- FundAgent 基金理财智能 Agent 系统 - 基础表结构
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
    UNIQUE KEY uk_fund_code (fund_code)
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
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
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
    UNIQUE KEY uk_fund_trade_date (fund_code, trade_date)
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
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
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
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息推送记录表';

-- ============================================
-- 基本索引（提高查询性能）
-- ============================================

-- fund_info 表索引
CREATE INDEX idx_fund_info_fund_type ON fund_info(fund_type);
CREATE INDEX idx_fund_info_is_active ON fund_info(is_active);

-- fund_holding 表索引
CREATE INDEX idx_fund_holding_fund_code ON fund_holding(fund_code);
CREATE INDEX idx_fund_holding_status ON fund_holding(status);
CREATE INDEX idx_fund_holding_purchase_date ON fund_holding(purchase_date);

-- fund_daily_data 表索引
CREATE INDEX idx_fund_daily_data_trade_date ON fund_daily_data(trade_date);
CREATE INDEX idx_fund_daily_data_fund_code ON fund_daily_data(fund_code);

-- fund_strategy_log 表索引
CREATE INDEX idx_fund_strategy_log_fund_code ON fund_strategy_log(fund_code);
CREATE INDEX idx_fund_strategy_log_trade_date ON fund_strategy_log(trade_date);
CREATE INDEX idx_fund_strategy_log_suggestion ON fund_strategy_log(suggestion);

-- message_log 表索引
CREATE INDEX idx_message_log_message_type ON message_log(message_type);
CREATE INDEX idx_message_log_channel ON message_log(channel);
CREATE INDEX idx_message_log_send_status ON message_log(send_status);
CREATE INDEX idx_message_log_create_time ON message_log(create_time);