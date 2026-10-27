import { me, bootReady } from '../core/boot.js';
import { api } from '../core/api.js';
import { esc, toast, modal, confirmDialog } from '../core/ui.js';

// 顶层 await 会让下面的 let roles/perms 停在暂时性死区
bootReady.then(() => (me ? main() : null)).catch((e) => console.error('角色页初始化失败', e));

let roles = [];
let perms = [];
let currentId = null;

async function main() {
    document.getElementById('btn-add-role').onclick = () => openRoleForm(null);
    document.getElementById('btn-save-perm').onclick = savePermissions;
    document.getElementById('role-list').addEventListener('click', onPick);
    try {
        [roles, perms] = await Promise.all([
            api.get('/api/roles'),
            api.get('/api/roles/permissions'),
        ]);
    } catch (e) {
        document.getElementById('role-list').innerHTML = `<div class="empty">加载失败：${esc(e.message)}</div>`;
        return;
    }
    renderRoleList();
    if (roles.length) select(roles[0].id);
}

function renderRoleList() {
    document.getElementById('role-list').innerHTML = roles.map((r) => `
        <div class="role-item ${r.id === currentId ? 'active' : ''}" data-id="${r.id}">
            <div>
                <div class="rn">${esc(r.roleName)}</div>
                <div class="rc">${esc(r.roleCode)}</div>
            </div>
            <div class="n">${r.userCount} 人</div>
        </div>`).join('');
}

function onPick(e) {
    const item = e.target.closest('[data-id]');
    if (!item) return;
    select(Number(item.dataset.id));
}

function select(id) {
    currentId = id;
    renderRoleList();
    const role = roles.find((r) => r.id === id);
    if (!role) return;

    document.getElementById('perm-title').innerHTML =
        `${esc(role.roleName)} <span class="badge grey" style="margin-left:8px">${esc(role.roleCode)}</span>`;
    document.getElementById('btn-save-perm').style.display = '';

    const groups = new Map();
    perms.forEach((p) => {
        const g = p.permGroup || '其他';
        if (!groups.has(g)) groups.set(g, []);
        groups.get(g).push(p);
    });

    const checked = new Set(role.permIds || []);
    let html = `<div style="display:flex;gap:10px;align-items:center;margin-bottom:16px;flex-wrap:wrap">
            <span class="muted" style="font-size:12.5px">${esc(role.description || '没有描述')}</span>
            <span class="spacer" style="flex:1"></span>
            <span class="muted" style="font-size:12.5px">${role.userCount} 个用户使用此角色</span>
            <button class="btn btn-sm" data-act="edit-role">编辑资料</button>
            ${role.roleCode === 'ADMIN' ? '<span class="badge grey">内置角色，不可删除</span>'
            : '<button class="btn btn-sm btn-danger" data-act="del-role">删除角色</button>'}
        </div>`;

    for (const [group, list] of groups) {
        html += `<div class="perm-group"><h4>${esc(group)}</h4><div class="perm-grid">` +
            list.map((p) => `
                <label class="perm">
                    <input type="checkbox" value="${p.id}" ${checked.has(p.id) ? 'checked' : ''}>
                    <span>
                        <span class="pn">${esc(p.permName)}</span><br>
                        <span class="pc">${esc(p.permCode)}</span>
                    </span>
                </label>`).join('') + '</div></div>';
    }

    if (role.roleCode === 'ADMIN') {
        html += `<div style="margin-top:8px;padding:10px 12px;border-radius:9px;background:var(--warn-soft);color:var(--warn);font-size:12.5px">
            管理员角色必须保留「角色管理」，否则系统里将没人能再调整权限。</div>`;
    }

    const body = document.getElementById('perm-body');
    body.innerHTML = html;
    body.querySelector('[data-act=edit-role]').onclick = () => openRoleForm(role);
    const del = body.querySelector('[data-act=del-role]');
    if (del) del.onclick = () => deleteRole(role);
}

async function savePermissions() {
    const role = roles.find((r) => r.id === currentId);
    if (!role) return;
    const ids = [...document.querySelectorAll('#perm-body input[type=checkbox]:checked')]
        .map((c) => Number(c.value));
    try {
        await api.put(`/api/roles/${role.id}/permissions`, { permIds: ids });
        toast('权限已保存，对下一个请求立即生效', 'ok');
        await reloadRoles();
    } catch (e) {
        toast(e.message, 'err');
    }
}

async function reloadRoles() {
    try {
        roles = await api.get('/api/roles');
        renderRoleList();
        select(currentId);
    } catch (e) {
        toast(e.message, 'err');
    }
}

function openRoleForm(role) {
    const isEdit = !!role;
    const m = modal({
        title: isEdit ? `编辑角色 ${role.roleName}` : '新增角色',
        body: `
            <div class="form-grid">
                <div class="field">
                    <label for="rf-code">角色编码<span class="req">*</span></label>
                    <input id="rf-code" type="text" maxlength="50" ${isEdit ? 'disabled' : ''}
                           value="${esc(isEdit ? role.roleCode : '')}" placeholder="例如 EDITOR">
                    ${isEdit ? '<div class="desc">编码是代码里引用的标识，创建后不可修改。</div>' : ''}
                </div>
                <div class="field">
                    <label for="rf-name">角色名称<span class="req">*</span></label>
                    <input id="rf-name" type="text" maxlength="50" value="${esc(isEdit ? role.roleName : '')}">
                </div>
                <div class="field">
                    <label for="rf-sort">排序</label>
                    <input id="rf-sort" type="number" value="${isEdit ? role.sort : 100}" min="0">
                </div>
                <div class="field">
                    <label for="rf-status">状态</label>
                    <select id="rf-status">
                        <option value="1" ${!isEdit || role.status === 1 ? 'selected' : ''}>启用</option>
                        <option value="0" ${isEdit && role.status === 0 ? 'selected' : ''}>停用</option>
                    </select>
                </div>
                <div class="field full">
                    <label for="rf-desc">描述</label>
                    <input id="rf-desc" type="text" maxlength="200" value="${esc(isEdit ? (role.description || '') : '')}">
                </div>
            </div>`,
        footer: `<button class="btn" data-act="cancel">取消</button>
                 <button class="btn btn-primary" data-act="ok">${isEdit ? '保存' : '创建'}</button>`,
    });

    m.root.querySelector('[data-act=cancel]').onclick = m.close;
    m.root.querySelector('[data-act=ok]').onclick = async () => {
        const body = {
            roleCode: m.root.querySelector('#rf-code').value.trim(),
            roleName: m.root.querySelector('#rf-name').value.trim(),
            sort: Number(m.root.querySelector('#rf-sort').value) || 100,
            status: Number(m.root.querySelector('#rf-status').value),
            description: m.root.querySelector('#rf-desc').value.trim() || null,
        };
        if (!body.roleName) return toast('角色名称不能为空', 'err');
        if (!isEdit && !/^[A-Z_]{2,20}$/.test(body.roleCode)) {
            return toast('角色编码为 2-20 位大写字母或下划线，例如 EDITOR', 'err');
        }
        try {
            if (isEdit) await api.put(`/api/roles/${role.id}`, body);
            else {
                const id = await api.post('/api/roles', body);
                currentId = id;
            }
            m.close();
            toast(isEdit ? '已保存' : '已创建，记得勾选权限', 'ok');
            await reloadRoles();
        } catch (e) {
            toast(e.message, 'err');
        }
    };
}

async function deleteRole(role) {
    if (!await confirmDialog(`删除角色「${role.roleName}」？仍被用户使用时会被拒绝。`, { okText: '删除', danger: true })) return;
    try {
        await api.del(`/api/roles/${role.id}`);
        toast('已删除', 'ok');
        currentId = null;
        roles = await api.get('/api/roles');
        renderRoleList();
        if (roles.length) select(roles[0].id);
    } catch (e) {
        toast(e.message, 'err');
    }
}
