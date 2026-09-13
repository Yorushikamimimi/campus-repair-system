# Phase 1 业务开发报告

## 1 开发范围

本阶段在既有 `backend/`、`frontend/`、`database/` 初始化工程上增量开发，设计基线为 `S4/类图/图4-3_系统类图_改进后/S4_图4-3_系统类图_改进后.puml`。

已完成：

- 最终领域实体与 MyBatis-Plus 映射
- 用户登录、BCrypt 密码校验、JWT 与基础角色认证
- 报修类型/地点查询
- 报修工单创建、图片本地保存、本人列表、进度查询、待审核状态下内容修改
- 工单状态服务基础状态机与状态日志
- Repository 数据访问边界
- Phase 1 前端登录、布局、我的工单、新建报修、工单详情
- 单元测试、MockMvc 测试、构建和启动健康检查

未实现：管理员审核/派单完整页面与流程、维修处理、验收返修、评价、统计和后台完整页面。这些不属于本阶段交付范围。

## 2 实现的类

### 2.1 领域类

实现了 13 个领域实体类：

`AbstractAuditableEntity`、`SysUser`、`SysRole`、`SysUserRole`、`RepairOrder`、`RepairType`、`RepairLocation`、`RepairImage`、`DispatchRecord`、`RepairRecord`、`AcceptanceRecord`、`EvaluationRecord`、`OrderStatusLog`。

另有 `OrderStatus` 状态枚举，集中维护数据库状态代码与中文展示名的映射。

用户角色通过 `sys_user_role` 关联，`SysUser` 没有新增 `role_id` 字段。`RepairOrder` 继承 `AbstractAuditableEntity`，自身业务方法保留 `updateContent()` 和 `isOwnedBy()`；状态变更由 `OrderStateService` 负责。

S5 代码度量口径：`RepairCatalogService`、`RepairCatalogServiceImpl`、`RepairCatalogController`、`OrderStatus` 以及 `UserController` 新增的本人/指定用户角色查询入口，均属于实现阶段实际代码的一部分。后续统计类数量、WMC、RFC 等指标时，应以 S5 当前源码重新统计，不沿用 S4 设计阶段的类数量或度量值。

### 2.2 Service 类

- `UserPermissionService` / `UserPermissionServiceImpl`
- `RepairOrderService` / `RepairOrderServiceImpl`
- `RepairCatalogService` / `RepairCatalogServiceImpl`
- `QueryService` / `QueryServiceImpl`
- `OrderStateService` / `OrderStateServiceImpl`
- `FileStorageService` / `LocalFileStorageServiceImpl`

### 2.3 Controller 类

- `UserController`
- `RepairOrderController`
- `RepairCatalogController`
- 原有 `HealthController`

### 2.4 Repository 类

已按图4-3边界建立 10 个 Repository：

`UserPermissionRepository`、`RepairOrderRepository`、`RepairTypeRepository`、`RepairLocationRepository`、`RepairImageRepository`、`DispatchRepository`、`MaintenanceRepository`、`AcceptanceRepository`、`EvaluationRepository`、`OrderStatusRepository`。

其中本阶段重点可用的是用户权限、工单、报修类型、报修地点、报修图片和状态日志 Repository。其他后续业务 Repository 已建立正确的表级边界，但未扩展为 Phase 2 业务流程。

## 3 API清单

### 3.1 Phase 1 核心 API

| 方法 | 路径 | 权限 | 用途 |
|---|---|---|---|
| POST | `/api/auth/login` | 匿名 | 用户名、BCrypt 密码登录并返回 JWT |
| POST | `/api/repair-orders` | 已认证；本阶段以 REPORTER 验证 | 创建报修工单；支持 JSON 或 multipart/form-data，multipart 可带 `files` |
| GET | `/api/repair-orders/my` | 已认证；本阶段以 REPORTER 验证 | 查询当前登录用户自己的工单 |
| GET | `/api/repair-orders/{orderId}` | 已认证；本阶段以 REPORTER 验证 | 查询当前用户自己的工单进度 |
| PUT | `/api/repair-orders/{orderId}` | 已认证；本阶段以 REPORTER 验证 | 修改待审核工单的标题和描述 |
| GET | `/api/repair-types` | 已认证 | 查询启用中的报修类型 |
| GET | `/api/repair-locations` | 已认证 | 查询启用中的报修地点 |

### 3.2 用户权限 API

| 方法 | 路径 | 权限 | 用途 |
|---|---|---|---|
| GET | `/api/users` | ADMIN | 按角色和状态查询用户 |
| POST | `/api/users/{userId}/status` | ADMIN | 启用或禁用用户，参数 `status=0/1` |
| POST | `/api/users/{userId}/roles/{roleId}` | ADMIN | 分配用户角色 |
| GET | `/api/users/me/roles` | 已认证 | 查询当前用户角色 |
| GET | `/api/users/{userId}/roles` | ADMIN | 查询指定用户角色 |

按 Controller 处理入口计，本阶段共实现 13 个 API handler；其中 7 个为 Phase 1 核心业务接口，`POST /api/repair-orders` 的 JSON 和 multipart 是同一路径的两种请求体入口。

所有 Controller 返回 `ApiResponse`，业务异常由 `GlobalExceptionHandler` 统一处理；认证失败和权限不足由 Security 的统一 JSON 处理器返回同样的响应结构。

## 4 数据库访问

MyBatis-Plus 通过 `@MapperScan("com.campusrepair.repository")` 扫描 Repository。核心访问职责如下：

- `UserPermissionRepository`：用户、角色关联查询和用户状态更新
- `RepairOrderRepository`：`insertOrder`、`findById`、`findByReporter`、`updateOrder`、`findByFilters`
- `RepairTypeRepository`：按 ID 查询和查询启用类型
- `RepairLocationRepository`：按 ID 查询和查询启用地点
- `RepairImageRepository`：图片记录保存、按工单查询和按工单删除
- `OrderStatusRepository`：状态日志追加、按工单查询和最新日志查询

`RepairOrderRepository` 没有重新加入图片、类型或地点访问方法；这三类访问分别由拆分后的 Repository 承担。Controller 不直接访问数据库，类型/地点列表经 `RepairCatalogService` 编排。

数据库脚本静态检查得到 12 个 `CREATE TABLE` 定义，字段名未被 Java 实现改写。当前已执行 `schema.sql / seed.sql` 并通过 `SHOW TABLES` 验证：

- 表数量：12
- `SHOW TABLES` 包含 `sys_user`、`sys_role`、`sys_user_role`、`repair_order`、`repair_type`、`repair_location`、`repair_image`、`dispatch_record`、`repair_record`、`acceptance_record`、`evaluation_record`、`order_status_log`
- `sys_user` 不包含 `role_id`

`seed.sql` 仅包含基础字典数据：`sys_role`、`repair_type`、`repair_location`。  
`phase1_admin`、`phase1_reporter`、`phase1_maintainer`、`phase1_reporter_2` 为本地集成复验阶段补充插入的测试账号，密码均以 BCrypt hash 保存。  
本地 MySQL 连接与复验账号可用于真实登录验收；`campus_repair_app` 已可登录数据库，`seed_data=PASS`。

## 5 登录与JWT

登录真实调用链为：

`POST /api/auth/login` → `UserController` → `UserPermissionServiceImpl.login()` → `UserPermissionRepository.findUserByUsername()` → 检查用户存在和 `status=1` → `BCryptPasswordEncoder.matches()` → 查询 `sys_user_role` / `sys_role` → `JwtTokenProvider.generateToken(userId, roles)` → 返回 token。

JWT 中保存用户 ID 和角色编码。`JwtAuthenticationFilter` 验证签名和有效期后，将用户 ID 写入认证主体，将角色转为 `ROLE_REPORTER`、`ROLE_ADMIN`、`ROLE_MAINTAINER` 权限。工单 Controller 使用认证上下文中的用户 ID，不信任前端传入的 `reporterId`。

数据库种子脚本未包含测试账号，仅维护基础字典数据；本轮复验的测试账号为本地补充插入，密码使用 BCrypt hash 存储。

## 6 报修业务流程

### 6.1 创建工单

1. Controller 从 JWT 获取当前用户 ID，并进行请求字段校验。
2. `RepairOrderServiceImpl` 校验类型和地点存在且启用，校验标题/描述非空、长度合法。
3. 创建 `RepairOrder`，初始数据库状态为 `SUBMITTED`，其中文展示为“待审核”。
4. `RepairOrderRepository.insertOrder()` 保存工单。
5. 如有图片，`LocalFileStorageServiceImpl` 按工单 ID建立目录、随机文件名保存，并由 `RepairImageRepository` 保存图片记录。
6. 调用 `OrderStateService.submit()` 写入初始状态日志。
7. 方法使用 `@Transactional`；图片保存或状态日志失败时，数据库事务回滚，已写入的本地文件会执行清理。

### 6.2 查询和修改

- 我的工单只调用 `findByReporter(currentUserId)`。
- 工单详情先按 ID 查询，再校验工单归属；他人工单返回权限错误。
- 只有 `SUBMITTED`（待审核）状态允许修改标题和描述。
- `QueryService` 只提供 `queryOrderProgress()` 和 `queryOrders()`，不依赖 `MaintenanceRepository`，不提供 `queryRepairHistory()`。

## 7 状态流转

| 数据库代码 | 中文语义 | 当前状态服务支持 |
|---|---|---|
| `SUBMITTED` | 待审核 | `submit()` 初始提交 |
| `PENDING_PROCESS` | 待处理 | `approve()`；`dispatch()` 完成派单日志 |
| `PROCESSING` | 处理中 | `startRepair()` |
| `PENDING_ACCEPTANCE` | 待验收 | `finishRepair()` |
| `REWORK` | 待返修 | `reject()`；`reopen()` 后重新维修 |
| `COMPLETED` | 已完成 | `accept()` |

状态服务集中检查旧状态、目标状态和状态日志，不在工单实体或 Controller 中散落修改状态字符串。本阶段正式创建工单的状态为 `SUBMITTED` / “待审核”。

## 8 前端页面

已实现 Vue 3 + Element Plus 的最低可用页面：

- 登录页：用户名/密码校验、登录请求、错误提示和 token 保存
- 基础布局：导航、我的工单、新建报修、退出登录
- 我的报修列表：工单号、标题、状态、提交时间和详情跳转
- 新建报修：类型、地点、标题、描述、图片多选和 multipart 提交
- 工单详情/进度：工单信息、中文状态、提交/更新时间
- Axios 请求拦截器：自动附加 Bearer token，401 清理 token 并回到登录页
- Vue Router 登录拦截：未登录不能访问业务页面

未加入复杂视觉效果和 Phase 2 管理后台页面。

## 9 测试结果

`mvn clean test` 实际结果：24 个测试，Failures=0，Errors=0，Skipped=0。

测试类及有效用例（按真实 surefire 报表）：

- `com.campusrepair.controller.RepairOrderControllerTest`：2
- `com.campusrepair.controller.UserControllerTest`：3
- `com.campusrepair.service.OrderStateServiceImplTest`：2
- `com.campusrepair.service.QueryServiceImplTest`：2
- `com.campusrepair.service.RepairOrderServiceImplTest`：8
- `com.campusrepair.service.UserPermissionServiceImplTest`：5
- `com.campusrepair.storage.LocalFileStorageServiceImplTest`：2

合计 24 个测试，全部在本地 `backend/target/surefire-reports/` 可追溯。

这些测试使用 Mock、MockMvc 或临时目录，没有把它们标记为 MySQL 集成测试。

## 10 构建结果

### 后端

- `mvn clean test`：PASS
- Tests run：24
- Failures：0
- Errors：0
- Skipped：0
- 编译目标：Java 17（`mvn` 编译时 `release` 为 17；当前环境 `java` 与 `mvn` 实际运行时为 21.0.2）

### 启动和健康检查

- `mvn spring-boot:run`：PASS（2026-09-13 本地实测）
- `GET http://localhost:8080/api/health`：HTTP 200，返回 `code=200`
- 未带 token 请求受保护工单接口：HTTP 401，返回统一 `ApiResponse`

### 前端

- `npm install`：PASS
- `npm run build`：PASS
- Vite 构建有 Element Plus 体积提示，但没有构建失败

## 11 与S4设计一致性

- `QueryService` 没有 `queryRepairHistory()`；维修历史职责未被 QueryServiceImpl 接管。
- `QueryServiceImpl` 只依赖 `RepairOrderRepository`，没有依赖 `MaintenanceRepository`。
- `RepairOrderRepository` 已完成类型、地点、图片职责拆分。
- `RepairOrder` 没有 `changeStatus()` 或 `canOperate()`；状态统一由 `OrderStateServiceImpl` 处理。
- `EvaluationRecord` 和 `EvaluationRepository` 保持独立；本阶段未实现评价业务，因此不存在 `AcceptanceService` 承担评价的实现。
- `sys_user` 没有 `role_id`，角色只通过 `sys_user_role` 关联。
- 未修改 `S4` 图件、S0-S4 文档和任何 DOCX 文件。

实现层适配说明：

1. 初始化骨架已经将 JWT 放在 `security` 包，本阶段沿用该位置完善，而没有改变职责边界；图4-3中的 `infrastructure` 包仅作为设计参考。
2. 图4-3中上传参数写作 `List<String>`，实际 Controller 使用 `List<MultipartFile>`，因为本阶段需要真实接收 multipart 文件并保存到本地；Service 的业务职责和 FileStorage 边界未改变。
3. 数据库中的 `repair_order.status` 为 `VARCHAR(32)`，默认值为 `SUBMITTED`；Java 与 SQL 均使用状态字符串保持一致，不是 `TINYINT` / `Integer` 映射。
4. 冻结表使用 `repair_order.submit_time`，`RepairOrder` 将其作为领域创建/提交时间映射，没有修改 SQL 字段名。

以上是实现层适配，没有发生图4-3所禁止的职责合并或边界回退。

S5 代码度量尚未在本报告中执行；待度量阶段应基于实际源码、实际新增类和实际方法重新生成指标结果。

## 12 未完成事项

- 管理员审核和派单的完整业务接口及页面
- 维修人员任务、维修记录和维修历史业务
- 报修人验收、返修和评价业务
- 统计服务和后台完整页面
- MySQL 真实建库、`schema.sql/seed.sql` 执行及 `SHOW TABLES` 复核：已完成
- 使用 JDK 21 运行时启动与数据库连接、登录到接口复验已完成；复验中的异常点已修复并已回归。项目编译目标仍为 `release=17`（Maven Compiler）。

## 13 阻塞项

- Git：当前目录不是 Git 仓库；已尝试 `git rev-parse --show-toplevel` 失败；`git ls-remote https://github.com/Yorushikamimimimi/campus-repair-system` 返回 `Repository not found`。
- MySQL：`mysql 8.0.46`、`mysqladmin 8.0.46`，3306 监听正常，`SHOW TABLES` 与表计数通过；未做远端拉取/推送。
- 登录与运行时：已在 JDK 21 运行时完成 `mvn clean test`、`mvn spring-boot:run` 与健康检查，服务运行口令均通过。

## 14 Phase 1 真实集成复验（补充）

本轮新增复验结论：

- 成功：服务健康检查、LOGIN、报修类型、报修地点、创建无图工单、我的工单、工单详情、匿名401、跨账号拒绝（403）、图片上传、管理员列表查询、管理员角色登录、维修人员角色查询。
- 成功：`PUT /api/repair-orders/{orderId}` 返回 200 并真实更新数据库；`REPORTER` 身份访问 `/api/users` 返回 403。
