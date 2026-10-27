#!/usr/bin/env bash
# 建库、建账号、导表结构与初始数据。
#
# 必须用 root 执行（本机 root 账号走 unix_socket 认证，普通用户连不上）：
#   sudo bash db/init.sh
#
# 重复执行会清空 cms_db 下的所有表，只用于开发环境。
set -euo pipefail

if [[ ${EUID} -ne 0 ]]; then
    echo "请用 root 执行：sudo bash db/init.sh" >&2
    exit 1
fi

DB_NAME="${DB_NAME:-cms_db}"
DB_USER="${DB_USER:-cmsapp}"
DB_PASS="${DB_PASS:-Cms@2026}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SQL_DIR="${SCRIPT_DIR}/../src/main/resources/sql"

for f in "${SQL_DIR}/schema.sql" "${SQL_DIR}/data.sql"; do
    [[ -f "$f" ]] || { echo "缺少文件：$f" >&2; exit 1; }
done

echo "==> 创建数据库 ${DB_NAME}"
mysql --default-character-set=utf8mb4 -uroot <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\`
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'localhost';
FLUSH PRIVILEGES;
SQL

echo "==> 导入表结构"
mysql --default-character-set=utf8mb4 -uroot "$DB_NAME" < "${SQL_DIR}/schema.sql"

echo "==> 导入初始数据"
mysql --default-character-set=utf8mb4 -uroot "$DB_NAME" < "${SQL_DIR}/data.sql"

echo "==> 校验"
mysql --default-character-set=utf8mb4 -uroot "$DB_NAME" -e "
SELECT 'user' AS t, COUNT(*) AS n FROM sys_user
UNION ALL SELECT 'role', COUNT(*) FROM sys_role
UNION ALL SELECT 'permission', COUNT(*) FROM sys_permission
UNION ALL SELECT 'category', COUNT(*) FROM cms_category
UNION ALL SELECT 'article', COUNT(*) FROM cms_article
UNION ALL SELECT 'log', COUNT(*) FROM sys_log;
"

echo
echo "完成。数据库 ${DB_NAME}，应用账号 ${DB_USER}/${DB_PASS}"
echo "测试账号：admin / author / auditor / guest，密码均为 123456"
