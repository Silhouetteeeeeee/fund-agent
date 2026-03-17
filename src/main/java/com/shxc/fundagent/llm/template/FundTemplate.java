package com.shxc.fundagent.llm.template;

import com.shxc.fundagent.strategy.model.FundPositionContext;
import com.shxc.fundagent.strategy.model.PortfolioContext;

import java.util.List;


public class FundTemplate {

    public static final String FFO_PROFESSIONAL_PROMPT = """
        【角色定位】
        你是一名资深的公募基金 FOF 投资经理，拥有丰富的组合投研经验，擅长基金组合的风格归因、风险预算管理、收益拆解与优化，所有分析基于基金最新定期报告披露的持仓数据、风格因子暴露、业绩归因模型，严谨专业，可使用标准金融术语。
        
        【输入信息】
        1. 持仓明细：
        
        %s
        
        2. 投资约束与基准：
        - 投资目标：绝对收益
        - 风险预算：组合年化波动率目标≤10%%，最大回撤控制≤18%%
        - 投资期限：5 年
        - 业绩基准：偏股混合型基金指数 885001.WI/60%%沪深 300+40%%中债总指数
        - 投资限制：单一基金仓位不超过 20%%，单一行业暴露不超过 30%%，权益类资产仓位最高不超过 80%%、最低不低于 30%%，不能加杠杆投资，不能买分级基金、衍生品等高风险品种
        
        【核心分析要求】
        请严格基于以上信息，完成以下 4 个模块的深度分析：
        1. 组合深度归因
        - 收益归因：Brinson 归因拆解，区分资产配置收益、行业选择收益、个基选择收益、择时收益
        - 风格归因：基于 Barra CNE5 模型，拆解组合在规模、价值、成长、盈利、波动、流动性等因子上的暴露情况，判断风格一致性与偏离度
        - 业绩评价：计算组合的年化收益、最大回撤、夏普比率、卡玛比率、信息比率、索提诺比率，与业绩基准、同类组合对标分析
        2. 风险与分散化分析
        - 组合风险贡献度分析，计算单只基金/资产类别的边际风险贡献，识别核心风险来源
        - 基金间相关性矩阵分析，评估组合分散化效果，识别高度相关的冗余持仓
        - 极端场景压力测试：模拟熊市、行业黑天鹅等极端行情下的组合回撤表现，评估风险抵御能力
        3. 持仓有效性诊断
        - 基金经理能力圈与业绩持续性分析，评估 alpha 获取能力的稳定性、风格漂移情况、规模对业绩的影响
        - 持仓重叠度分析：计算重仓股重合比例，识别组合内的隐性同质化持仓
        - 交易成本评估：A/C 类选择合理性、申赎费率、调仓摩擦成本分析
        4. 量化优化方案
        - 基于风险预算的仓位优化，给出各资产/风格/行业的目标配置比例，实现目标波动率下的收益最大化
        - 冗余持仓的清理建议，给出调仓方向与优先级
        - 再平衡策略设计，给出触发再平衡的阈值与执行节奏
        - 尾部风险对冲的可选方案建议
        
        【输出规则】
        1. 专业严谨，使用标准金融术语，结构化排版，数据化呈现，关键结论加粗
        2. 所有分析必须有数据支撑，不得主观臆断
        3. 输出包含核心结论摘要、分模块详细分析、优化方案三个部分
        4. 全文末尾标注合规提示：以上分析仅为基于历史数据的量化诊断，不构成任何投资建议，历史业绩不代表未来表现，市场有风险，投资需谨慎
        """;

    public static String buildFfoPrompt(PortfolioContext context) {
        List<FundPositionContext> funds = context.getFunds();
        StringBuilder fundStr = new StringBuilder();
        fundStr.append("""
                | 基金代码 | 基金全称 | 基金类型 | 持仓市值 | 仓位占比 | 任职基金经理信息 | 持仓收益率 | 持仓时长 | 日涨幅 | 周涨幅 | 月涨幅 | 年涨幅 |
                | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
                """);
        for (FundPositionContext row : funds) {
            fundStr.append("| ").append(row.getFundCode()).append(" ")
                    .append("| ").append(row.getFundName()).append(" ")
                    .append("| ").append(row.getFundType()).append(" ")
                    .append("| ").append(row.getNetValue()).append(" ")
                    .append("| ").append(row.getPosition()).append("% ")
                    .append("| ").append(row.getManagerInfo()).append(" ")
                    .append("| ").append(row.getProfitRate()).append("% ")
                    .append("| ").append(row.getHoldDays()).append("天 ")
                    .append("| ").append(row.getDailyChangePercent()).append("% ")
                    .append("| ").append(row.getWeeklyChangePercent()).append("% ")
                    .append("| ").append(row.getMonthlyChangePercent()).append("% ")
                    .append("| ").append(row.getYearlyChangePercent()).append("% |\n");
        }
        fundStr.append("=== 整体账户情况 ===\n总资产：").append(context.getTotalAssets()).append(" 元\n")
                .append("总投入：").append(context.getTotalCost()).append(" 元\n")
                .append("总收益：").append(context.getTotalProfit()).append(" 元\n")
                .append("总收益率：").append(context.getTotalProfitRate()).append("%\n")
                .append("可用资金：").append(context.getAvailableCash()).append(" 元\n")
                .append("目标仓位：").append(context.getTargetPosition()).append("%\n");

        return FFO_PROFESSIONAL_PROMPT.formatted(fundStr.toString());

    }

}
