# SmartBI Clone

首个可执行里程碑：Vue 3 + TypeScript 前端、Java 21 + Spring Boot API、PostgreSQL、Flyway 数据库迁移、数据源/SQL 数据集元数据与安全预览 API。

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
- `POST /api/v1/datasets`：创建只读 SQL 数据集
- `GET /api/v1/datasets`：数据集列表
- `POST /api/v1/datasets/{id}/preview`：预览查询，最多返回 500 行

当前预览执行使用平台 PostgreSQL 连接。外部数据源连接池、凭据加密、权限和租户隔离属于后续里程碑。

