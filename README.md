# SmartBI Clone

Vue 3 + Spring Boot 数据查询与电子表格报表闭环：平台元数据与外部 PostgreSQL 严格分离，SQL 数据集始终在绑定数据源上执行。

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
- `POST/GET/PUT /api/v1/report-templates`：保存、列出、重新打开和更新电子表格模板
- `POST /api/v1/report-templates/{id}/render`：使用参数在服务端确定性渲染模板

数据源密码仅通过服务端环境变量解析，例如 `env://EXTERNAL_ANALYTICS_PASSWORD`，且变量名还必须命中 `SMARTBI_ALLOWED_CREDENTIALS`；响应、异常和审计日志均不包含密码、凭据引用或凭据值。Compose 会启动独立的 `analytics-db` 并创建只读 `analytics_reader` 演示账号。

预览请求示例：

```json
{"queryId":"d45258bf-7c55-4f0f-aa9e-e0644cd1ab08","parameters":{"minimum":100},"page":0,"size":100}
```

安全边界：空或未知 API key 默认拒绝；`ADMIN` 可跨所有者访问，`EDITOR` 可创建资源，其余身份只读。非管理员的数据源、数据集、预览、连接测试与查询取消均按持久化所有者隔离。

报表设计器真实呈现并保存单元格样式、合并、行高、列宽和数据集字段绑定。显式 `regionId` 将同一数据集的多个字段组成统一扩展区域：每区域只查询一次并按同一结果行对齐；旧模板会按数据集、方向和锚点自动分组。服务端先确定性插入行列并平移静态内容、尺寸和合并区域，再计算 `=A1+B1` 与基础 `=SUM(A1:B9)`，引用、值、语法和循环错误会返回明确错误值。布局冲突返回 422。完整兼容与坐标规则见 [docs/spreadsheet-report-mvp.md](docs/spreadsheet-report-mvp.md)。

PostgreSQL JDBC URL 必须是规范的单主机、单端口 URI，主机和端口必须命中 `SMARTBI_ALLOWED_DB_HOSTS` / `SMARTBI_ALLOWED_DB_PORTS`。拒绝多主机、user-info、fragment、全部连接参数、编码绕过、其他协议及非允许目标；建池前重新解析 DNS，并拒绝 unspecified、link-local 与 multicast 地址。DNS 可能在校验后变化，因此生产部署仍须用防火墙/NetworkPolicy 把 API 的数据库出口限制到批准的 IP/CIDR 和端口，不能把应用层 URL 校验当作完整 SSRF 隔离。Compose 的 `analytics-db` 是 CI/演示用允许目标。

SQL 必须由 AST 解析为单条 `SELECT/WITH`；结构遍历拒绝写入型 CTE、`SELECT INTO`、行锁子句、堆叠语句和已知 PostgreSQL 危险函数，不再用关键词黑名单误伤名为 `update`/`delete` 的合法字段。命名参数词法器理解 `::` cast、单双引号、转义、美元引用和注释，拒绝缺失、额外及复杂对象参数，值始终通过 JDBC 绑定。静态检查不可能证明任意内置、扩展或用户自定义函数无副作用：生产必须撤销写权限和危险函数执行权限，使用最小权限只读账号。执行时同时启用 JDBC read-only、显式只读事务、PostgreSQL `default_transaction_read_only=on`、超时、取消、分页、500 行总上限，并在连接归还池前显式回滚。

## 已知剩余风险 / 合并边界

- 本基线面向开发与演示；API key + `ADMIN/EDITOR/VIEWER` 不是生产级 IAM/RBAC，尚无租户组织模型、密钥轮换或 Vault 集成。
- DNS 防护存在解析后变化（rebind/TOCTOU）窗口，必须依赖部署层数据库出口 allow-list；TLS、证书校验与目标数据库身份绑定需在生产配置完成。
- SQL AST 与危险函数集合是纵深防御，不是数据库沙箱；数据库角色权限、函数 `EXECUTE` 权限和只读事务才是最终边界。
- 当前仅实现 PostgreSQL；GaussDB/MySQL/Oracle 只有类型占位，未实现或验证方言与驱动。
- API key 保存在浏览器 `sessionStorage`，仅适合本地演示；生产前应改为正式登录会话并配套 CSP/XSS 防护。

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

CI 还会实际验证：未认证默认拒绝、多主机 JDBC URL 拒绝、写入型 CTE 拒绝、只读数据库角色无法建表、查询闭环、统一报表扩展与布局后公式可用，以及 API 日志不出现演示密码。测试证据以 PR checks 的不可变日志和提交内 `docs/security-test-evidence.md` 为准。

