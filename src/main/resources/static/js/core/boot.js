/**
 * 每个管理页的第一段代码：拿到当前用户、按权限裁菜单、挂上引导组件。
 *
 * 页面模块通过 `await bootReady` 等它完成，避免出现「菜单还没裁完就渲染表格」。
 */

import { api } from './api.js';
import { toast } from './ui.js';
import { initGuide } from '../guide.js';

export let me = null;

export const bootReady = (async () => {
    try {
        me = await api.get('/api/auth/me');
    } catch (e) {
        // 401 已由 api.js 跳转；其他错误留在页面上，比白屏好排查
        if (e.code !== 40100) toast(e.message, 'err');
        document.querySelectorAll('.loading').forEach((n) => { n.textContent = '登录状态校验失败'; });
        return;
    }

    renderUser();
    trimNav();
    bindLogout();
    fillPendingCount();
    initGuide(me);
})();

function renderUser() {
    const name = document.getElementById('me-name');
    const role = document.getElementById('me-role');
    const avatar = document.getElementById('me-avatar');
    if (!name) return;
    const display = me.nickname || me.username;
    name.textContent = display;
    role.textContent = me.roleCodes && me.roleCodes.length ? me.roleCodes.join(' / ') : '无角色';
    avatar.textContent = display.slice(0, 1);
}

function trimNav() {
    const perms = new Set(me.permissions || []);
    document.querySelectorAll('[data-perm]').forEach((el) => {
        const need = el.dataset.perm.split(',').map((s) => s.trim()).filter(Boolean);
        if (!need.some((p) => perms.has(p))) el.remove();
    });
}

function bindLogout() {
    const btn = document.getElementById('btn-logout');
    if (!btn) return;
    btn.onclick = async () => {
        try {
            await api.post('/api/auth/logout');
        } finally {
            location.href = '/login';
        }
    };
}

async function fillPendingCount() {
    if (!(me.permissions || []).includes('article:audit')) return;
    const slot = document.getElementById('nav-pending-count');
    if (!slot) return;
    try {
        const stats = await api.get('/api/dashboard/stats');
        if (stats.pendingTotal > 0) slot.textContent = stats.pendingTotal;
    } catch (e) { /* 角标拿不到不影响主流程 */ }
}

/** 页面里判断权限的小工具 */
export function can(perm) {
    return !!(me && (me.permissions || []).includes(perm));
}

export function canAny(...perms) {
    return perms.some((p) => can(p));
}
