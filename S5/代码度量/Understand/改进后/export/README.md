# Understand 改进后复量（未完成）

- HEAD：d6899c297da083ffcf2eb2bcb44d31e5c29ee518；分支 feature/s5-metrics。
- 原工程 Analyze All 完成时间：2026-09-15 11:24；范围缓存为77个 Java 源文件。
- project_metrics.csv、file_metrics.csv、raw JSON 逐字序列化自本次 local/projectmetrics.dat；不采用旧值或源码解析器代替工具值。
- class_metrics.csv：7个指定目标类的最终 GUI 报告实测值，不是全类导出。
- method_metrics.csv：C1～C5及13个新增 private 方法的目标清单。GUI_VERIFIED 行为 GUI 实测；MISSING_DATA 行留空，空白不表示0。CountStmtExe 未取到。
- 本文件集是工具缓存/GUI 数据的转录，不冒充可用的原生 CLI 报告导出。
- GUI 阈值行数：Cyclomatic>=2/3/4/5/6/7 分别78/43/13/4/1/0；MaxNesting>=1/2/3/4 分别83/9/1/0。
- 最大值可确认：Cyclomatic=6，MaxNesting=3。全量指标有效分母尚未核实，因此项目 Avg Complexity、Avg Depth 留为待核实，不用 Functions=314冒充分母。
- 全量方法表的可访问性树没有返回行，随后 Understand 退出；恢复尝试返回 noWindowsAvailable。原因未确定。
- Parse Accuracy、Unanalyzed 和完整外部库 warning 清单未读取；源码汇总 CountAnalysisError=0、CountAnalysisWarning=0，不能扩大为外部库 warning=0。
- 改进前目录未覆盖；未修改源码、DOCX，未截图、commit、push。
- 恢复入口：重新打开原工程（不新建），先避开全量 Low Maintainability 表，补 C5 主方法、C4三项private方法、CountStmtExe及全量有值分母；之后再确认报告填写可用。
