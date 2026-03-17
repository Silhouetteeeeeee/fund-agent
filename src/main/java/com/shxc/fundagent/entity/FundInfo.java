package com.shxc.fundagent.entity;

import com.shxc.fundagent.enums.FundType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基金基础信息实体
 * 对应详细设计文档中的 fund_info 表
 */
@Entity
@Table(name = "fund_info", uniqueConstraints = {
        @UniqueConstraint(columnNames = "fund_code")
})
@Data
public class FundInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 基金代码，唯一标识
     */
    @Column(name = "fund_code", nullable = false, length = 10)
    private String fundCode;

    /**
     * 基金名称
     */
    @Column(name = "fund_name", nullable = false, length = 100)
    private String fundName;

    /**
     * 基金类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "fund_type", nullable = false, length = 20)
    private FundType fundType;

    /**
     * 风险等级 1-5（1最低，5最高）
     */
    @Column(name = "risk_level")
    private Integer riskLevel = 3;

    /**
     * 基金公司
     */
    @Column(name = "fund_company", length = 50)
    private String fundCompany;

    /**
     * 成立日期
     */
    @Column(name = "established_date")
    private LocalDate establishedDate;

    @Column(name = "syl_1n")
    private BigDecimal syl1n;

    @Column(name = "syl_6y")
    private BigDecimal syl6y;

    @Column(name = "syl_3y")
    private BigDecimal syl3y;

    @Column(name = "syl_1y")
    private BigDecimal syl1y;

    /**
     * 基金经理
     */
    @Column(name = "manager_info", length = 50)
    private String managerInfo;

    /**
     * 基金规模（亿元）
     */
    @Column(name = "fund_size")
    private Double fundSize;

    /**
     * 管理费率（%）
     */
    @Column(name = "management_fee")
    private Double managementFee;

    /**
     * 托管费率（%）
     */
    @Column(name = "custody_fee")
    private Double custodyFee;

    /**
     * 是否启用监控
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * 备注信息
     */
    @Column(name = "remark", length = 200)
    private String remark;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 无参构造函数
     */
    public FundInfo() {
    }

    /**
     * 带基本参数的构造函数
     */
    public FundInfo(String fundCode, String fundName, FundType fundType) {
        this.fundCode = fundCode;
        this.fundName = fundName;
        this.fundType = fundType;
    }

    /**
     * 获取风险等级描述
     */
    public String getRiskLevelDescription() {
        if (riskLevel == null) {
            return "未评级";
        }
        switch (riskLevel) {
            case 1: return "低风险";
            case 2: return "中低风险";
            case 3: return "中风险";
            case 4: return "中高风险";
            case 5: return "高风险";
            default: return "未知风险";
        }
    }
}