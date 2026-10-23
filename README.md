# SmartBI Clone

Vue 3 + Spring Boot 数据源查询闭环：平台元数据与外部 PostgreSQL 严格分离，SQL 数据集始终在绑定数据源上执行。

## 一键启动

要求：Docker Desktop（Compose v2）。

```bash
docker compose up --build
```

- Web: http://localhost:5173
- API 健康检查: http://localhost:8080/actuator/health
- OpenAPI: http://localhost:8080/swagger-ui.html

## 本地开发

前端需要 Node.js 20+：

```bash
npm install
npm run dev:web
```

后端需要 JDK 21 和 Maven 3.9+：

```bash
cd apps/api
mvn spring-boot:run
```

## API

所有 `/api/**` 请求都必须携带 `X-API-Key`。本地 Compose 演示值见 `.env.example`；生产环境必须从部署密钥管理注入强密钥。

- `POST /api/v1/data-sources`：登记 JDBC 数据源元数据（只保存密钥引用，不接收明文密码）
- `GET /api/v1/data-sources`：数据源列表
- `POST /api/v1/data-sources/{id}/test`：使用临时只读连接测试数据源
- `POST /api/v1/datasets`：创建只读 SQL 数据集
- `GET /api/v1/datasets`：数据集列表
- `POST /api/v1/datasets/{id}/preview`：参数化分页预览，最多返回 500 行
- `DELETE /api/v1/queries/{queryId}`：取消正在运行的查询（queryId 由客户端预先生成）

数据源密码仅通过服务端环境变量解析，例如 `env://EXTERNAL_ANALYTICS_PASSWORD`；响应、异常和审计日志均不包含密码或凭据值。Compose 会启动独立的 `analytics-db` 并创建只读 `analytics_reader` 演示账号。

预览请求示例：

```json
{"queryId":"d45258bf-7c55-4f0f-aa9e-e0644cd1ab08","parameters":{"minimum":100},"page":0,"size":100}
```

安全边界：空或未知 API key 默认拒绝；`ADMIN` 可跨所有者访问，`EDITOR` 可创建资源，其余身份只读。非管理员的数据源、数据集、预览、连接测试与查询取消均按持久化所有者隔离。

PostgreSQL JDBC URL 必须是规范 URI，主机和端口必须命中 `SMARTBI_ALLOWED_DB_HOSTS` / `SMARTBI_ALLOWED_DB_PORTS`。拒绝 user-info、fragment、连接参数、其他协议及非允许目标；Compose 只允许隔离网络内的 `analytics-db`。

SQL 必须解析为单条 `SELECT/WITH`；词法层拒绝写语句、写入型 CTE、`SELECT INTO`、堆叠语句和已知 PostgreSQL 副作用函数。命名参数词法器理解 `::` cast、单双引号、转义、美元引用和注释，值始终通过 JDBC 绑定。这是纵深防御而非对任意数据库函数纯度的形式化证明：生产仍必须使用最小权限只读账号。执行时还会设置只读事务、PostgreSQL `default_transaction_read_only=on`、30 秒超时、取消、分页和 500 行总上限。GaussDB/MySQL/Oracle 仅保留接口，尚未实现驱动及方言。

## 验收命令

```bash
cd apps/api && mvn test
npm test
npm run build:web
docker compose up --build -d
docker compose ps
curl http://localhost:8080/actuator/health
docker compose down -v
```

