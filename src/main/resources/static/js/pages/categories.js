import { me, bootReady } from '../core/boot.js';
import { api } from '../core/api.js';
import { esc, toast, modal, confirmDialog } from '../core/ui.js';

// 顶层 await 会让下面的 let flat 停在暂时性死区
bootReady.then(() => (me ? main() : null)).catch((e) => console.error('分类页初始化失败', e));

let flat = [];

async function main() {
    document.getElementById('btn-add-root').onclick = () => openForm(null, 0);
    document.getElementById('cat-body').addEventListener('click', onClick);
    await load();
}

async function load() {
    const tbody = document.getElementById('cat-body');
    try {
        const roots = await api.get('/api/categories/tree');
        flat = [];
        const walk = (nodes, depth) => nodes.forEach((n) => {
            flat.push(n);
            if (n.children && n.children.length) walk(n.children, depth + 1);
        });
        walk(roots, 0);

        if (!flat.length) {
            tbody.innerHTML = `<tr><td colspan="6"><div class="empty"><div class="big">◌</div>还没有分类</div></td></tr>`;
            return;
        }
        tbody.innerHTML = flat.map((c) => `
            <tr class="cat-row ${c.status !== 1 ? 'hidden' : ''}">
                <td>
                    <span class="indent" style="padding-left:${(c.level - 1) * 22}px"></span>
                    <span class="lv">${c.level}</span>
                    <span class="name">${esc(c.name)}</span>
                </td>
                <td class="muted">L${c.level}</td>
                <td class="num">${c.sort}</td>
                <td>${c.status === 1
            ? '<span class="badge PUBLISHED">启用</span>'
            : '<span class="badge OFFLINE">停用</span>'}</td>
                <td class="muted ellipsis">${esc(c.description || '—')}</td>
                <td class="actions">
                    ${c.level < 3 ? `<button class="btn-link" data-act="add" data-id="${c.id}">添加子分类</button>` : ''}
                    <button class="btn-link" data-act="edit" data-id="${c.id}">编辑</button>
                    <button class="btn-link" data-act="toggle" data-id="${c.id}">${c.status === 1 ? '停用' : '启用'}</button>
                    <button class="btn-link danger" data-act="delete" data-id="${c.id}">删除</button>
                </td>
            </tr>`).join('');
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="6"><div class="empty">加载失败：${esc(e.message)}</div></td></tr>`;
    }
}

async function onClick(e) {
    const el = e.target.closest('[data-act]');
    if (!el) return;
    const id = Number(el.dataset.id);
    const cat = flat.find((c) => c.id === id);
    const act = el.dataset.act;

    if (act === 'add') return openForm(null, id);
    if (act === 'edit') return openForm(cat, cat.parentId);
    if (act === 'toggle') {
        try {
            await api.put(`/api/categories/${id}/status?status=${cat.status === 1 ? 0 : 1}`);
            toast(cat.status === 1 ? '已停用' : '已启用', 'ok');
            await load();
        } catch (err) {
            toast(err.message, 'err');
        }
        return;
    }
    if (act === 'delete') {
        if (!await confirmDialog(`删除分类「${cat.name}」？有子分类或文章时会被拒绝。`, { okText: '删除', danger: true })) return;
        try {
            await api.del(`/api/categories/${id}`);
            toast('已删除', 'ok');
            await load();
        } catch (err) {
            toast(err.message, 'err');
        }
    }
}

/** cat 为空表示新增；parentId 决定挂在谁下面（0 = 根） */
function openForm(cat, parentId) {
    const parent = parentId ? flat.find((c) => c.id === parentId) : null;
    const isEdit = !!cat;
    const m = modal({
        title: isEdit ? '编辑分类' : (parent ? `在「${parent.name}」下新增` : '新增一级分类'),
        body: `
            <div class="form-grid">
                <div class="field full">
                    <label>上级分类</label>
                    <input type="text" value="${esc(parent ? parent.name : '根节点（一级分类）')}" disabled>
                    ${isEdit ? '<div class="desc">分类不支持更换上级，需要移动请新建后迁移文章。</div>' : ''}
                </div>
                <div class="field">
                    <label for="cf-name">分类名称<span class="req">*</span></label>
                    <input id="cf-name" type="text" maxlength="50" value="${esc(cat ? cat.name : '')}" placeholder="例如：后端">
                </div>
                <div class="field">
                    <label for="cf-sort">排序</label>
                    <input id="cf-sort" type="number" value="${cat ? cat.sort : 100}" min="0" max="99999">
                    <div class="desc">同级里数字小的排前面。</div>
                </div>
                <div class="field">
                    <label for="cf-status">状态</label>
                    <select id="cf-status">
                        <option value="1" ${!cat || cat.status === 1 ? 'selected' : ''}>启用</option>
                        <option value="0" ${cat && cat.status === 0 ? 'selected' : ''}>停用</option>
                    </select>
                </div>
                <div class="field full">
                    <label for="cf-desc">说明</label>
                    <input id="cf-desc" type="text" maxlength="200" value="${esc(cat ? (cat.description || '') : '')}" placeholder="选填">
                </div>
            </div>`,
        footer: `<button class="btn" data-act="cancel">取消</button>
                 <button class="btn btn-primary" data-act="ok">${isEdit ? '保存' : '创建'}</button>`,
    });

    m.root.querySelector('[data-act=cancel]').onclick = m.close;
    m.root.querySelector('[data-act=ok]').onclick = async () => {
        const name = m.root.querySelector('#cf-name').value.trim();
        if (!name) { toast('分类名称不能为空', 'err'); return; }
        const body = {
            parentId: parentId || 0,
            name,
            sort: Number(m.root.querySelector('#cf-sort').value) || 100,
            status: Number(m.root.querySelector('#cf-status').value),
            description: m.root.querySelector('#cf-desc').value.trim() || null,
        };
        try {
            if (isEdit) await api.put(`/api/categories/${cat.id}`, body);
            else await api.post('/api/categories', body);
            m.close();
            toast(isEdit ? '已保存' : '已创建', 'ok');
            await load();
        } catch (err) {
            toast(err.message, 'err');
        }
    };
}
