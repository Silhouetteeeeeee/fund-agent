package com.shxc.fundagent.repository;

import com.shxc.fundagent.entity.FundHolding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 持仓信息数据访问接口
 */
@Repository
public interface FundHoldingRepository extends JpaRepository<FundHolding, Long> {

    /**
     * 根据基金代码查询持仓记录
     */
    List<FundHolding> findByFundCode(String fundCode);

    /**
     * 根据基金代码和状态查询持仓记录
     */
    List<FundHolding> findByFundCodeAndStatus(String fundCode, String status);

    /**
     * 查询所有活跃持仓（状态为ACTIVE）
     */
    List<FundHolding> findByStatus(String status);

    /**
     * 根据基金代码和持仓编号查询持仓记录
     */
    @Query("SELECT h FROM FundHolding h WHERE h.status = 'ACTIVE' and h.fundCode =:fundCode")
    FundHolding findActiveHoldingByFundCode(String fundCode);

    @Query("SELECT h FROM FundHolding h WHERE h.calculateDate < :calculateDate ")
    List<FundHolding> findFundHoldingsByCalculateDateBefore(LocalDate calculateDate);

    /**
     * 根据购买日期范围查询
     */
    List<FundHolding> findByPurchaseDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 统计持仓总成本
     */
    @Query("SELECT SUM(h.costPrice * h.holdingAmount) FROM FundHolding h WHERE h.status = 'ACTIVE'")
    Optional<BigDecimal> sumTotalCost();

    /**
     * 统计持仓总市值
     */
    @Query("SELECT SUM(h.holdingValue) FROM FundHolding h WHERE h.status = 'ACTIVE'")
    Optional<BigDecimal> sumTotalHoldingValue();

    /**
     * 按基金代码统计持仓成本
     */
    @Query("SELECT h.fundCode, SUM(h.costPrice * h.holdingAmount) FROM FundHolding h " +
           "WHERE h.status = 'ACTIVE' GROUP BY h.fundCode")
    List<Object[]> sumCostByFundCode();

    /**
     * 按基金代码统计持仓份额
     */
    @Query("SELECT h.fundCode, SUM(h.holdingAmount) FROM FundHolding h " +
           "WHERE h.status = 'ACTIVE' GROUP BY h.fundCode")
    List<Object[]> sumHoldingAmountByFundCode();

    /**
     * 查询指定基金的最大购买日期
     */
    @Query("SELECT MAX(h.purchaseDate) FROM FundHolding h WHERE h.fundCode = :fundCode")
    Optional<LocalDate> findMaxPurchaseDateByFundCode(@Param("fundCode") String fundCode);

    /**
     * 查询指定基金的平均持仓成本
     */
    @Query("SELECT AVG(h.costPrice) FROM FundHolding h WHERE h.fundCode = :fundCode AND h.status = 'ACTIVE'")
    Optional<BigDecimal> findAvgCostPriceByFundCode(@Param("fundCode") String fundCode);

    @Query("SELECT h.costPrice FROM FundHolding h WHERE h.fundCode = :fundCode AND h.status = 'ACTIVE'")
    Optional<BigDecimal> findCostPriceByFundCode(@Param("fundCode") String fundCode);

    /**
     * 批量更新持仓状态
     */
    @Query("UPDATE FundHolding h SET h.status = :status WHERE h.id IN :ids")
    int updateStatusByIds(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * 根据基金代码删除持仓记录
     */
    void deleteByFundCode(String fundCode);

    /**
     * 检查指定基金是否有活跃持仓
     */
    @Query("SELECT COUNT(h) > 0 FROM FundHolding h WHERE h.fundCode = :fundCode AND h.status = 'ACTIVE'")
    boolean existsActiveHoldingByFundCode(@Param("fundCode") String fundCode);
}