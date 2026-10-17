# TripStar Java 后端

基于 Spring Boot 2.6.13、Java 8、MyBatis、MySQL 的旅行规划与行程问答模块。
保留现有 9060 端口和数据库连接配置，数据库密码通过环境变量提供。项目可直接在 IDEA 中重新加载 Maven 后运行 `TripStarApplication`。

## 配置和启动

在 IDEA 的 Run/Debug Configuration → Environment variables 中设置：

```text
DB_PASSWORD=你的数据库密码
LLM_API_KEY=你的模型密钥
LLM_BASE_URL=https://api.deepseek.com/v1
LLM_MODEL_ID=deepseek-chat
```

`DB_PASSWORD` 为必填项，`application.yaml` 不保存真实数据库密码。请勿将真实密码或密钥提交到 Git；`.env` 和 `.env.*` 已忽略，但 Spring Boot 不会自动加载它们，需通过 IDE 或启动进程设置环境变量。

接口采用兼容 Chat Completions 的 HTTP 协议；`LLM_BASE_URL` 填到 `/v1` 层级，程序追加 `/chat/completions`。
默认供应商为 DeepSeek，可替换成兼容供应商。未配置密钥仍可启动，规划任务会明确失败并返回 `AI_NOT_CONFIGURED`。
真实生成和聊天需要自行提供有效密钥。规划输出默认上限为 8192 tokens，可通过 `LLM_PLAN_MAX_TOKENS` 按供应商能力调整；内容截断会明确返回失败。

数据库表与提供的 `MYSQL_SCHEMA.sql` 一致，副本位于 `src/main/resources/db/schema.sql`。
**启动时不会执行建表或修改表结构**；如果数据库中已存在这七张表，无需再次执行脚本。
MySQL 需为 8.0.16+。每个连接的会话时区设置为 UTC，DATETIME 按 UTC 解释；展示层自行转换为 Asia/Shanghai。
保留已有连接 URL、用户名及端口；数据库密码改为从 `DB_PASSWORD` 环境变量读取。

Maven 命令：

```bash
mvn test
mvn spring-boot:run
```

本次验证采用临时依赖缓存，可重跑：

```bash
mvn -Dmaven.repo.local=/tmp/tripstar-m2 test
```

## MVC 分层

```text
com.josie.tripstar
├── config                 # 模型配置、线程池、MVC 配置
├── controller             # 规划、聊天、偏好接口和异常处理
├── mapper                 # MyBatis 数据访问接口
├── entity                 # 七张表的实体
│   ├── enums              # 任务状态（Java 包名使用小写）
│   ├── result             # 通用响应
│   └── vo                 # 接口参数及结构化行程
├── interceptor            # 匿名用户标识校验
├── service                # 仅暴露业务接口
│   └── impl               # 规划、问答、模型调用和记忆业务的实现
└── utils                  # JSON 转换、模型消息组装和记忆权重算法
resources/mapper           # MyBatis XML
```

Controller 依赖 Service 接口；service.impl 中的实现类负责业务与事务；Mapper + XML 负责 SQL。
未添加不需要的工具类目录或空实现。保留原始 demos 示例；新增业务都在 `/api` 下。

## 接口与调用顺序

所有接口带 `X-User-Id` 请求头，值为客户端生成并持久化的 UUID 或 1～64 位字母、数字、下划线、连字符。
沿用参考项目的匿名身份模式，**该标识不是登录鉴权凭证**。接入账号登录时，应替换 `UserInterceptor` 中的身份来源。
跨用户的计划、任务和会话查询统一返回 404。
请求与响应的业务字段使用 snake_case。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| POST | `/api/trip/plan` | 提交规划，返回 202 与 task_id、plan_id |
| GET | `/api/trip/status/{task_id}` | 轮询状态；完成时 result 包含具体版本和行程 |
| GET | `/api/trip/history?limit=10` | 当前用户最近创建的已完成计划，含最新版本摘要 |
| GET | `/api/trip/plans/{plan_id}/versions` | 最近 100 个不可变版本摘要 |
| GET | `/api/trip/plans/{plan_id}/versions/{version_no}` | 读取具体行程版本 |
| POST | `/api/chat/sessions` | 以 plan_id、version_no 创建会话 |
| POST | `/api/chat/ask` | 以 session_id、message 提问，返回 reply |
| GET | `/api/chat/sessions/{session_id}/messages?after=0&limit=50` | 按序号分页读取历史消息 |
| POST | `/api/memory` | 手动保存 `{ "content": "偏爱博物馆" }` |
| GET | `/api/memory` | 返回衰减后仍有效的 Top-10 偏好 |

IDEA HTTP Client 的完整请求示例见 `docs/tripstar.http`，包含响应变量提取。

规划请求示例：

```json
{
  "idempotency_key": "beijing-20261101-v1",
  "city": "北京",
  "start_date": "2026-11-01",
  "end_date": "2026-11-03",
  "travel_days": 3,
  "transportation": "公共交通",
  "accommodation": "经济型酒店",
  "preferences": ["历史文化", "美食"],
  "free_text_input": "不安排过于紧凑的行程",
  "language": "zh"
}
```

多城市可传 `cities: [{ "city": "北京", "days": 2 }, { "city": "西安", "days": 1 }]`。
总天数必须为 1～30，包含首尾日期；各城市天数之和必须等于 travel_days。

第一次提交未带 plan_id 时创建新计划。拿到响应中的 plan_id 后，重试或再次生成时应带上它。
幂等键作用域是 **同一个 plan_id**：同键同内容返回原任务，同键不同内容返回 409；不同键生成新版本。
未带 plan_id 的两次请求会创建两个计划，不能用幂等键跨计划去重；首次响应丢失时，可通过历史记录找回成功计划。
失败后使用已有 plan_id 和新幂等键重试，原失败任务会保留。

任务状态为 processing / completed / failed。轮询间隔建议 1～2 秒，到终态后停止。
不返回虚假成功数据。模型输出需通过日期、城市分配、必填字段、三餐、金额和预算汇总校验。
预算统一 CNY；景点、酒店和餐费分类合计必须与每日明细一致，五类费用之和必须与 total 一致。

会话绑定创建时选定的版本，后来生成新版本不会改变旧会话上下文。
模型失败不会写入半轮消息；并发提问时，后保存的旧上下文请求返回 409，应刷新历史后重新发送。
服务端最多注入最近 10 轮、24000 字符历史，完整消息仍保存在数据库中。
聊天只回答和建议，不自动改写行程；重新规划通过新的规划任务完成。

错误响应示例（HTTP 状态码区分 400/404/409/502/503/500）：

```json
{ "success": false, "message": "模型服务请求失败或超时，请稍后重试", "error_code": "AI_UNAVAILABLE" }
```

## 记忆模块

直接参考上游独立 memory 源码迁移到 `service/impl`，未引入 hello_agents 或其他 Agent 框架。
来源及上游许可见 `THIRD_PARTY_NOTICES.md`。

- 对 trim 后 UTF-8 文本计算 SHA-256，数据库唯一键按用户去重。
- 每日权重衰减系数 0.97；低于 2 的记忆不召回；按权重取 Top-10，每条不超过 120 字符。
- 初始准入分数为 4；同内容合并增加 `新权重 × 0.4`，最高 10。
- 手动显式偏好初始权重为 8，隐式偏好不能覆盖其 explicit 来源。
- 先衰减再合并，使用独立 last_decay_at 和乐观锁避免重复衰减或并发覆盖。
- 低权重记录仅过滤，不自动删除用户数据。
- `ENABLE_USER_MEMORY=false` 可关闭读写；此时记忆接口返回空列表/成功空操作。
- `MEMORY_EXTRACT_ENABLED=true` 可启用规划成功后的长期偏好提取，默认关闭以避免额外模型调用。
  提取仅根据用户需求，不从模型自行推荐的行程中推断用户喜好；提取失败不影响已完成的规划。

## 任务执行与当前范围

规划采用两线程、有界队列（20 个排队任务）。提交事务成功后才派发后台执行；队列满会保存为失败任务。
模型调用不占用数据库事务；完成时锁定计划分配版本号，并在同一事务写入行程版本和完成状态。
定时任务每分钟检查中断任务：开始执行超过 10 分钟或排队超过 1 小时则置失败；晚返回结果不能覆盖终态。
当前没有持久化消息队列，进程重启后中断任务按上述时限失败，不自动重放。

本阶段完成后端 chat、trip 和所需 memory。没有接入多智能体、实时地图/酒店/天气、小红书、知识图谱、WebSocket、前端页面或账号登录。
因此行程属于模型建议，费用是估算，weather_info 为空，不生成未经查询的坐标或实时天气。
API 路径和主要字段参照原仓库，但因服务端持久化和用户隔离，原前端需适配 X-User-Id、幂等键和会话创建流程；不能无修改直接替换 Python 服务。

测试覆盖任务落库与版本、幂等与冲突、日期/预算校验、聊天整轮持久化与失败回滚、同会话并发冲突、超时终态保护、用户隔离、记忆去重与衰减，以及模型 HTTP 协议与错误处理。
集成测试使用 test profile 的独立 H2 MySQL 模式和模型替身，不连接已配置的本地 MySQL，不调用真实模型。
H2 验证不等同于 MySQL 真实环境联调；真实模型输出与供应商兼容性需要配置密钥后验证。
