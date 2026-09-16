# Understand 手工截图清单（最多8张）

> 截图前先确认窗口底部显示 `Analysis Completed: 2026-09-15 09:52`，Home 为 `100% Parse Accuracy`。截图只保留 Understand 主窗口，不带 Word、浏览器或桌面隐私内容。

| 序号 | 在哪里打开 | 必须显示的字段/内容 | 推荐窗口范围 |
|---|---|---|---|
| 1 | `Project > Configure Project > Sources` | 工程名；源码树定位到 `backend/src/main/java`；`test/java` 为 Excluded | 主窗口全屏，左侧源码范围与配置区完整 |
| 2 | `Project > Configure Project > Languages > Java` | Version=`10–21`；`Use Spring` 已勾选；Classpath中能看到 JDK 与 Maven依赖 | Java配置页全窗口；路径列表显示前几项即可 |
| 3 | 点击工具栏 `Home` | `100% Parse Accuracy`、Files=77、Lines=3183、Classes=79、Functions=301；底部完成时间 | Home主窗口全屏，顶部统计卡和底部状态栏都入镜 |
| 4 | `Project > View Analysis Log`（或当前版本对应 Analysis Log） | 最终 `0 Unanalyzed files`、`0 Errors`、`43 Warnings`；任取数条外部jar路径 | 日志窗口聚焦最终09:52区段，不必截旧928条全文 |
| 5 | `Reports > Low Maintainability Classes Table` | 阈值说明；`MaintenanceServiceImpl=18/31/81`、`RepairOrderServiceImpl=11/29/76`、`StatisticsServiceImpl=5/31/47` | 报表全宽，至少覆盖三行重点类及三列指标 |
| 6 | `Reports > Low Maintainability Functions` | 默认阈值 `6/18/2`；`listTasks=6/43/2`、`ensureRecordComplete=6/18/2`、`averageRepairDuration=6/18/2` | 报表区域全宽，阈值和三行全部入镜 |
| 7 | 在 Project Browser 打开 `MaintenanceServiceImpl.java`，再开 `Metrics Browser` 并选中 `listTasks` | Entity名；Cyclomatic=6、CountLine=43、MaxNesting=2 | 代码与右/下方 Metrics 同屏，方法签名和三项值清晰 |
| 8 | 打开 `StatisticsServiceImpl.java`，在 `Metrics Browser` 选中 `averageRepairDuration` | Entity名；Cyclomatic=6、CountLine=18、MaxNesting=2 | 代码与 Metrics 同屏，完整显示方法和三项值 |

截图文件名建议依次使用 `01_工程范围.png` 至 `08_averageRepairDuration方法度量.png`。这是命名建议，不要求本轮创建或整理截图文件。
