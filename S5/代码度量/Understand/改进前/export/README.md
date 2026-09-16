# Understand 最终“改进前”导出说明

- 最终 Analyze All：2026-09-15 09:52，范围为 `backend/src/main/java` 的77个 Java 文件。
- `project_metrics.csv` 与 `file_metrics.csv` 已逐项核对最终 `local/projectmetrics.dat`；数值和77个文件均一致。
- `class_metrics.csv` 来自最终分析后的 `Low Maintainability Classes Table`，阈值为 `CountDeclMethod >= 20 OR CountClassCoupled >= 20 OR PercentLackOfCohesion >= 90`。
- `method_metrics.csv` 来自最终分析后的 `Low Maintainability Functions` 候选报告，筛选为 `Cyclomatic >= 4 AND CountLine >= 1 AND MaxNesting >= 1`，共17个有方法体且圈复杂度至少4的候选；报告界面已恢复默认阈值。
- `Understand_文件度量可视化.xlsx` 是此前辅助可视化，不作为本轮最终类耦合和分析时间的依据；最终口径以四个 CSV、`metric_mapping.csv` 与汇总说明为准。
- CLI/Python API 因当前许可证不提供 API 授权，不能直接执行报告导出；CSV 值均来自 Understand 最终缓存或 GUI 交互报表，不用旧脚本替代方法复杂度。
