-- =============================================================
-- 企业级 CMS · 初始化数据
-- 依赖 schema.sql 已执行。可重复执行（先清空再插入）。
-- 所有测试账号的密码都是 123456（BCrypt cost=10）
-- =============================================================

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE sys_role_permission;
TRUNCATE TABLE sys_user_role;
TRUNCATE TABLE sys_log;
TRUNCATE TABLE cms_article;
TRUNCATE TABLE cms_category;
TRUNCATE TABLE sys_permission;
TRUNCATE TABLE sys_role;
TRUNCATE TABLE sys_user;
SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------- 角色 -------------------------
INSERT INTO sys_role (id, role_code, role_name, description, sort) VALUES
(1, 'GUEST',   '游客',   '只读：浏览前台文章', 10),
(2, 'AUTHOR',  '作者',   '撰写、修改自己的稿件并提交审核', 20),
(3, 'AUDITOR', '审核员', '审核稿件、发布与下架', 30),
(4, 'ADMIN',   '管理员', '系统内置，拥有全部权限', 40);

-- ------------------------- 权限点 -------------------------
INSERT INTO sys_permission (id, perm_code, perm_name, perm_group, sort) VALUES
(1,  'article:read',       '查看文章',    '内容', 10),
(2,  'article:create',     '新建文章',    '内容', 20),
(3,  'article:update:own', '修改自己的文章', '内容', 30),
(4,  'article:update:any', '修改任意文章', '内容', 40),
(5,  'article:delete:own', '删除自己的文章', '内容', 50),
(6,  'article:delete:any', '删除任意文章', '内容', 60),
(7,  'article:submit',     '提交审核',    '内容', 70),
(8,  'article:audit',      '审核文章',    '审核', 80),
(9,  'article:publish',    '发布与下架',  '审核', 90),
(10, 'category:read',      '查看分类',    '分类', 100),
(11, 'category:write',     '维护分类',    '分类', 110),
(12, 'user:manage',        '用户管理',    '账号与权限', 120),
(13, 'role:manage',        '角色管理',    '账号与权限', 130),
(14, 'log:read',           '查看操作日志', '系统', 140),
(15, 'guide:use',          '使用站内引导', '系统', 150);

-- ------------------------- 角色-权限 -------------------------
-- 游客：能看、能用引导
INSERT INTO sys_role_permission (role_id, perm_id) VALUES
(1, 1), (1, 10), (1, 15);

-- 作者：自己的内容全生命周期（不含审核）
INSERT INTO sys_role_permission (role_id, perm_id) VALUES
(2, 1), (2, 2), (2, 3), (2, 5), (2, 7), (2, 10), (2, 15);

-- 审核员：看 + 审 + 发/下架
INSERT INTO sys_role_permission (role_id, perm_id) VALUES
(3, 1), (3, 4), (3, 8), (3, 9), (3, 10), (3, 15);

-- 管理员：全部
INSERT INTO sys_role_permission (role_id, perm_id)
SELECT 4, id FROM sys_permission;

-- ------------------------- 用户 -------------------------
-- 密码统一 123456，BCrypt 密文（cost 10）。生产环境当然不能这样。
INSERT INTO sys_user (id, username, password, nickname, email, status, create_by) VALUES
(1, 'admin',   '$2b$10$b6u1GsWQpJ3uXI03tu8Sne39Zca0Y89gPkdf86wY297ovLRrbgf2C', '系统管理员', 'admin@example.com',   1, 1),
(2, 'author',  '$2b$10$b6u1GsWQpJ3uXI03tu8Sne39Zca0Y89gPkdf86wY297ovLRrbgf2C', '张三',       'author@example.com',  1, 1),
(3, 'auditor', '$2b$10$b6u1GsWQpJ3uXI03tu8Sne39Zca0Y89gPkdf86wY297ovLRrbgf2C', '李四',       'auditor@example.com', 1, 1),
(4, 'guest',   '$2b$10$b6u1GsWQpJ3uXI03tu8Sne39Zca0Y89gPkdf86wY297ovLRrbgf2C', '王五',       'guest@example.com',   1, 1),
(5, 'bob',     '$2b$10$b6u1GsWQpJ3uXI03tu8Sne39Zca0Y89gPkdf86wY297ovLRrbgf2C', '赵六',       'bob@example.com',     1, 1);

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 4), (2, 2), (3, 3), (4, 1), (5, 2);

-- ------------------------- 分类 -------------------------
INSERT INTO cms_category (id, parent_id, name, level, sort, status, description, create_by) VALUES
(1,  0,  '技术',     1, 10, 1, '技术类内容', 1),
(2,  0,  '产品',     1, 20, 1, '产品类内容', 1),
(3,  0,  '运营',     1, 30, 1, '运营类内容', 1),
(4,  1,  '后端',     2, 10, 1, NULL, 1),
(5,  1,  '前端',     2, 20, 1, NULL, 1),
(6,  4,  'Java',     3, 10, 1, NULL, 1),
(7,  4,  '数据库',   3, 20, 1, NULL, 1),
(8,  5,  'CSS',      3, 10, 1, NULL, 1),
(9,  2,  '需求分析', 2, 10, 1, NULL, 1),
(10, 3,  '活动策划', 2, 10, 1, NULL, 1);

-- ------------------------- 文章 -------------------------
-- PUBLISHED 的进前台；PENDING 的进审核台；DRAFT / REJECTED / OFFLINE 只有作者看得到
INSERT INTO cms_article
(id, title, summary, content, category_id, author_id, status,
 auditor_id, audit_opinion, audit_time, submit_time, publish_time, offline_reason,
 view_count, version, create_by, create_time) VALUES
(1,  'Spring AOP 是怎么织入的', '从代理对象的生成讲到调用链，讲清权限校验为什么能做成切面。',
 'AOP 的核心是代理。Spring 在容器启动时判断目标类是否实现了接口，有接口用 JDK 动态代理，没有用 CGLIB 字节码增强。调用方拿到的是代理对象，代理在调用真实方法前后插入增强逻辑。权限校验正是这样一件事：方法执行前读注解、算权限、不满足就抛异常，业务代码里一行判断都不用写。',
 6, 2, 'PUBLISHED', 3, '结构清楚，可以发布', '2026-10-08 10:20:00', '2026-10-08 09:00:00', '2026-10-08 10:20:00', NULL, 128, 0, 2, '2026-10-07 16:00:00'),

(2,  'MyBatis-Plus 分页插件的正确顺序', '分页、乐观锁、防全表更新三个内部拦截器谁先谁后。',
 'PaginationInnerInterceptor 要放在最前面，否则 count 语句可能被后续拦截器改写；OptimisticLockerInnerInterceptor 在更新时补 version 条件；BlockAttackInnerInterceptor 拦截没有 where 的 update / delete。顺序错了不会报错，只会在某个边缘场景悄悄失效。',
 6, 2, 'PUBLISHED', 3, '通过', '2026-10-09 11:00:00', '2026-10-09 08:30:00', '2026-10-09 11:00:00', NULL, 96, 0, 2, '2026-10-08 20:00:00'),

(3,  '一次 N+1 查询的排查记录', '列表页 20 条数据发了 41 条 SQL，最后用一条 join 解决。',
 '现象是列表接口耗时 800ms。打开 SQL 日志一数，主查询 1 条，然后每条记录又去查作者名和分类名，20 条就是 40 条。改法是把关联表 join 进来一次取完，在 XML 里写 baseColumns 复用字段列表。改完 3 条 SQL 以内，耗时降到 40ms。',
 7, 2, 'PUBLISHED', 3, '实测数据扎实', '2026-10-10 14:00:00', '2026-10-10 09:00:00', '2026-10-10 14:00:00', NULL, 210, 0, 2, '2026-10-09 21:00:00'),

(4,  '数据库索引为什么没走上', '最左前缀、隐式类型转换、函数包裹字段，三个最常见的坑。',
 '第一个坑是最左前缀：联合索引 (status, category_id, create_time) 只按 category_id 查用不上索引。第二个是隐式转换：字段是 varchar，条件里传数字，索引失效。第三个是函数包裹：where date(create_time) = ... 会让索引完全废掉，改成范围查询。',
 7, 5, 'PUBLISHED', 3, '通过', '2026-10-11 10:00:00', '2026-10-11 09:00:00', '2026-10-11 10:00:00', NULL, 175, 0, 5, '2026-10-10 19:00:00'),

(5,  '用拦截器做登录校验而不是每个方法都写', 'HandlerInterceptor 的三个回调点各适合放什么。',
 'preHandle 在进入 Controller 前执行，适合做登录和权限判断；postHandle 在返回视图前执行，适合塞公共数据；afterCompletion 无论成败都会执行，适合清理 ThreadLocal——这一点很关键，线程池复用线程，不清理会把上一个请求的用户信息带给下一个请求。',
 6, 2, 'PUBLISHED', 3, '通过', '2026-10-12 15:00:00', '2026-10-12 10:00:00', '2026-10-12 15:00:00', NULL, 88, 0, 2, '2026-10-11 20:00:00'),

(6,  'CSS Grid 做卡片布局', '一行代码搞定响应式卡片网格，比 flex 少写很多媒体查询。',
 'grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)) 这一行就把「一行放几个」交给浏览器决定。窗口变窄自动减少列数，不需要写断点。gap 控制间距，比给每个卡片加 margin 再处理最后一个更干净。',
 8, 5, 'PUBLISHED', 3, '通过', '2026-10-13 09:30:00', '2026-10-13 08:00:00', '2026-10-13 09:30:00', NULL, 143, 0, 5, '2026-10-12 18:00:00'),

(7,  '需求分析要写到什么颗粒度', '写到「能据此建表、能据此写接口」就够了，再多是浪费。',
 '过粗的需求分析后面会反复返工；过细则在需求变更时全部作废。判断标准很朴素：拿着这份文档，另一个人能不能直接把表建出来、把接口签名定下来。能，就够；不能，就还缺东西。',
 9, 2, 'PUBLISHED', 3, '通过', '2026-10-14 11:00:00', '2026-10-14 09:00:00', '2026-10-14 11:00:00', NULL, 67, 0, 2, '2026-10-13 20:00:00'),

(8,  '活动复盘怎么写才有用', '不要写「效果不错」，要写清楚哪个环节掉人。',
 '复盘的价值在于找出可复用的结论。把漏斗拆开：曝光、点击、参与、留存，每一层算出转化率，找出掉得最多的那一层，然后问为什么。结论要写成「下次遇到同类活动，X 环节应该改成 Y」，否则明年还得重来一遍。',
 10, 5, 'PUBLISHED', 3, '通过', '2026-10-15 16:00:00', '2026-10-15 10:00:00', '2026-10-15 16:00:00', NULL, 52, 0, 5, '2026-10-14 20:00:00'),

(9,  '为什么用业务码而不是直接抛 HTTP 状态', '前端要根据原因做不同提示，HTTP 状态码分不了这么细。',
 'HTTP 状态码表达的是「这次请求在协议层的结果」，业务失败往往是 200 + 业务码。比如「分类层级超过三级」和「分类名重复」，都是 400，但前端要提示的话完全不同。用五位业务码，前端可以精确匹配。',
 6, 2, 'PUBLISHED', 3, '通过', '2026-10-16 09:00:00', '2026-10-16 08:00:00', '2026-10-16 09:00:00', NULL, 74, 0, 2, '2026-10-15 20:00:00'),

(10, '乐观锁在 CMS 里的实际用途', '两个编辑同时打开同一篇文章，后保存的会被拦下。',
 '场景很常见：编辑 A 和 B 同时打开一篇文章修改，A 先保存成功，B 再保存就会把 A 的修改覆盖掉。加一个 version 字段，更新时带上 version = 旧值，MySQL 返回影响行数 0 就说明被人改过，提示 B 刷新重试。这比加锁便宜得多。',
 6, 5, 'PUBLISHED', 3, '通过', '2026-10-17 10:00:00', '2026-10-17 09:00:00', '2026-10-17 10:00:00', NULL, 119, 0, 5, '2026-10-16 20:00:00'),

(11, 'Spring Boot 启动时的自动配置顺序', '为什么有些 Bean 不能直接注入，会引发循环依赖。',
 '自动配置类按 @AutoConfigureAfter / Before 排序，但容器把 Web 相关的基础设施（比如 RequestMappingHandlerMapping）注册得比较晚。如果某个 Bean 在构造时就注入它，可能拿到还没初始化完的实例。正确做法是延迟获取，或者监听 ApplicationReadyEvent。',
 6, 2, 'PUBLISHED', 3, '通过', '2026-10-18 11:00:00', '2026-10-18 09:00:00', '2026-10-18 11:00:00', NULL, 91, 0, 2, '2026-10-17 20:00:00'),

(12, '分页参数为什么要卡上限', 'pageSize 传 100000 的时候，数据库就死了。',
 '分页不是免费的：limit 100000, 20 需要先扫过 100000 行再丢掉。前端正常用不会传这么大，但接口是公开的，任何人都能构造请求。所以在插件层设 maxLimit，超过就按上限处理，这是最简单的一道防线。',
 7, 2, 'PUBLISHED', 3, '通过', '2026-10-18 16:00:00', '2026-10-18 14:00:00', '2026-10-18 16:00:00', NULL, 63, 0, 2, '2026-10-17 21:00:00'),

-- 待审核
(13, '小程序端适配的几个坑', 'rpx 和 px 混用、安全区域、以及 iOS 的橡皮筋效果。',
 'rpx 是按 750 设计稿换算的，和 px 混用很容易在真机上尺寸不对。安全区域要处理底部 home indicator，否则按钮被挡住。iOS 的 scroll-view 下拉会带动整页回弹，要单独处理。',
 5, 2, 'PENDING', NULL, NULL, NULL, '2026-10-19 09:00:00', NULL, NULL, 0, 0, 2, '2026-10-18 20:00:00'),

(14, '接口文档用 ApiFox 维护的经验', '先写文档再写代码，改动会少很多。',
 '把请求体、响应体、错误码先定下来，前后端可以并行开发。ApiFox 支持导入 OpenAPI，改完同步很方便。关键是要把错误码写进文档，否则前端只能靠猜。',
 4, 5, 'PENDING', NULL, NULL, NULL, '2026-10-19 15:00:00', NULL, NULL, 0, 0, 5, '2026-10-19 10:00:00'),

(15, '日志脱敏应该在哪一层做', '在切面里做，还是每个 DTO 上打注解？',
 '推荐在写日志的那一刻做，也就是切面里。因为脱敏的目的就是「不落盘」，越靠近落盘点越可靠。DTO 上加注解的方式，一旦有人绕过 DTO 直接序列化对象，敏感字段就又漏出去了。',
 6, 2, 'PENDING', NULL, NULL, NULL, '2026-10-20 09:30:00', NULL, NULL, 0, 0, 2, '2026-10-19 21:00:00'),

(16, '分类树删除前要检查什么', '子分类、文章引用、以及逻辑删除带来的坑。',
 '删除分类前必须检查两件事：有没有子分类，有没有文章引用。另外逻辑删除会让唯一索引 (parent_id, name) 在删除后仍然占位，同名分类建不出来，需要给唯一索引加上 deleted 列或者改用物理删除标记。',
 4, 5, 'PENDING', NULL, NULL, NULL, '2026-10-20 11:00:00', NULL, NULL, 0, 0, 5, '2026-10-20 08:00:00'),

-- 草稿
(17, '把智能体做成「知道整个站点」的样子', '站点结构从代码里反射出来，而不是手写一份清单。',
 '手写的清单一定会和代码脱节。正确做法是启动时扫描所有 Controller 的注解，把页面、路由、权限拼成站点地图。少写一个注解，服务就拒绝启动，这样结构永远不会漂移。',
 6, 2, 'DRAFT', NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 2, '2026-10-21 20:00:00'),

(18, '从零搭建一台开发环境', 'JDK、Maven、MySQL、Git 的版本怎么选。',
 'JDK 用 11 或 17，Spring Boot 2.7 都支持；Maven 3.6 以上；MySQL 8 或 MariaDB 10.6 都可以。重点是版本要写进 README，否则换台机器就跑不起来。',
 4, 5, 'DRAFT', NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, 5, '2026-10-22 19:00:00'),

-- 已驳回
(19, '本站即将上线，敬请期待', '一句话公告。',
 '内容过短，且与本栏目定位不符。',
 3, 2, 'REJECTED', 3, '内容过于单薄，请补充具体信息后重新提交', '2026-10-21 10:00:00', '2026-10-21 09:00:00', NULL, NULL, 0, 0, 2, '2026-10-20 20:00:00'),

-- 已下架
(20, '旧版接口迁移说明', 'v1 接口即将停止服务，请尽快迁移到 v2。',
 'v1 接口将在本月底停止服务。迁移指南见新的 v2 文档。v2 的响应结构统一为 result 包装，错误码改为五位业务码。',
 6, 2, 'OFFLINE', 3, '通过', '2026-09-10 10:00:00', '2026-09-10 09:00:00', '2026-09-10 10:00:00', 'v2 接口已全量上线，本文内容过期', 340, 0, 2, '2026-09-09 20:00:00');

-- ------------------------- 操作日志 -------------------------
-- 作为演示数据，让日志页打开就有内容；真实日志由 LogAspect 写入
INSERT INTO sys_log (user_id, username, log_type, operation, method, uri, http_method, params, ip, result, error_msg, cost_ms, create_time) VALUES
(2, 'author',  'LOGIN',   '登录系统',   'AuthController#login',      '/api/auth/login',      'POST', '{"username":"author","password":"***"}', '127.0.0.1', 'SUCCESS', NULL, 42,  '2026-10-21 08:50:00'),
(2, 'author',  'ARTICLE', '提交文章审核', 'ArticleController#submit',  '/api/articles/13/submit', 'POST', '{"id":13}', '127.0.0.1', 'SUCCESS', NULL, 18, '2026-10-21 09:00:00'),
(3, 'auditor', 'LOGIN',   '登录系统',   'AuthController#login',      '/api/auth/login',      'POST', '{"username":"auditor","password":"***"}', '127.0.0.1', 'SUCCESS', NULL, 39,  '2026-10-21 10:05:00'),
(3, 'auditor', 'AUDIT',   '驳回文章',   'ArticleController#reject',  '/api/articles/19/reject', 'POST', '{"id":19,"opinion":"内容过于单薄"}', '127.0.0.1', 'SUCCESS', NULL, 21, '2026-10-21 10:10:00'),
(4, 'guest',   'ARTICLE', '发布文章',   'ArticleController#approve', '/api/articles/13/approve', 'POST', '{"id":13}', '127.0.0.1', 'DENIED', '缺少权限：article:audit', 3, '2026-10-21 11:00:00'),
(1, 'admin',   'ROLE',    '调整角色权限', 'RoleController#permissions', '/api/roles/2/permissions', 'PUT', '{"permIds":[1,2,3,5,7,10,15]}', '127.0.0.1', 'SUCCESS', NULL, 27, '2026-10-22 09:30:00'),
(2, 'author',  'AI_QUERY', '站内引导问答', 'GuideController#chat',     '/api/guide/chat',      'POST', '{"hitCards":["article.list"],"source":"RULE","length":9}', '127.0.0.1', 'SUCCESS', NULL, 6, '2026-10-22 10:00:00');
