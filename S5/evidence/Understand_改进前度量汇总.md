# Understand “改进前”度量汇总

## 最终分析基线

- 工具：SciTools Understand 8.0（Build 1262），macOS。
- 工程：`S5/代码度量/Understand/project/campus-repair-system-baseline.und`。
- 最终 Analyze All：2026-09-15 09:52；77个 Java 项目文件，0 Unanalyzed，0 Errors，43 Warnings，Home 显示 100% Parse Accuracy。
- 源码范围：仅 `backend/src/main/java`；`backend/src/test/java` 已显式排除。
- 基线 Git：`feature/s5-metrics` @ `80a524e75e415f8762e3d50cc64cee8fe8edec2e`。
- 本轮只优化 Understand 工程设置和更新 S5 度量证据；未修改业务源码，未开始“改进后”度量。

## 解析异常与配置优化

原 09:07 分析同样完成77个文件，且为0 Unanalyzed、0 Errors，但有928条 `Missing package` / `Missing type`。日志同时缺少 `java.lang.String`、`java.util.List` 等 JDK 类型，以及 Spring Boot、Spring Security、Jakarta Validation、MyBatis、Lombok 等依赖类型，说明当时 Java classpath 为空。0% Parse Accuracy 反映的是符号解析不完整，不是77个源码文件发生 Java 语法失败；当时仍能建立源码实体并生成 Metrics，但耦合等依赖解析型指标可信度较低。

最终配置保持 Java `10–21` 语言档位，启用 `Use Spring`，导入 JDK 21 的 `java.base.jmod`、`src.zip` 和由 `backend/pom.xml` 导出的92个 Maven jar，共94条有效 classpath，然后执行 Analyze All。最终43条 warning 全部指向外部库：Spring Boot 可选 Groovy、Spring Security 可选 SAML2/OAuth2/LDAP 类型，以及 JDK `src.zip` 的内部 `sun.awt` 引用；没有一条指向 `backend/src/main/java`。因此项目源码当前可按 100% Parse Accuracy 使用。

SciTools 官方说明也指出 Java 缺失 classpath 是 undefined entities 的首要原因，Maven `pom.xml` 不能被 Understand 直接读取，需要先导出 classpath；Java 工程还应按需要加入 JDK 模块/源码并启用 Spring。

## 项目级最终 Metrics

| 指标 | 数值 |
|---|---:|
| Lines / CountLine | 3183 |
| Code Lines / CountLineCode | 2719 |
| Blank Lines / CountLineBlank | 460 |
| Comment Lines / CountLineComment | 7 |
| Comment to Code Ratio / RatioCommentToCode | 0.00 |
| Files / CountDeclFile | 77 |
| Classes / CountDeclClass | 79 |
| Functions / CountDeclFunction | 301 |
| Declarative Statements / CountStmtDecl | 1287 |
| Executable Statements / CountStmtExe | 788 |

## 第2节 LOC 工具口径

| 指标 | Understand指标名 | 实际值 | 是否直接获得 | 备注 |
|---|---|---:|---|---|
| 物理行 | CountLine / Lines | 3183 | 是 | 项目范围物理行数 |
| 有效代码行 | CountLineCode / Code Lines | 2719 | 是 | Understand 直接分类结果 |
| 注释行 | CountLineComment / Comment Lines | 7 | 是 | 不用总行数反推 |
| 空行 | CountLineBlank / Blank Lines | 460 | 是 | 不用总行数反推 |
| 注释率 | RatioCommentToCode / Comment to Code Ratio | 0.00 | 是 | 工具直接值；界面保留两位小数 |

## 第4节 SourceMonitor 12项映射

Statements 统一采用 `Executable Statements=788`，理由是它最接近程序控制与行为语句，且不会把声明混入流程语句。平均项使用同一套 Understand 分母。复杂度与深度来自最终 `Low Maintainability Functions` 中 `Cyclomatic >= 4 AND CountLine >= 1 AND MaxNesting >= 1` 的17个有方法体候选；平均值只代表这17个候选，不冒充301个函数的全项目平均值。

| SourceMonitor模板指标 | Understand实际指标 | 改进前值 | 获取方式 | 是否直接对应 | 备注 |
|---|---|---:|---|---|---|
| Lines | CountLine | 3183 | Project Home / projectmetrics.dat | 是 | 物理行 |
| Statements | CountStmtExe | 788 | Project Home / projectmetrics.dat | 否 | 统一采用的替代口径：可执行语句 |
| % Branches | NOT_DIRECT | N/A | 检查项目与方法指标 | 否 | 没有完全等价的分支语句比例；不自造公式 |
| % Comments | RatioCommentToCode | 0.00 | Project Home / projectmetrics.dat | 是 | Understand 直接值 |
| Classes | CountDeclClass | 79 | Project Home / projectmetrics.dat | 是 | Understand 类实体口径 |
| Methods per Class | Functions / Classes | 3.81 | `301 / 79` | 否 | 同一工具实体口径计算 |
| Functions | CountDeclFunction | 301 | Project Home / projectmetrics.dat | 是 | Understand 函数实体口径 |
| Avg Stmts/Method | CountStmtExe / CountDeclFunction | 2.62 | `788 / 301` | 否 | 与 Statements 口径一致 |
| Max Complexity | Cyclomatic | 6 | method_metrics.csv | 是 | 17个高复杂度候选的最大值 |
| Avg Complexity | Cyclomatic | 4.76 | 17个候选算术平均 | 否 | 候选集平均，不是全项目平均 |
| Max Depth | MaxNesting | 3 | method_metrics.csv | 是 | 17个高复杂度候选的最大值 |
| Avg Depth | MaxNesting | 1.53 | 17个候选算术平均 | 否 | 与复杂度采用同一候选集 |

## Classes 与 Functions 口径解释

- 第1节人工统计继续使用 `54 class + 21 interface = 75`；加2个 enum 后，顶层 Java 类型为77。
- 源码另有两个命名嵌套类：`AcceptanceServiceImpl.AcceptanceContext` 与 `MaintenanceServiceImpl.TaskContext`。Understand 的 Classes 将 class、interface、enum、record 等类型统一纳入类实体口径，因此 `77个顶层类型 + 2个命名嵌套类 = 79`。本项目无需用匿名类或 synthetic entity 补数。
- 源码脚本的257是“普通方法、排除构造方法”口径；Understand 的301是更宽的函数实体口径。GUI 实体树已确认会列出构造方法、接口方法和 Lambda Method（例如 `listTasks.(lambda_expr_1)`）。当前证据不足以把44个差额逐一归因到 record 隐式成员、Lombok getter/setter 或静态初始化，因此不作无证据拆分。

## 重点类与方法

最终类报告阈值：`CountDeclMethod >= 20 OR CountClassCoupled >= 20 OR PercentLackOfCohesion >= 90`。最终方法报告阈值：`Cyclomatic >= 6 AND CountLine >= 18 AND MaxNesting >= 2`。

| 对象 | 最终实测指标 |
|---|---|
| `MaintenanceServiceImpl` | Code Lines=256；CountDeclMethod=18；CountClassCoupled=31；PercentLackOfCohesion=81 |
| `RepairOrderServiceImpl` | Code Lines=163；CountDeclMethod=11；CountClassCoupled=29；PercentLackOfCohesion=76 |
| `DispatchServiceImpl` | Code Lines=148；CountDeclMethod=13；CountClassCoupled=24；PercentLackOfCohesion=80 |
| `StatisticsServiceImpl` | Code Lines=97；CountDeclMethod=5；CountClassCoupled=31；PercentLackOfCohesion=47 |
| `MaintenanceServiceImpl.listTasks` | Cyclomatic=6；CountLine=43；MaxNesting=2 |
| `MaintenanceServiceImpl.ensureRecordComplete` | Cyclomatic=6；CountLine=18；MaxNesting=2 |
| `StatisticsServiceImpl.averageRepairDuration` | Cyclomatic=6；CountLine=18；MaxNesting=2 |

`listTasks` 的 Understand 正式口径固定为 `V(G)=6`；旧自写脚本的11属于另一工具口径，不进入本次 Understand 前后对比表。

## 最终数据位置与限制

- `S5/代码度量/Understand/改进前/export/project_metrics.csv`
- `S5/代码度量/Understand/改进前/export/file_metrics.csv`
- `S5/代码度量/Understand/改进前/export/class_metrics.csv`
- `S5/代码度量/Understand/改进前/export/method_metrics.csv`
- `S5/代码度量/Understand/改进前/metric_mapping.csv`
- `S5/代码度量/Understand/改进前/超阈候选.md`
- `S5/evidence/Understand_手工截图清单.md`

当前演示/公开仓库模式支持 GUI 分析，但 CLI/Python API 报告导出仍因 API 许可证不可用而失败。最终项目/文件 CSV 与 `projectmetrics.dat` 一致；类/方法 CSV 来自最终 GUI 交互报表。Understand 官方指标文档说明工具不内置通用推荐阈值，因此学校第5节应明确标注“Understand 报告当前阈值”或“本组阈值”，不能把阈值冒充工具官方推荐值。
