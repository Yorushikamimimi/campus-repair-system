# Understand 5项代码改进复量（待补全）

改前来自受保护method_metrics.csv；改后来自本次GUI实测。空值为missing_data，不是0。

| 编号 | 责任人 | 对象 | 指标 | 改前值 | 改后值 | 变化 | 结论 | commit |
|---|---|---|---|---:|---:|---|---|---|
| C1 | R1 邓鸿翔 | com.campusrepair.security.CurrentUser.requireId | Cyclomatic | 5 | 2 | 5 → 2 | 主方法指标下降 | 7cb9cb7 |
| C1 | R1 邓鸿翔 | com.campusrepair.security.CurrentUser.requireId | CountLine | 15 | 6 | 15 → 6 | 主方法指标下降 | 7cb9cb7 |
| C1 | R1 邓鸿翔 | com.campusrepair.security.CurrentUser.requireId | MaxNesting | 2 | 1 | 2 → 1 | 主方法指标下降 | 7cb9cb7 |
| C1 | R1 邓鸿翔 | com.campusrepair.security.CurrentUser.requireId | CountStmtExe | 未导出 | 未取到 | 未导出 → 未取到 | 待补实测 | 7cb9cb7 |
| C2 | R2 陈语 | com.campusrepair.service.impl.QueryServiceImpl.normalizeStatus | Cyclomatic | 5 | 2 | 5 → 2 | 主方法指标下降 | eb69f63 |
| C2 | R2 陈语 | com.campusrepair.service.impl.QueryServiceImpl.normalizeStatus | CountLine | 10 | 4 | 10 → 4 | 主方法指标下降 | eb69f63 |
| C2 | R2 陈语 | com.campusrepair.service.impl.QueryServiceImpl.normalizeStatus | MaxNesting | 2 | 1 | 2 → 1 | 主方法指标下降 | eb69f63 |
| C2 | R2 陈语 | com.campusrepair.service.impl.QueryServiceImpl.normalizeStatus | CountStmtExe | 未导出 | 未取到 | 未导出 → 未取到 | 待补实测 | eb69f63 |
| C3 | R3 羊鸣天 | com.campusrepair.service.impl.MaintenanceServiceImpl.ensureRecordComplete | Cyclomatic | 6 | 2 | 6 → 2 | 主方法指标下降 | b89f964 |
| C3 | R3 羊鸣天 | com.campusrepair.service.impl.MaintenanceServiceImpl.ensureRecordComplete | CountLine | 18 | 8 | 18 → 8 | 主方法指标下降 | b89f964 |
| C3 | R3 羊鸣天 | com.campusrepair.service.impl.MaintenanceServiceImpl.ensureRecordComplete | MaxNesting | 2 | 1 | 2 → 1 | 主方法指标下降 | b89f964 |
| C3 | R3 羊鸣天 | com.campusrepair.service.impl.MaintenanceServiceImpl.ensureRecordComplete | CountStmtExe | 未导出 | 未取到 | 未导出 → 未取到 | 待补实测 | b89f964 |
| C4 | R4 赵俊超 | com.campusrepair.service.impl.StatisticsServiceImpl.averageRepairDuration | Cyclomatic | 6 | 3 | 6 → 3 | 主方法指标下降 | d747a32 |
| C4 | R4 赵俊超 | com.campusrepair.service.impl.StatisticsServiceImpl.averageRepairDuration | CountLine | 18 | 15 | 18 → 15 | 主方法指标下降 | d747a32 |
| C4 | R4 赵俊超 | com.campusrepair.service.impl.StatisticsServiceImpl.averageRepairDuration | MaxNesting | 2 | 1 | 2 → 1 | 主方法指标下降 | d747a32 |
| C4 | R4 赵俊超 | com.campusrepair.service.impl.StatisticsServiceImpl.averageRepairDuration | CountStmtExe | 未导出 | 未取到 | 未导出 → 未取到 | 待补实测 | d747a32 |
| C5 | R1 邓鸿翔 | com.campusrepair.storage.LocalFileStorageServiceImpl.validateFile | Cyclomatic | 6 | 未取到 | 6 → 未取到 | 待补实测 | d6899c2 |
| C5 | R1 邓鸿翔 | com.campusrepair.storage.LocalFileStorageServiceImpl.validateFile | CountLine | 12 | 未取到 | 12 → 未取到 | 待补实测 | d6899c2 |
| C5 | R1 邓鸿翔 | com.campusrepair.storage.LocalFileStorageServiceImpl.validateFile | MaxNesting | 1 | 未取到 | 1 → 未取到 | 待补实测 | d6899c2 |
| C5 | R1 邓鸿翔 | com.campusrepair.storage.LocalFileStorageServiceImpl.validateFile | CountStmtExe | 未导出 | 未取到 | 未导出 → 未取到 | 待补实测 | d6899c2 |

## 新增 private 方法

| 编号 | 方法 | Cyclomatic | CountLine | MaxNesting | 结论 |
|---|---|---:|---:|---:|---|
| C1 | com.campusrepair.security.CurrentUser.principalToUserId | 4 | 11 | 2 | 一般：主方法下降，复杂度4的辅助逻辑仍偏高 |
| C2 | com.campusrepair.service.impl.QueryServiceImpl.normalizeStatusValue | 2 | 5 | 1 | 辅助方法复杂度2～3；主方法完整实测后判定 |
| C2 | com.campusrepair.service.impl.QueryServiceImpl.displayNameToCode | 3 | 6 | 2 | 辅助方法复杂度2～3；主方法完整实测后判定 |
| C3 | com.campusrepair.service.impl.MaintenanceServiceImpl.validateProcessDescription | 2 | 6 | 1 | 辅助方法复杂度2～3；主方法完整实测后判定 |
| C3 | com.campusrepair.service.impl.MaintenanceServiceImpl.validateCompletedRecord | 2 | 5 | 1 | 辅助方法复杂度2～3；主方法完整实测后判定 |
| C3 | com.campusrepair.service.impl.MaintenanceServiceImpl.validateProcessingRecord | 3 | 8 | 1 | 辅助方法复杂度2～3；主方法完整实测后判定 |
| C4 | com.campusrepair.service.impl.StatisticsServiceImpl.isValidDurationRecord | 未取到 | 未取到 | 未取到 | 待取证，不能判成功 |
| C4 | com.campusrepair.service.impl.StatisticsServiceImpl.calculateDurationMinutes | 未取到 | 未取到 | 未取到 | 待取证，不能判成功 |
| C4 | com.campusrepair.service.impl.StatisticsServiceImpl.roundedAverage | 未取到 | 未取到 | 未取到 | 待取证，不能判成功 |
| C5 | com.campusrepair.storage.LocalFileStorageServiceImpl.validateOrderId | 2 | 3 | 1 | 辅助方法复杂度2～3；主方法完整实测后判定 |
| C5 | com.campusrepair.storage.LocalFileStorageServiceImpl.validateFilename | 3 | 7 | 1 | 辅助方法复杂度2～3；主方法完整实测后判定 |
| C5 | com.campusrepair.storage.LocalFileStorageServiceImpl.validateContent | 2 | 3 | 1 | 辅助方法复杂度2～3；主方法完整实测后判定 |
| C5 | com.campusrepair.storage.LocalFileStorageServiceImpl.validateExtension | 2 | 5 | 1 | 辅助方法复杂度2～3；主方法完整实测后判定 |

## 类级前后

| 类 | 改前方法数/耦合/LCOM% | 改后方法数/耦合/LCOM% |
|---|---|---|
| CurrentUser | 原导出未包含 | 3 / 8 / 0 |
| QueryServiceImpl | 原导出未包含 | 8 / 13 / 63 |
| MaintenanceServiceImpl | 18 / 31 / 81 | 21 / 31 / 83 |
| StatisticsServiceImpl | 5 / 31 / 47 | 8 / 32 / 67 |
| LocalFileStorageServiceImpl | 原导出未包含 | 11 / 14 / 73 |
| RepairOrderServiceImpl | 11 / 29 / 76 | 11 / 29 / 76 |
| DispatchServiceImpl | 13 / 24 / 80 | 13 / 24 / 80 |

方法级复杂度得到改善，但类级耦合超阈仍然存在；StatisticsServiceImpl耦合还增加1。原阈值>20，四项仍全部超阈。未补猜缺失的改前类指标。

## 最终补数核验

原改前CSV不含CountStmtExe列，以上五个改前CountStmtExe的“未导出”正式口径为N/A（原CSV无该指标）。改后CountStmtExe仍缺实测，不能填0或按源码猜测。

指定原工程重新打开被Licensing（Build1262）拦截：要求License Code，OK禁用，未载入工程。完整方法导出、C5主方法以及C4三个辅助方法剩余指标未能取得，仍为missing_data。现有部分CSV没有被伪装成全量完成版。

类耦合结论保持MaintenanceServiceImpl 31→31、StatisticsServiceImpl 31→32、RepairOrderServiceImpl 29→29、DispatchServiceImpl 24→24。方法级主方法复杂度下降并不代表类级耦合已经改善；StatisticsServiceImpl仅记录轻微上升，不猜测原因。
