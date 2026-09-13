# Phase 3 验收返修评价统计开发报告

## 1 阶段范围与基线

本阶段接管已有 Phase 3 实现，执行“代码审计 → 后端回归 → 真实 MySQL/HTTP 验收 → 前端构建检查 → 证据收口”。未重写既有 Phase 3 业务类，未新增数据库表、字段或业务状态，S4 设计与 DOCX 未修改。

冻结状态集合仍为：

`SUBMITTED`、`PENDING_PROCESS`、`PROCESSING`、`PENDING_ACCEPTANCE`、`REWORK`、`COMPLETED`。

本轮环境实测：Java 编译目标为 17，运行时为 OpenJDK 21.0.2；MySQL 客户端/服务版本为 8.0.46；数据库为 `campus_repair`，实际表数量为 12。为避开旧 8080 实例，当前源码在 8090 启动并完成 HTTP 验收。

## 2 Phase 3 现有实现审计

已确认以下代码真实存在并被当前控制器调用：

- `backend/src/main/java/com/campusrepair/service/AcceptanceService.java`
- `backend/src/main/java/com/campusrepair/service/impl/AcceptanceServiceImpl.java`
- `backend/src/main/java/com/campusrepair/service/EvaluationService.java`
- `backend/src/main/java/com/campusrepair/service/impl/EvaluationServiceImpl.java`
- `backend/src/main/java/com/campusrepair/service/StatisticsService.java`
- `backend/src/main/java/com/campusrepair/service/impl/StatisticsServiceImpl.java`
- `backend/src/main/java/com/campusrepair/controller/AcceptanceController.java`
- `backend/src/main/java/com/campusrepair/controller/QueryStatisticsController.java`
- `backend/src/main/java/com/campusrepair/repository/AcceptanceRepository.java`
- `backend/src/main/java/com/campusrepair/repository/EvaluationRepository.java`

职责边界保持为：AcceptanceService 负责验收/返修/验收历史，EvaluationService 负责评价，StatisticsService 负责统计；维修历史仍由 MaintenanceService 提供。

## 3 后端测试回归

执行命令：`cd backend && mvn clean test`

本次真实结果：

- `Tests run = 89`
- `Failures = 0`
- `Errors = 0`
- `Skipped = 0`
- `BUILD SUCCESS`

当前实际测试数量以本次 Surefire/Maven 输出为准；此前 Phase 2 证据中的 `88` 已不是当前源码的实际数量，不沿用旧数字作为本阶段实测值。

Phase 3 重点测试类均已存在并通过：`AcceptanceServiceImplTest` 9、`EvaluationServiceImplTest` 9、`StatisticsServiceImplTest` 6、`AcceptanceControllerTest` 7、`QueryStatisticsControllerTest` 7。

## 4 真实 HTTP + MySQL 正常验收闭环

测试服务：`http://127.0.0.1:8090`；真实账号对应数据库用户 ID：REPORTER=7、REPORTER_2=10、ADMIN=8、MAINTAINER=9。因项目材料只保存 BCrypt hash、未保存明文密码，本轮未修改数据库密码，采用当前应用 JWT 密钥为真实数据库用户签发测试 token，验证真实 Spring Boot、真实 MySQL、真实角色拦截和业务落库。

正常工单 `order_id=33` 实际链路：

`REPORTER 创建 → ADMIN 审核通过 → ADMIN 派单 → MAINTAINER 接单 → PROCESSING → 完成维修 → PENDING_ACCEPTANCE → REPORTER 验收通过 → COMPLETED → REPORTER 评价`

HTTP 结果：创建、审核、派单、接单、完成、验收、评价均为 HTTP 200；评价记录 ID 为 4。

MySQL 实际核验：

- `repair_order.status = COMPLETED`
- `dispatch_record` 1 条
- `repair_record` 1 条
- `acceptance_record` 1 条
- `evaluation_record` 1 条
- `order_status_log` 6 条
- 状态日志包含 `SUBMITTED -> PENDING_PROCESS`、`PENDING_PROCESS -> PROCESSING`、`PROCESSING -> PENDING_ACCEPTANCE`、`PENDING_ACCEPTANCE -> COMPLETED`

## 5 真实 HTTP + MySQL 返修闭环

返修工单 `order_id=34` 实际链路：

`REPORTER 创建 → ADMIN 审核/派单 → MAINTAINER 接单 → 完成维修 → PENDING_ACCEPTANCE → 验收失败 → REWORK → 继续维修 → PROCESSING → 第二次完成 → PENDING_ACCEPTANCE → 验收通过 → COMPLETED → 评价`

MySQL 实际核验：

- `repair_order.status = COMPLETED`
- `dispatch_record` 1 条
- `repair_record` 2 条，第一轮历史未被覆盖
- `acceptance_record` 2 条，包含失败和通过两次验收
- `evaluation_record` 1 条
- `order_status_log` 9 条
- 状态日志包含 `PENDING_ACCEPTANCE -> REWORK`、`REWORK -> PROCESSING`、`PROCESSING -> PENDING_ACCEPTANCE`、`PENDING_ACCEPTANCE -> COMPLETED`

## 6 评价规则与权限验收

针对真实完成工单 33：

- 本人评分 1～5：HTTP 200，真实写入 `evaluation_record`
- 评分 0：HTTP 400
- 评分 6：HTTP 400
- 重复评价：HTTP 409，提示工单已评价
- 其他 REPORTER（用户 10）评价他人工单：HTTP 403

针对返修工单 34 在首次 `PENDING_ACCEPTANCE` 阶段评价：HTTP 409，提示仅已完成工单可评价。

验收历史权限：其他 REPORTER 访问工单 33 的验收历史 HTTP 403；MAINTAINER 访问验收历史 HTTP 403。

UserController 角色入口真实 HTTP 结果：

- `GET /api/users/me/roles`（本人 REPORTER）：HTTP 200，返回 `REPORTER`
- `GET /api/users/7/roles`（ADMIN 查询指定用户）：HTTP 200，返回 `REPORTER`
- REPORTER 查询指定用户角色：HTTP 403

## 7 统计接口验收

管理员统计接口均真实返回 HTTP 200：

- `/api/admin/statistics/status`：返回各冻结状态计数，当前实测包含 `SUBMITTED=19`、`PENDING_PROCESS=0`、`PROCESSING=2`、`PENDING_ACCEPTANCE=5`、`REWORK=3`、`COMPLETED=5`
- `/api/admin/statistics/type`：返回 2 个报修类型统计项
- `/api/admin/statistics/period?start=2026-01-01&end=2026-12-31`：返回 34
- `/api/admin/statistics/repair-duration`：返回 `0.0` 分钟；真实维修操作在同一秒内完成，因此分钟差为 0。实现按 `start_time/end_time` 计算，并跳过空时间记录

统计权限：匿名访问 HTTP 401；REPORTER HTTP 403；MAINTAINER HTTP 403。

## 8 控制流复杂度口径

按实际源码单方法方法体计算，不把被调用私有方法内部判断外溢计入：

- `MaintenanceServiceImpl.processRepairOrder`：`V(G)=3`（两个直接判断）
- `AcceptanceServiceImpl.acceptRepairResult`：`V(G)=2`（一个直接判断）
- `EvaluationServiceImpl.evaluateService`：`V(G)=4`（三个直接判断）

均未超过本阶段要求的单方法复杂度上限 10。该结果为 S5 实际代码口径，不沿用 S4 设计阶段数字。

## 9 前端审计与构建

已核对现有页面职责：

- `frontend/src/views/OrderDetail.vue`：报修人验收通过、验收失败返修、验收历史、已完成评价
- `frontend/src/views/MaintenanceTasks.vue`：维修人员接单、处理结果、返修继续维修、维修历史
- `frontend/src/views/AdminStatistics.vue`：按状态、类型、日期范围、维修时长统计

审计发现并最小修复了 `OrderDetail.vue` 评价表单模板的两个 ref 绑定错误：将 `evaluateForm.value.score/comment` 改为 Vue 模板正确的 `evaluateForm.score/comment`。后端职责与 API 未改变。

执行命令：`cd frontend && npm run build`

结果：`PASS`，Vite 成功产出构建文件。仅保留既有大 chunk warning，不影响构建成功。

## 10 收口边界与未验证事项

- 本轮未修改 S4 设计文件、DOCX、数据库 schema 和冻结业务状态。
- 本轮未启动 S5 最终 LOC/WMC/RFC 等整体代码度量；后续度量必须按当前实际代码重新统计，特别包含实现层新增的 RepairCatalogService/Impl/Controller、OrderStatus 以及 UserController 角色入口。
- 当前工作目录未检测到 Git 元数据，无法提供 Git diff、分支或提交 hash；本轮实际代码写入为 `frontend/src/views/OrderDetail.vue` 两处模板绑定修复，以及本报告和 `phase3_validation.txt` 两个证据文件。
- 本轮未用明文密码重新执行 `/api/auth/login`；Phase1 已有登录证据仍需在已知明文密码条件下另行复验。业务验收使用了应用同密钥签发的真实用户 JWT，已验证真实角色和业务流程，但不把它扩大表述为登录密码复验。
- 本轮未做浏览器视觉操作验收；已完成源代码页面审计和生产构建验收。

## 11 结论

Phase 3 正常验收、失败返修再验收、评价规则、角色权限、统计接口和真实数据库落库均已完成现场验收；前端评价表单绑定缺口已做最小修复，前端构建通过。Phase 3 可按本报告证据收口，但不应把本轮结果当作 S5 最终代码度量报告。
