-- FundAgent 基金理财智能 Agent 系统 - 最小化表结构
-- 生成时间: 2026-03-08

-- 1. fund_info - 基金基础信息
CREATE TABLE fund_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL,
    fund_name VARCHAR(100) NOT NULL,
    fund_type VARCHAR(20) NOT NULL,
    risk_level INT DEFAULT 3,
    fund_company VARCHAR(50),
    established_date DATE,
    manager VARCHAR(50),
    fund_size DOUBLE,
    management_fee DOUBLE,
    custody_fee DOUBLE,
    is_active BOOLEAN DEFAULT TRUE,
    remark VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (fund_code)
);

-- 2. fund_holding - 持仓信息
CREATE TABLE fund_holding (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL,
    cost_price DECIMAL(10,4) NOT NULL,
    holding_amount DECIMAL(16,2) NOT NULL,
    holding_value DECIMAL(16,2),
    purchase_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    sell_date DATE,
    sell_price DECIMAL(10,4),
    sell_profit DECIMAL(16,2),
    remark VARCHAR(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3. fund_daily_data - 每日基金数据
CREATE TABLE fund_daily_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL,
    trade_date DATE NOT NULL,
    net_value DECIMAL(10,4),
    estimate_value DECIMAL(10,4),
    change_rate DECIMAL(6,2),
    turnover DECIMAL(16,2),
    turnover_rate DECIMAL(6,2),
    pe_ratio DECIMAL(10,2),
    pb_ratio DECIMAL(10,2),
    nav_date DATE,
    data_source VARCHAR(50) DEFAULT 'tianTianFund',
    data_quality VARCHAR(10) DEFAULT 'MEDIUM',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (fund_code, trade_date)
);

-- 4. fund_strategy_log - 策略日志
CREATE TABLE fund_strategy_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fund_code VARCHAR(10) NOT NULL,
    trade_date DATE NOT NULL,
    yield_rate DECIMAL(6,2) NOT NULL,
    daily_change DECIMAL(6,2),
    weekly_change DECIMAL(6,2),
    monthly_change DECIMAL(6,2),
    current_price DECIMAL(10,4),
    cost_price DECIMAL(10,4),
    suggestion VARCHAR(20) NOT NULL,
    suggestion_reason VARCHAR(200),
    triggered_rule VARCHAR(50),
    rule_priority INT,
    confidence DECIMAL(3,2),
    is_notified BOOLEAN DEFAULT FALSE,
    notify_time TIMESTAMP NULL,
    is_executed BOOLEAN DEFAULT FALSE,
    execute_time TIMESTAMP NULL,
    execute_result VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. message_log - 消息推送记录
CREATE TABLE message_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id VARCHAR(50) UNIQUE,
    message_type VARCHAR(20) NOT NULL,
    message_title VARCHAR(100) NOT NULL,
    message_content LONGTEXT NOT NULL,
    recipient VARCHAR(100),
    channel VARCHAR(20) NOT NULL,
    send_status INT NOT NULL DEFAULT 2,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    error_message VARCHAR(500),
    scheduled_time TIMESTAMP NULL,
    send_time TIMESTAMP NULL,
    expire_time TIMESTAMP NULL,
    priority INT DEFAULT 3,
    is_urgent BOOLEAN DEFAULT FALSE,
    related_id VARCHAR(50),
    related_type VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);