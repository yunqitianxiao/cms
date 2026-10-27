# 接口文档

Base URL：`http://localhost:8090`。所有接口返回 JSON（页面路由 `/app/*`、`/portal` 返回 HTML）。

**ApiFox 集合**：`apifox/cms-openapi.json`（OpenAPI 3.0，49 个接口、50 个 schema）。导入方式：Apifox → 导入数据 → OpenAPI/Swagger → 选该文件。导入后先调 `/api/auth/login`（`admin` / `123456`），Apifox 会保存返回的 Cookie，其余接口直接可跑。

---

## 一、通用约定

### 1. 统一响应

```json
{
  "code": 0,
  "message": "成功",
  "data": {},
  "traceId": "8f3c1e2a",
  "timestamp": 1793096629245
}
```

- `code = 0` 表示成功，其余为业务错误码。
- `traceId` 来自 `X-Trace-Id` 请求头，没有则服务端生成；出错时把 `traceId` 报给开发，能在日志里定位到那一次请求。
- `timestamp` 是服务端时间戳（epoch 毫秒）；`data` 为空时不输出该字段（`spring.jackson.default-property-inclusion: non_null`）。
- HTTP 状态码与业务码同时表达：HTTP 说"协议层结果"，`code` 说"业务原因"。例如权限不足返回 HTTP 403 + `code 40300`。

### 2. 业务码

| code | 含义 | 典型场景 |
|---|---|---|
| 0 | 成功 | |
| 40000 | 请求参数不合法 | Bean Validation 未通过、排序字段不在白名单 |
| 40001 | 分类层级超出上限 | 尝试创建第 4 级分类 |
| 40100 | 未登录或登录已失效 | 无 session 访问受保护接口 |
| 40101 | 用户名或密码错误 | 登录 |
| 40102 | 账号已被停用 | 已停用账号登录 |
| 40300 | 没有操作权限 | `@RequirePermission` 不通过 |
| 40301 | 只能操作自己的数据 | 作者改别人的文章 |
| 40400 | 资源不存在 | 文章/用户 id 不存在或已删除 |
| 40900 | 资源状态冲突 | 如删除最后一个管理员 |
| 40901 | 当前状态不允许该操作 | 状态机非法迁移 |
| 40902 | 数据已存在 | 用户名、角色编码、同级分类重名 |
| 40903 | 数据被引用，无法删除 | 删有文章的分类、删有用户的角色 |
| 50000 | 系统内部错误 | 未预期异常 |
| 50001 | 数据库操作失败 | SQL 异常 |

### 3. 鉴权

基于 Session Cookie（`JSESSIONID`），登录成功后由浏览器自动携带。

- 页面：未登录访问 `/app/*` 会 `302` 到 `/login?next=<原路径>`。
- 接口：未登录返回 `40100`，前端统一跳登录页。

### 4. 分页

请求参数 `pageNum`（默认 1，最小 1）、`pageSize`（默认 10，最大 200）。

响应：

```json
{
  "total": 42,
  "pageNum": 1,
  "pageSize": 10,
  "pages": 5,
  "records": []
}
```

---

## 二、认证 `/api/auth`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/api/auth/login` | 无 | 登录 |
| POST | `/api/auth/logout` | 无 | 退出 |
| POST | `/api/auth/register` | 无 | 注册（默认 GUEST 角色） |
| GET | `/api/auth/me` | 登录 | 当前用户 + 角色 + 权限 |

**登录** `POST /api/auth/login`

```json
{ "username": "admin", "password": "123456" }
```

成功返回：

```json
{
  "code": 0,
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "系统管理员",
    "roleCodes": ["ADMIN"],
    "permissions": ["article:read", "article:create", "..."]
  }
}
```

失败：`40101` 用户名或密码错误；`40102` 账号已被停用。登录成功与被拒绝都会写 `sys_log`（`log_type = LOGIN`）。

**注册** `POST /api/auth/register`

```json
{ "username": "zhangsan", "password": "123456", "nickname": "张三", "email": "z@example.com" }
```

用户名 4–20 位字母/数字/下划线，密码 6–32 位。

---

## 三、文章 `/api/articles`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/articles` | `article:read` | 分页查询 |
| GET | `/api/articles/{id}` | `article:read` | 详情 |
| POST | `/api/articles` | `article:create` | 新建（保存为草稿） |
| PUT | `/api/articles/{id}` | `article:update:own` 或 `article:update:any` | 修改草稿/被驳回稿 |
| POST | `/api/articles/{id}/submit` | `article:submit` 或 `article:update:any` | 提交审核 |
| POST | `/api/articles/{id}/withdraw` | 同上 | 撤回（待审核 → 草稿） |
| POST | `/api/articles/{id}/resubmit` | 同上 | 已下架 → 重新提交审核 |
| POST | `/api/articles/{id}/approve` | `article:audit` | 审核通过 |
| POST | `/api/articles/{id}/reject` | `article:audit` | 驳回（意见必填） |
| POST | `/api/articles/{id}/offline` | `article:publish` | 下架（原因必填） |
| POST | `/api/articles/batch/approve` | `article:publish` | 批量通过 |
| POST | `/api/articles/batch/offline` | `article:publish` | 批量下架 |
| DELETE | `/api/articles/{id}` | `article:delete:own` 或 `article:delete:any` | 删除（仅草稿/被驳回） |

**查询参数**

| 参数 | 类型 | 说明 |
|---|---|---|
| `pageNum` / `pageSize` | int | 分页 |
| `keyword` | string | 标题或摘要包含 |
| `status` | string | `DRAFT`/`PENDING`/`PUBLISHED`/`REJECTED`/`OFFLINE` |
| `categoryId` | long | 分类，**包含其所有子分类** |
| `authorId` | long | 作者 |
| `mine` | bool | 只看我的 |
| `sort` | string | 形如 `createTime,desc;title,asc`，最多 3 个 |

`sort` 可选逻辑字段：`createTime`、`updateTime`、`publishTime`、`title`、`viewCount`。其他值返回 `40000`——排序片段不能直接拼进 SQL，只走白名单映射。

**保存文章** `POST /api/articles`

```json
{
  "title": "MyBatis-Plus 分页插件踩坑记录",
  "summary": "总条数为什么一直是 0",
  "content": "第一段……\n第二段……",
  "categoryId": 4,
  "version": 0
}
```

`version` 为乐观锁版本号：编辑页回传，冲突时返回 `40900` 并提示刷新。新文章可为空。

**批量接口** 返回逐条结果，不会因为一条失败就整体回滚：

```json
{
  "total": 3,
  "success": 2,
  "failed": [17],
  "reasons": { "17": "当前状态【草稿】不能执行【审核通过】", "99999": "文章不存在" }
}
```

`failed` 是失败记录的 **id 数组**（不是个数），`reasons` 是以 id 为键的原因表；`success` 才是数字。前端判断成功要用 `failed.length === 0`——空数组在 JS 里是真值。

---

## 四、分类 `/api/categories`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/categories/tree?onlyEnabled=false` | `category:read` | 树形结构 |
| GET | `/api/categories` | `category:read` | 平铺列表 |
| POST | `/api/categories` | `category:write` | 新增 |
| PUT | `/api/categories/{id}` | `category:write` | 修改（**忽略 parentId**） |
| PUT | `/api/categories/{id}/status?status=1` | `category:write` | 启用/停用 |
| DELETE | `/api/categories/{id}` | `category:write` | 删除 |

```json
{ "parentId": 0, "name": "技术", "sort": 10, "status": 1, "description": null }
```

- `parentId = 0` 为一级分类；`level` 由服务端推导，超过 3 级返回 `40001`。
- 修改接口不接收上级变更：换父节点会让 `level` 与子树全部失效，本项目直接不支持，需要移动时新建分类再迁移文章。
- 删除有子分类或挂着文章的分类返回 `40903`。

---

## 五、用户 `/api/users`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/users` | `user:manage` | 分页查询 |
| GET | `/api/users/{id}` | `user:manage` | 详情 |
| POST | `/api/users` | `user:manage` | 新增 |
| PUT | `/api/users/{id}` | `user:manage` | 修改资料（用户名不可改） |
| PUT | `/api/users/{id}/status?status=0` | `user:manage` | 启用/停用 |
| PUT | `/api/users/{id}/roles` | `user:manage` | 分配角色 |
| DELETE | `/api/users/{id}` | `user:manage` | 删除 |

查询参数：`keyword`（用户名/昵称）、`status`、`roleCode`、分页。

新增/修改请求：

```json
{
  "username": "lisi",
  "password": "123456",
  "nickname": "李四",
  "email": "lisi@example.com",
  "phone": "13800000000",
  "status": 1,
  "roleIds": [2]
}
```

- `password` 留空表示不修改密码；前端应传 `null` 而不是空串。
- 用户名创建后不可修改，避免审计链断裂。
- 停用或删除最后一个可用管理员返回 `40900`；删除自己同样返回 `40900`。
- 名下仍有稿件的账号不能删除，返回 `40903`（用户是逻辑删除，外键不会拦）。

---

## 六、角色 `/api/roles`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/roles` | `role:manage` 或 `user:manage` | 角色列表（含权限 id、用户数） |
| GET | `/api/roles/permissions` | 同上 | 全部权限点（按组展示） |
| POST | `/api/roles` | `role:manage` | 新增 |
| PUT | `/api/roles/{id}` | `role:manage` | 修改 |
| PUT | `/api/roles/{id}/permissions` | `role:manage` | 全量覆盖权限 |
| DELETE | `/api/roles/{id}` | `role:manage` | 删除 |

```json
{ "roleCode": "EDITOR", "roleName": "编辑", "sort": 100, "status": 1, "description": null }
```

- `roleCode` 为 2–20 位大写字母或下划线，唯一，创建后不可修改。
- 分配权限请求：`{ "permIds": [1, 2, 3] }`，**全量覆盖**语义（传什么就是什么，不是增量）。
- 管理员角色不可删除；且保存权限时必须保留 `role:manage`，否则返回 `40900`——防止所有人被锁在权限配置之外。
- 有用户引用的角色删除返回 `40903`。

---

## 七、操作日志 `/api/logs`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/logs` | `log:read` | 分页查询 |

参数：`keyword`（操作名/用户名/URI）、`logType`、`result`（`SUCCESS`/`DENIED`/`FAIL`）、`startDate`、`endDate`（`yyyy-MM-dd`）、分页。

`params` 字段中的 `password`、`token` 等已替换为 `***`。

---

## 八、工作台 `/api/dashboard`

`GET /api/dashboard/stats`（登录即可）

```json
{
  "myDraft": 2,
  "myPending": 1,
  "myRejected": 1,
  "myPublished": 5,
  "pendingTotal": 4,
  "publishedTotal": 12,
  "categoryCount": 10,
  "userCount": 5,
  "permissions": ["article:read", "..."]
}
```

前 4 项是当前用户的；`pendingTotal` 等全局数字只有审核员和管理员用得上（前端按权限决定是否展示）。`permissions` 让前端不必再请求一次 `/api/auth/me`。

---

## 九、站内引导 `/api/guide`

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/api/guide/pages` | `guide:use` | 全部页面卡片（按当前用户标注可访问性） |
| POST | `/api/guide/chat` | `guide:use` | 提问 |
| GET | `/api/guide/suggestions` | `guide:use` | 快捷问题 |
| GET | `/api/guide/llm` | `guide:use` | 模型通道状态（不回显密钥） |
| PUT | `/api/guide/llm` | `guide:use` | 填写自己的模型密钥（只存当前会话） |
| DELETE | `/api/guide/llm` | `guide:use` | 清除本会话密钥 |
| POST | `/api/guide/llm/test` | `guide:use` | 测试模型连通性 |
| GET | `/api/guide/selfcheck` | `log:read` | 站点地图自检结果 |

**卡片结构**

```json
{
  "id": "article.create",
  "title": "写新文章",
  "group": "内容",
  "route": "/app/article-edit",
  "summary": "起草新文章，写完后提交审核",
  "steps": ["填标题和摘要……", "选择分类……", "保存后回列表提交审核"],
  "tips": ["被驳回的文章修改后回到草稿状态，需要重新提交"],
  "aliases": ["写文章", "投稿", "新建", "起草"],
  "highlight": "#article-title",
  "permissions": ["article:create"],
  "apiPath": "/api/articles",
  "httpMethod": "POST",
  "accessible": true,
  "denyHint": null
}
```

`accessible = false` 时 `denyHint` 说明缺哪项权限，前端把卡片置灰。

**提问** `POST /api/guide/chat`

```json
{ "message": "文章被驳回了怎么改" }
```

```json
{
  "answer": "为你找到 1 个相关页面。",
  "cards": [ { "id": "article.list", "...": "..." } ],
  "source": "RULE",
  "notFound": false
}
```

- `source`：`RULE` 规则匹配 / `LLM` 模型理解 / `EMPTY` 未命中。
- 只有规则**没命中**时才调用模型；模型不可用或超时会静默回落，前端只会看到 `RULE` 或"没有这个功能"。
- `notFound = true` 表示系统里没有对应功能，此时 `cards` 为空。**不硬塞卡片**是这里的设计选择。
- 返回的卡片 `id` 一定在站点地图里；LLM 通道输出的 id 若不在其中会被丢弃。

**模型通道设置**

```json
GET /api/guide/llm
{
  "enabled": true,
  "configured": true,
  "source": "USER",
  "keyHint": "sk-…a1b2",
  "model": "deepseek-chat",
  "baseUrl": "https://api.deepseek.com"
}
```

- `source`：`USER` 用户在本次会话里填的 / `CONFIG` 服务端配置（`DEEPSEEK_API_KEY` 或 `app.guide.llm.api-key`）/ `NONE`。
- **响应里没有 key 字段**，只有尾号；`configured` 才是"当前是否真的会走模型"（总开关 + 有密钥）。

```json
PUT /api/guide/llm
{ "apiKey": "sk-xxxxxxxx" }
```

密钥只写入当前 HttpSession（服务端内存），不落库、不写前端存储；长度 8–200、不能含空白，否则 `40000`。退出登录后失效。审计日志里该字段显示为 `***`。

```json
DELETE /api/guide/llm     // 清除本会话密钥，回落到服务端配置（若有）

POST /api/guide/llm/test
{ "ok": true, "latencyMs": 1079, "message": "连接成功，模型 deepseek-chat 可用" }
```

测试失败同样返回 `code = 0`，用 `data.ok = false` 表达结果——这是诊断接口，不该让前端把"密钥填错"当成系统异常。`message` 已做脱敏，不会回显密钥。

---

## 十、前台 `/api/portal`（免登录）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/portal/articles` | 已发布文章分页，参数同文章查询 |
| GET | `/api/portal/articles/{id}` | 已发布文章详情，阅读量 +1 |
| GET | `/api/portal/categories` | 启用状态的分类树 |

非 `PUBLISHED` 状态的文章在这里一律返回 `40400`，不区分"不存在"和"未发布"。

---

## 十一、页面路由

| 路径 | 说明 |
|---|---|
| `/login`、`/register` | 登录、注册 |
| `/app/dashboard` | 工作台 |
| `/app/articles` | 文章列表 |
| `/app/article-edit?id=` | 写/改文章 |
| `/app/review` | 内容审核 |
| `/app/categories` | 分类管理 |
| `/app/users` | 用户管理 |
| `/app/roles` | 角色与权限 |
| `/app/logs` | 操作日志 |
| `/portal`、`/portal/article/{id}` | 前台列表、正文 |

`/app/*` 由 `HandlerInterceptor` 校验登录；具体权限在接口层用 AOP 拦截。
