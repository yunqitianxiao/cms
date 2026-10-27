-- =============================================================
-- 企业级 CMS · 数据库结构
-- 六张主表 + 两张关联表。可重复执行：先按外键依赖倒序删除再重建。
-- =============================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_log;
DROP TABLE IF EXISTS cms_article;
DROP TABLE IF EXISTS cms_category;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

-- -------------------------------------------------------------
-- 1. 用户
-- -------------------------------------------------------------
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录名，创建后不可修改（审计链的锚点）',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文，不存明文',
    nickname    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
    email       VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0 正常 / 1 已删',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT                DEFAULT NULL COMMENT '创建人',
    update_by   BIGINT                DEFAULT NULL COMMENT '最后修改人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- -------------------------------------------------------------
-- 2. 角色
-- -------------------------------------------------------------
CREATE TABLE sys_role (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    role_code   VARCHAR(50) NOT NULL COMMENT '角色编码，代码里的常量以此为准',
    role_name   VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(200)         DEFAULT NULL COMMENT '描述',
    sort        INT         NOT NULL DEFAULT 100 COMMENT '展示顺序',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
    deleted     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT               DEFAULT NULL,
    update_by   BIGINT               DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

-- -------------------------------------------------------------
-- 3. 权限点
-- 权限点由初始化脚本写入，运行期只读：启动期的站点地图校验要依赖"权限码集合是稳定的"
-- -------------------------------------------------------------
CREATE TABLE sys_permission (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    perm_code   VARCHAR(64) NOT NULL COMMENT '权限码，如 article:audit',
    perm_name   VARCHAR(64) NOT NULL COMMENT '权限名称',
    perm_group  VARCHAR(32) NOT NULL DEFAULT '' COMMENT '分组，用于前端分类展示',
    description VARCHAR(200)         DEFAULT NULL COMMENT '说明',
    sort        INT         NOT NULL DEFAULT 100,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_perm_code (perm_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='权限点表';

-- -------------------------------------------------------------
-- 4. 分类（自关联，最多 3 级）
-- -------------------------------------------------------------
CREATE TABLE cms_category (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '上级分类，0 表示根',
    name        VARCHAR(50) NOT NULL COMMENT '分类名',
    level       TINYINT     NOT NULL DEFAULT 1 COMMENT '层级 1-3，由服务端推导',
    sort        INT         NOT NULL DEFAULT 100 COMMENT '同级排序',
    status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
    description VARCHAR(200)         DEFAULT NULL,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by   BIGINT               DEFAULT NULL,
    update_by   BIGINT               DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_parent_name (parent_id, name),
    KEY idx_category_parent (parent_id, sort)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='内容分类表';

-- -------------------------------------------------------------
-- 5. 文章
-- -------------------------------------------------------------
CREATE TABLE cms_article (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    title          VARCHAR(200) NOT NULL COMMENT '标题',
    summary        VARCHAR(500)          DEFAULT NULL COMMENT '摘要',
    content        MEDIUMTEXT   NOT NULL COMMENT '正文',
    category_id    BIGINT       NOT NULL COMMENT '所属分类',
    author_id      BIGINT       NOT NULL COMMENT '作者',
    status         VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/PUBLISHED/REJECTED/OFFLINE',
    auditor_id     BIGINT                DEFAULT NULL COMMENT '最后审核人',
    audit_opinion  VARCHAR(500)          DEFAULT NULL COMMENT '审核意见',
    audit_time     DATETIME              DEFAULT NULL COMMENT '审核时间',
    submit_time    DATETIME              DEFAULT NULL COMMENT '提交审核时间',
    publish_time   DATETIME              DEFAULT NULL COMMENT '发布时间',
    offline_reason VARCHAR(500)          DEFAULT NULL COMMENT '下架原因',
    view_count     INT          NOT NULL DEFAULT 0 COMMENT '阅读量',
    version        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted        TINYINT      NOT NULL DEFAULT 0,
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by      BIGINT                DEFAULT NULL,
    update_by      BIGINT                DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_article_query (status, category_id, create_time),
    KEY idx_article_author (author_id, status),
    KEY idx_article_title (title),
    KEY idx_article_publish (publish_time),
    CONSTRAINT fk_article_category FOREIGN KEY (category_id) REFERENCES cms_category (id),
    CONSTRAINT fk_article_author FOREIGN KEY (author_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='文章表';

-- -------------------------------------------------------------
-- 6. 操作日志
-- -------------------------------------------------------------
CREATE TABLE sys_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT                DEFAULT NULL COMMENT '操作人 id，未登录可为空',
    username    VARCHAR(50)           DEFAULT NULL COMMENT '操作人用户名（冗余，便于人员删除后仍可追溯）',
    log_type    VARCHAR(20)  NOT NULL COMMENT 'LOGIN/ARTICLE/AUDIT/CATEGORY/USER/ROLE/AI_QUERY',
    operation   VARCHAR(100) NOT NULL COMMENT '可读的操作名',
    method      VARCHAR(200)          DEFAULT NULL COMMENT '类名#方法名',
    uri         VARCHAR(200)          DEFAULT NULL,
    http_method VARCHAR(10)           DEFAULT NULL,
    params      VARCHAR(2000)         DEFAULT NULL COMMENT '脱敏后的入参',
    ip          VARCHAR(64)           DEFAULT NULL,
    result      VARCHAR(20)  NOT NULL COMMENT 'SUCCESS/DENIED/FAIL',
    error_msg   VARCHAR(400)          DEFAULT NULL,
    cost_ms     BIGINT                DEFAULT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_log_time (create_time),
    KEY idx_log_user (user_id),
    KEY idx_log_type (log_type, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='操作日志表';

-- -------------------------------------------------------------
-- 7. 用户-角色
-- -------------------------------------------------------------
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_ur_role (role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联表';

-- -------------------------------------------------------------
-- 8. 角色-权限
-- -------------------------------------------------------------
CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL,
    perm_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, perm_id),
    KEY idx_rp_perm (perm_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_rp_perm FOREIGN KEY (perm_id) REFERENCES sys_permission (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='角色权限关联表';
