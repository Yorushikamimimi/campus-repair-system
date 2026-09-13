# S5 源码度量工具

## 用法

在项目根目录执行：

```bash
python3 S5/代码度量/tools/measure_metrics.py
```

本次运行环境为 Python 3.12.13。脚本为项目内可复现的轻量度量器，不依赖未纳入项目的商业度量工具；本机未安装 `cloc`，因此 LOC 结果由脚本按源码文件逐行计算。

## 度量范围

- Java 主源码：`backend/src/main/java`
- Java 测试源码：`backend/src/test/java`
- 前端源码：`frontend/src` 下的 `.js`、`.vue`、`.css`、`.html`
- 排除构建产物、依赖目录、上传文件、Git 元数据和 S0～S4 文档

脚本统计有效代码行、空行和注释行；字符串与注释中的内容不计为代码。Java 类型按顶层 `class`、`interface`、`enum`、`record` 识别，`record` 按 class 计数。

## 口径

- 方法数、WMC：只统计普通方法，构造方法排除，WMC 每个方法权重为 1。
- RFC：本类普通方法集合与方法体中直接调用的外部方法名集合的并集大小。
- 圈复杂度：方法体初值为 1，按 `if`、循环、`catch`、`case`、逻辑运算和三元表达式决策点累加。
- DIT：沿源码中的父类链计算，根类深度为 0；NOC 只计直接子类。
- CBO：统计直接引用的系统类型及接口实现/扩展关系；JDK、Spring、第三方类型不计入，继承边不计入 CBO。
- LCOM：按 `max(P-Q, 0)` 计算；接口、无实例属性类型和纯 Repository 的 LCOM 按 S4 兼容口径记录为 0。
- 阈值：WMC>15、RFC>35、DIT>6、NOC>6、CBO>8、LCOM>1；实际超阈值对象与指标写入 `actual_over_threshold.csv`。

解析器面向本项目现有格式，用于课程度量和留痕，不替代完整 Java AST 分析器。需要更严格的跨语言或复杂语法精确分析时，应将工具结果与人工复核分开记录。

## 覆盖率与测试

后端 `backend/pom.xml` 配置 JaCoCo Maven Plugin 0.8.12。执行 `mvn clean test` 后，JaCoCo XML 位于 `backend/target/site/jacoco/jacoco.xml`，脚本读取该文件生成 `coverage_summary.csv`；若 XML 不存在，则明确写入 `NOT_RUN`。

Surefire XML 位于 `backend/target/surefire-reports`，脚本汇总为 `test_metrics.csv` 和 `test_summary.csv`。

## 输出

所有 CSV、证据和汇总文件写入当前目录 `S5/代码度量/`，包括类型、LOC、方法、圈复杂度、CK、覆盖率、测试、S4 对比、S0 偏差和超阈值清单。
