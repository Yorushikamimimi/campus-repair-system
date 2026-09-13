# 数据库初始化

数据库为 MySQL 8.0，先执行 `schema.sql`，再执行 `seed.sql`。脚本依据 S3 ER 图建立 12 张表。当前环境未检测到可用 MySQL 服务，因此尚未执行真实建库；具备数据库凭据后请复验 `SHOW TABLES`。
