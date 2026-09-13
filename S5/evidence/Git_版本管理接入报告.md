# Git版本管理接入报告

## 1 接入前状态

接入前工作目录为 `/Users/yang/Workspace/school/软件度量大作业`，`git rev-parse --show-toplevel` 返回“not a git repository”，目录不存在 `.git`。项目资料、后端源码、前端源码和 Phase 1～3 证据均保留在原目录中。

## 2 正式远端

正式远端：`https://github.com/Yorushikamimi/campus-repair-system`

接入前通过 `git ls-remote` 访问成功且没有返回任何 ref，确认该远端是可访问的空仓库。GitHub 身份检查显示当前登录账号为 `Yorushikamimi`，具备 `repo` 权限。

## 3 接入策略

由于远端为空仓库，没有执行相邻目录 clone 和远端内容覆盖操作；在当前完整项目目录执行 `git init -b main`，添加唯一正确的 `origin`，审计后建立首次完整基线。

首次基线提交：

- Commit：`d695d26`
- Message：`feat: 建立校园报修系统初始基线`
- 提交文件：201 个正式项目文件

S0～S9 课程资料、数据库 schema/seed、后端 pom、前端 package/package-lock 和 README 均纳入版本管理。

## 4 .gitignore

根目录 `.gitignore` 已补齐以下排除项：

- Java/Maven：`backend/target/`、`backend/.m2repo/`、`*.class`、`*.log`
- Node/Vue：`frontend/node_modules/`、`frontend/dist/`
- IDE/系统：`.idea/`、`.vscode/`、`.DS_Store`
- 环境与密钥：`.env`、`.env.*`、`application-local.yml`，保留 `.env.example`
- 运行时和临时文件：`uploads/`、`backend/uploads/`、`*.tmp`、`*.swp`、`*.bak`、`~$*`

课程正式资料、`database/schema.sql`、`database/seed.sql`、后端 `pom.xml`、前端 `package.json`/`package-lock.json` 未被忽略。

## 5 敏感信息检查

扫描发现 `backend/.env.local` 含本地数据库密码、JWT secret 和测试账号密码；该文件已被 `.env.*` 忽略，没有进入 Git index，也没有推送到远端。

已创建安全占位文件：[backend/.env.example](/Users/yang/Workspace/school/软件度量大作业/backend/.env.example)，只包含变量名和占位值。未提交真实数据库密码、JWT secret、明文密码、Access Token、完整 JWT 或私钥。

## 6 构建验证

接入前使用当前代码重新验证：

- `cd backend && mvn clean test`：PASS
- `Tests run = 89`，`Failures = 0`，`Errors = 0`，`Skipped = 0`
- `cd frontend && npm run build`：PASS

构建产生的 `backend/target/`、`frontend/dist/` 和依赖缓存未进入 Git。

## 7 首次提交

首次基线提交已完成：

`d695d26 feat: 建立校园报修系统初始基线`

该提交建立了项目正式版本管理起点，未使用 force push，也未覆盖或删除远端历史。

## 8 分支

- `main`：Phase 1～3 已验收的稳定基线
- `dev`：日常集成开发分支
- `feature/*`：后续具体功能或度量任务分支

当前分支为 `dev`。

## 9 远端推送

- `origin/main`：PASS，已推送首次基线 `d695d26`
- `origin/dev`：PASS，已推送并与当前基线一致
- 未使用 `git push --force`

## 10 最终状态

最终工作区在 Git 接入报告提交后应保持 clean；本任务不修改 S4 正式内容、S4 DOCX、数据库 schema、业务状态或 Phase 1～3 业务逻辑，也不开始 S5 最终源码度量。

Git 接入机器验收明细见：[git_validation.txt](/Users/yang/Workspace/school/软件度量大作业/S5/evidence/git_validation.txt)。
