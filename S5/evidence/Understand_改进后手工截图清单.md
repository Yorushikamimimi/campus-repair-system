# Understand 改进后手工截图清单

先重新打开原工程，确认11:24 Analyze与HEAD d6899c2对应。正式截图由用户完成，本轮未截图。

1. 改进后Project Home：覆盖分析时间、77 Files、Parse Accuracy、Unanalyzed、错误/警告与项目汇总。
2. 改进后类级明细：四个原超阈类及五个改进类，至少覆盖CountDeclMethod、CountClassCoupled、PercentLackOfCohesion；不得只截改善项。
3. C3 MaintenanceServiceImpl方法/文件Metrics：ensureRecordComplete及三个新增private方法。
4. C4 StatisticsServiceImpl方法/文件Metrics：averageRepairDuration及三个新增private方法，覆盖CountStmtExe。

图5-4推荐StatisticsServiceImpl：主方法Cyclomatic 6→3、MaxNesting 2→1，方便展示；同时如实展示类耦合31→32。若仅强调主方法圈复杂度降幅，C3为6→2，但需同时显示辅助方法。
