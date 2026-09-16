# S5 AI代码前后度量数据（待补全）

固定对象：StatisticsServiceImpl.averageRepairDuration；人工改进commit d747a32。
改前版本是否确为“AI生成初版”由人工责任记录确认，此处只绑定基线代码版本，不推断生成者。

| 指标 | 改前 | 改后 |
|---|---:|---:|
| Cyclomatic | 6 | 3 |
| CountLine | 18 | 15 |
| CountStmtExe | N/A（改前CSV无该列） | missing_data（许可阻塞，未取得） |
| MaxNesting | 2 | 1 |
| 类CountDeclMethod | 5 | 8 |
| WMC（采用CountDeclMethod方法数口径，非复杂度加权和） | 5 | 8 |
| CountClassCoupled | 31 | 32 |
| PercentLackOfCohesion | 47 | 67 |
| 类Comment Lines | 未取到 | 未取到 |
| 重复代码 | N/A（Understand本次未取得重复代码专门度量结果） | N/A（Understand本次未取得重复代码专门度量结果） |

三个新增private方法的指标待补，不用源码自行计算代替Understand实测。

## 第9节正式映射与最终补数状态

V(G)=本方法Cyclomatic（6→3）；Max Depth=本方法MaxNesting（2→1）；CBO=CountClassCoupled（31→32）。原“Avg Stmts/Method”应说明为“该方法语句数/方法级语句指标”，必须使用本方法CountStmtExe，不使用项目793/314替代。类/文件comment ratio尚未取得，不能拿项目0.25%冒充该类注释率。

软件安装目录存在ClonedFunctions报告插件，但存在插件不等于本次启用或已产生结果；本次没有直接重复代码检测结果，不能断言无重复。

最终补数时重新打开指定原工程，Understand弹出Licensing（Build1262），要求License Code，OK禁用；工程未成功载入，分析及报告菜单禁用。因此CountStmtExe、三个C4辅助方法及类/文件comment ratio仍为missing_data，第9节尚不能按完整实测表正式填写。类级耦合轻微上升，原因未经验证，不归因于新增依赖。
