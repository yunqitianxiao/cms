import { me, bootReady } from '../core/boot.js';
import { api } from '../core/api.js';
import {
    esc, fmtTime, toast, modal, confirmDialog, renderPager,
    loadingRow, emptyRow, debounce,
} from '../core/ui.js';

// 顶层 await 会让下面的 const state/COLS 停在暂时性死区
bootReady.then(() => (me ? main() : null)).catch((e) => console.error('用户页初始化失败', e));

const state = { pageNum: 1, pageSize: 10, records: [], roles: [] };
const COLS = 7;

async function main() {
    bind();
    await loadRoles();
    await load();
}

function bind() {
    document.getElementById('btn-add').onclick = () => openForm(null);
    document.getElementById('btn-search').onclick = () => { state.pageNum = 1; load(); };
    document.getElementById('btn-reset').onclick = () => {
        document.getElementById('f-keyword').value = '';
        document.getElementById('f-role').value = '';
        document.getElementById('f-status').value = '';
        state.pageNum = 1;
        load();
    };
    document.getElementById('f-status').onchange = () => { state.pageNum = 1; load(); };
    document.getElementById('f-role').onchange = () => { state.pageNum = 1; load(); };
    document.getElementById('f-keyword').oninput = debounce(() => { state.pageNum = 1; load(); }, 350);
    document.getElementById('user-body').addEventListener('click', onClick);
}

async function loadRoles() {
    try {
        state.roles = await api.get('/api/roles');
        document.getElementById('f-role').innerHTML = '<option value="">全部角色</option>' +
            state.roles.map((r) => `<option value="${esc(r.roleCode)}">${esc(r.roleName)}</option>`).join('');
    } catch (e) {
        toast('角色列表加载失败：' + e.message, 'err');
    }
}

async function load() {
    const tbody = document.getElementById('user-body');
    tbody.innerHTML = loadingRow(COLS);
    try {
        const page = await api.get('/api/users', {
            pageNum: state.pageNum,
            pageSize: state.pageSize,
            keyword: document.getElementById('f-keyword').value.trim(),
            roleCode: document.getElementById('f-role').value,
            status: document.getElementById('f-status').value,
        });
        state.records = page.records;
        render(page.records);
        renderPager(document.getElementById('pager'), page, (n) => { state.pageNum = n; load(); });
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="${COLS}"><div class="empty">加载失败：${esc(e.message)}</div></td></tr>`;
    }
}

function render(rows) {
    const tbody = document.getElementById('user-body');
    if (!rows.length) {
        tbody.innerHTML = emptyRow(COLS, '没有符合条件的账号');
        return;
    }
    tbody.innerHTML = rows.map((u) => `
        <tr>
            <td><b>${esc(u.username)}</b>${u.id === me.id ? ' <span class="badge grey">我</span>' : ''}</td>
            <td>${esc(u.nickname || '—')}</td>
            <td><div class="chips">${(u.roleNames || []).map((r) => `<span class="badge role">${esc(r)}</span>`).join('') || '—'}</div></td>
            <td class="muted" style="font-size:12.5px">
                ${u.email ? esc(u.email) : ''}${u.email && u.phone ? '<br>' : ''}${u.phone ? esc(u.phone) : ''}${!u.email && !u.phone ? '—' : ''}
            </td>
            <td>${u.status === 1 ? '<span class="badge PUBLISHED">启用</span>' : '<span class="badge OFFLINE">停用</span>'}</td>
            <td class="muted nowrap">${fmtTime(u.createTime)}</td>
            <td class="actions">
                <button class="btn-link" data-act="edit" data-id="${u.id}">编辑</button>
                <button class="btn-link" data-act="toggle" data-id="${u.id}">${u.status === 1 ? '停用' : '启用'}</button>
                <button class="btn-link danger" data-act="delete" data-id="${u.id}">删除</button>
            </td>
        </tr>`).join('');
}

async function onClick(e) {
    const el = e.target.closest('[data-act]');
    if (!el) return;
    const id = Number(el.dataset.id);
    const user = state.records.find((u) => u.id === id);
    const act = el.dataset.act;

    if (act === 'edit') return openForm(user);
    if (act === 'toggle') {
        try {
            await api.put(`/api/users/${id}/status?status=${user.status === 1 ? 0 : 1}`);
            toast(user.status === 1 ? '已停用，该账号立即下线' : '已启用', 'ok');
            await load();
        } catch (err) {
            toast(err.message, 'err');
        }
        return;
    }
    if (act === 'delete') {
        if (id === me.id) return toast('不能删除自己', 'err');
        if (!await confirmDialog(`删除账号「${user.username}」？该账号将立即无法登录。`, { okText: '删除', danger: true })) return;
        try {
            await api.del(`/api/users/${id}`);
            toast('已删除', 'ok');
            await load();
        } catch (err) {
            toast(err.message, 'err');
        }
    }
}

function openForm(user) {
    const isEdit = !!user;
    const roleIds = new Set(user ? (user.roleIds || []) : []);
    const roleBoxes = state.roles.map((r) => `
        <label class="check" style="padding:6px 0">
            <input type="checkbox" value="${r.id}" ${roleIds.has(r.id) ? 'checked' : ''}>
            <span>${esc(r.roleName)} <span class="mono muted">${esc(r.roleCode)}</span></span>
        </label>`).join('');

    const m = modal({
        title: isEdit ? `编辑用户 ${user.username}` : '新增用户',
        body: `
            <div class="form-grid">
                <div class="field">
                    <label for="uf-username">用户名<span class="req">*</span></label>
                    <input id="uf-username" type="text" maxlength="20" ${isEdit ? 'disabled' : ''}
                           value="${esc(isEdit ? user.username : '')}" placeholder="4-20 位字母数字下划线">
                    ${isEdit ? '<div class="desc">用户名创建后不可修改，用于审计追踪。</div>' : ''}
                </div>
                <div class="field">
                    <label for="uf-nickname">昵称<span class="req">*</span></label>
                    <input id="uf-nickname" type="text" maxlength="50" value="${esc(isEdit ? (user.nickname || '') : '')}">
                </div>
                <div class="field">
                    <label for="uf-password">${isEdit ? '新密码' : '初始密码'}<span class="req">${isEdit ? '' : '*'}</span></label>
                    <input id="uf-password" type="password" maxlength="32"
                           placeholder="${isEdit ? '留空表示不修改' : '6-32 位'}">
                </div>
                <div class="field">
                    <label for="uf-status">状态</label>
                    <select id="uf-status">
                        <option value="1" ${!isEdit || user.status === 1 ? 'selected' : ''}>启用</option>
                        <option value="0" ${isEdit && user.status === 0 ? 'selected' : ''}>停用</option>
                    </select>
                </div>
                <div class="field">
                    <label for="uf-email">邮箱</label>
                    <input id="uf-email" type="email" maxlength="100" value="${esc(isEdit ? (user.email || '') : '')}">
                </div>
                <div class="field">
                    <label for="uf-phone">手机号</label>
                    <input id="uf-phone" type="text" maxlength="11" value="${esc(isEdit ? (user.phone || '') : '')}">
                </div>
                <div class="field full">
                    <label>角色</label>
                    <div style="display:grid;grid-template-columns:1fr 1fr">${roleBoxes}</div>
                </div>
            </div>`,
        footer: `<button class="btn" data-act="cancel">取消</button>
                 <button class="btn btn-primary" data-act="ok">${isEdit ? '保存' : '创建'}</button>`,
        wide: true,
    });

    m.root.querySelector('[data-act=cancel]').onclick = m.close;
    m.root.querySelector('[data-act=ok]').onclick = async () => {
        const username = m.root.querySelector('#uf-username').value.trim();
        const nickname = m.root.querySelector('#uf-nickname').value.trim();
        const password = m.root.querySelector('#uf-password').value;
        const email = m.root.querySelector('#uf-email').value.trim();
        const phone = m.root.querySelector('#uf-phone').value.trim();
        const status = Number(m.root.querySelector('#uf-status').value);
        const ids = [...m.root.querySelectorAll('input[type=checkbox]:checked')].map((c) => Number(c.value));

        if (!isEdit && !/^[A-Za-z0-9_]{4,20}$/.test(username)) return toast('用户名需为 4-20 位字母、数字或下划线', 'err');
        if (!nickname) return toast('昵称不能为空', 'err');
        if (!isEdit && password.length < 6) return toast('初始密码至少 6 位', 'err');
        if (isEdit && password && password.length < 6) return toast('新密码至少 6 位', 'err');

        const body = {
            username: isEdit ? null : username,
            nickname,
            password: password || null,
            email: email || null,
            phone: phone || null,
            status,
            roleIds: ids,
        };

        try {
            if (isEdit) await api.put(`/api/users/${user.id}`, body);
            else await api.post('/api/users', body);
            m.close();
            toast(isEdit ? '已保存' : '已创建', 'ok');
            await load();
        } catch (err) {
            toast(err.message, 'err');
        }
    };
}
