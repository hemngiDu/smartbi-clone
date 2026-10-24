# 第三轮安全整改测试证据

记录日期：2026-10-24（Asia/Shanghai）

## 本地已执行

| 命令 | 结果 |
| --- | --- |
| `npm run test:web` | 通过：1 个测试文件、1 个测试 |
| `npm run build:web` | 通过：TypeScript 检查及 Vite production build |
| `mvn -B -f apps/api/pom.xml test`（Temurin 21.0.12.1） | 通过：14 个测试，0 失败、0 错误 |

## CI 必须执行

GitHub Actions 使用 Temurin 21、Node 22 和 Docker Compose，运行 `mvn -B test`、前端测试/构建及真实 PostgreSQL 查询闭环。Compose smoke test 另外断言未认证为 401、多主机 JDBC URL 与写入型 CTE 为 400、`analytics_reader` 无法 `CREATE TABLE`、失败写入未留下对象、API 日志不包含演示密码。

本机系统 Maven 默认绑定 Java 8，已在仓库忽略目录临时下载 Temurin 21 并完成后端测试；本机没有 Docker CLI，因此 Compose 结果不得伪报为本地通过，推送后以 PR #1 的 Actions 日志作为最终、可复核证据。
