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

安全边界：只接受可解析为单条 `SELECT/WITH` 的 SQL；禁止注释和分号；所有值使用 JDBC 参数绑定；外部连接和事务均设为只读，并启用 30 秒超时、取消、分页和 500 行总上限。GaussDB/MySQL/Oracle 已保留类型与适配器接口，但尚未实现驱动及方言。

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

