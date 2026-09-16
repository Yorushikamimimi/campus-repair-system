# Understand 第4节前后对照（待补全）

改前数值按本次任务固定数据；改后项目直接值来自11:24最终Analyze。Statements=CountStmtExe；Methods per Class=Functions/Classes；Avg Stmts/Method=CountStmtExe/Functions；% Comments=7/2756×100%=0.25%。没有为缺失指标创造公式。

| 指标 | 改进前 | 改进后 | 变化 | 本组阈值 | 判定 |
|---|---:|---:|---|---|---|
| Lines | 3183 | 3234 | 3183 → 3234（+51） | 未提供项目级强制阈值 | 如实记录变化 |
| Statements | 788 | 793 | 788 → 793（+5） | 未提供项目级强制阈值 | 如实记录变化 |
| % Branches | N/A | N/A | N/A → N/A | 未提供项目级强制阈值 | N/A或比例记录 |
| % Comments | 0.26% | 0.25% | 0.26% → 0.25% | 未提供项目级强制阈值 | N/A或比例记录 |
| Classes | 79 | 79 | 79 → 79（0） | 未提供项目级强制阈值 | 未变 |
| Methods per Class | 3.81 | 3.97 | 3.81 → 3.97（+0.16） | 未提供项目级强制阈值 | 如实记录变化 |
| Functions | 301 | 314 | 301 → 314（+13） | 未提供项目级强制阈值 | 如实记录变化 |
| Avg Stmts/Method | 2.62 | 2.53 | 2.62 → 2.53（-0.09） | 未提供项目级强制阈值 | 如实记录变化 |
| Max Complexity | 6 | 6 | 6 → 6（0） | 未提供项目级强制阈值 | 未变 |
| Avg Complexity | 1.55 | 待核实 | 1.55 → 待核实 | 未提供项目级强制阈值 | 缺有效分母，待补 |
| Max Depth | 3 | 3 | 3 → 3（0） | 未提供项目级强制阈值 | 未变 |
| Avg Depth | 0.29 | 待核实 | 0.29 → 待核实 | 未提供项目级强制阈值 | 缺有效分母，待补 |

源码汇总：Errors=0、Warnings=0；Files=77；Code Lines=2756；Blank Lines=474；Comment Lines=7；Declarative Statements=1297；Comment to Code Ratio=0.00（工具格式）。Parse Accuracy、Unanalyzed、外部库warning待GUI确认。

最终补数核验：指定原工程打开时出现Licensing（Build1262），要求License Code且OK禁用，工程未成功载入，分析及报告功能不可用。最终缓存projectmetrics.dat仍可读取Files=77、源码Errors=0、源码Warnings=0；该缓存不提供完整方法级数据、Parse Accuracy、Unanalyzed及外部库warning分项。不能将314 Functions直接视为有效Cyclomatic/MaxNesting分母。现有改后method_metrics.csv仍为部分取证版，两个平均值继续保留待核实，不以推算替代。需恢复可用许可后完成全量导出。
