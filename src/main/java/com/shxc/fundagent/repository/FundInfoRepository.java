package com.shxc.fundagent.repository;

import com.shxc.fundagent.entity.FundInfo;
import com.shxc.fundagent.enums.FundType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 基金基础信息数据访问接口
 */
@Repository
public interface FundInfoRepository extends JpaRepository<FundInfo, Long> {

    /**
     * 根据基金代码查询基金信息
     */
    Optional<FundInfo> findByFundCode(String fundCode);

    /**
     * 根据基金代码列表查询基金信息
     */
    List<FundInfo> findByFundCodeIn(List<String> fundCodes);

    /**
     * 根据基金类型查询基金信息
     */
    List<FundInfo> findByFundType(FundType fundType);

    /**
     * 根据基金类型分页查询
     */
    Page<FundInfo> findByFundType(FundType fundType, Pageable pageable);

    /**
     * 根据风险等级查询
     */
    List<FundInfo> findByRiskLevel(Integer riskLevel);

    /**
     * 根据是否启用查询
     */
    List<FundInfo> findByIsActive(Boolean isActive);

    /**
     * 根据基金名称模糊查询
     */
    List<FundInfo> findByFundNameContaining(String fundName);

    /**
     * 根据基金公司查询
     */
    List<FundInfo> findByFundCompany(String fundCompany);

    /**
     * 查询所有启用的基金
     */
    @Query("SELECT f FROM FundInfo f WHERE f.isActive = true")
    List<FundInfo> findAllActiveFunds();

    /**
     * 统计各类型的基金数量
     */
    @Query("SELECT f.fundType, COUNT(f) FROM FundInfo f GROUP BY f.fundType")
    List<Object[]> countByFundType();

    /**
     * 根据基金代码删除
     */
    void deleteByFundCode(String fundCode);

    /**
     * 检查基金代码是否存在
     */
    boolean existsByFundCode(String fundCode);

    /**
     * 批量更新基金启用状态
     */
    @Query("UPDATE FundInfo f SET f.isActive = :isActive WHERE f.fundCode IN :fundCodes")
    int updateIsActiveByFundCodes(@Param("fundCodes") List<String> fundCodes, @Param("isActive") Boolean isActive);
}