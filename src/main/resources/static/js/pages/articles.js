import { me, bootReady, can } from '../core/boot.js';
import { api } from '../core/api.js';
import {
    esc, fmtTime, statusBadge, toast, modal,
    confirmDialog, renderPager, loadingRow, emptyRow, debounce,
} from '../core/ui.js';
import { showArticleDetail } from '../components/article-detail.js';

// 不能写成顶层 `await bootReady; if (me) main();`：模块求值到 await 时，
// 下面的 const state/COLS 还没初始化，main() 一读就是暂时性死区（ReferenceError）。
// 用 then 把启动排到本次模块求值结束之后，顺带把初始化异常打到控制台而不是静默吞掉。
bootReady.then(() => (me ? main() : null)).catch((e) => console.error('文章列表初始化失败', e));

const state = { pageNum: 1, pageSize: 10, records: [], page: null, selected: new Set() };
const COLS = 7;

async function main() {
    bindToolbar();
    await loadCategories();
    await load();
}

/* ---------------- 筛选 ---------------- */

function bindToolbar() {
    const q = new URLSearchParams(location.search);
    if (q.get('status')) document.getElementById('f-status').value = q.get('status');
    if (q.get('mine') === '1') document.getElementById('f-mine').checked = true;

    document.getElementById('btn-search').onclick = () => { state.pageNum = 1; load(); };
    document.getElementById('btn-reset').onclick = () => {
        document.getElementById('article-keyword').value = '';
        document.getElementById('f-status').value = '';
        document.getElementById('f-category').value = '';
        document.getElementById('f-sort').value = 'updateTime desc';
        document.getElementById('f-mine').checked = false;
        state.pageNum = 1;
        load();
    };
    document.getElementById('f-status').onchange = () => { state.pageNum = 1; load(); };
    document.getElementById('f-category').onchange = () => { state.pageNum = 1; load(); };
    document.getElementById('f-sort').onchange = () => load();
    document.getElementById('f-mine').onchange = () => { state.pageNum = 1; load(); };
    document.getElementById('article-keyword').oninput = debounce(() => { state.pageNum = 1; load(); }, 350);

    document.getElementById('check-all').onchange = (e) => {
        state.records.forEach((r) => {
            if (e.target.checked) state.selected.add(r.id); else state.selected.delete(r.id);
        });
        document.querySelectorAll('#article-body [data-check]').forEach((c) => { c.checked = e.target.checked; });
        syncBulkBar();
    };

    document.querySelectorAll('[data-bulk]').forEach((b) => {
        b.onclick = () => onBulk(b.dataset.bulk);
    });

    document.getElementById('article-body').addEventListener('click', onRowAction);
}

async function loadCategories() {
    try {
        const list = await api.get('/api/categories');
        const sel = document.getElementById('f-category');
        sel.innerHTML = '<option value="">全部分类</option>' + list.map((c) =>
            `<option value="${c.id}">${'　'.repeat(Math.max(0, (c.level || 1) - 1))}${esc(c.name)}</option>`
        ).join('');
    } catch (e) {
        // 分类筛选是辅助功能，失败不阻塞列表
    }
}

/* ---------------- 列表 ---------------- */

async function load() {
    const tbody = document.getElementById('article-body');
    tbody.innerHTML = loadingRow(COLS);
    state.selected.clear();
    document.getElementById('check-all').checked = false;
    syncBulkBar();

    // 后端只认 "字段,方向" 这种一段式写法；orderBy 是 Service 内部字段，不让客户端传
    const [sort, orderBy] = document.getElementById('f-sort').value.split(' ');
    const params = {
        pageNum: state.pageNum,
        pageSize: state.pageSize,
        keyword: document.getElementById('article-keyword').value.trim(),
        status: document.getElementById('f-status').value,
        categoryId: document.getElementById('f-category').value,
        mine: document.getElementById('f-mine').checked ? 1 : '',
        sort: orderBy ? `${sort},${orderBy}` : sort,
    };

    try {
        const page = await api.get('/api/articles', params);
        state.page = page;
        state.records = page.records;
        renderRows(page.records);
        renderPager(document.getElementById('pager'), page, (n) => { state.pageNum = n; load(); });
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="${COLS}"><div class="empty">加载失败：${esc(e.message)}</div></td></tr>`;
        toast(e.message, 'err');
    }
}

function renderRows(rows) {
    const tbody = document.getElementById('article-body');
    if (!rows.length) {
        tbody.innerHTML = emptyRow(COLS, '这个筛选条件下没有稿件');
        return;
    }
    tbody.innerHTML = rows.map((a) => `
        <tr>
            <td class="col-check"><input type="checkbox" data-check="${a.id}" ${state.selected.has(a.id) ? 'checked' : ''}></td>
            <td class="title-cell">
                <a href="#" data-act="view" data-id="${a.id}">${esc(a.title)}</a>
                ${a.summary ? `<div class="muted ellipsis" style="font-size:12px">${esc(a.summary)}</div>` : ''}
            </td>
            <td class="nowrap">${esc(a.categoryName || '—')}</td>
            <td>${statusBadge(a.status)}</td>
            <td class="nowrap">${esc(a.authorName || '—')}</td>
            <td class="muted nowrap">${fmtTime(a.updateTime)}</td>
            <td class="actions">${actionsOf(a)}</td>
        </tr>`).join('');

    tbody.querySelectorAll('[data-check]').forEach((c) => {
        c.onchange = () => {
            const id = Number(c.dataset.check);
            if (c.checked) state.selected.add(id); else state.selected.delete(id);
            syncBulkBar();
        };
    });
}

function actionsOf(a) {
    const out = [];
    const mine = a.authorId === me.id;
    const editable = (a.status === 'DRAFT' || a.status === 'REJECTED')
        && (mine || can('article:update:any'));

    if (editable) out.push(`<a class="btn-link" href="/app/article-edit?id=${a.id}">编辑</a>`);
    if (a.status === 'DRAFT' && (mine || can('article:update:any'))) {
        out.push(btn('submit', a.id, '提交审核'));
        out.push(btn('delete', a.id, '删除', 'danger'));
    }
    if (a.status === 'PENDING' && (mine || can('article:update:any'))) {
        out.push(btn('withdraw', a.id, '撤回'));
    }
    if (a.status === 'REJECTED' && (mine || can('article:delete:any'))) {
        out.push(btn('delete', a.id, '删除', 'danger'));
    }
    if (a.status === 'PUBLISHED') {
        out.push(`<a class="btn-link" href="/portal/article/${a.id}" target="_blank" rel="noopener">前台</a>`);
        if (can('article:publish')) out.push(btn('offline', a.id, '下架', 'danger'));
    }
    if (a.status === 'OFFLINE' && (mine || can('article:update:any'))) {
        out.push(btn('resubmit', a.id, '重新提交'));
    }
    if (a.status === 'PENDING' && can('article:audit') && !mine) {
        out.push(btn('approve', a.id, '通过'));
        out.push(btn('reject', a.id, '驳回', 'danger'));
    }
    return out.join('') || '<span class="muted">—</span>';
}

function btn(act, id, label, cls = '') {
    return `<button class="btn-link ${cls}" data-act="${act}" data-id="${id}">${label}</button>`;
}

/* ---------------- 行内操作 ---------------- */

async function onRowAction(e) {
    const el = e.target.closest('[data-act]');
    if (!el) return;
    e.preventDefault();
    const id = Number(el.dataset.id);
    const article = state.records.find((r) => r.id === id);
    const act = el.dataset.act;

    if (act === 'view') return showArticleDetail(id);
    if (act === 'submit') return doAction(id, 'submit', '已提交审核，等待审核员处理');
    if (act === 'resubmit') return doAction(id, 'resubmit', '已重新提交审核');
    if (act === 'withdraw') {
        if (await confirmDialog('撤回后文章回到草稿状态，需要重新提交。', { okText: '撤回' })) {
            doAction(id, 'withdraw', '已撤回为草稿');
        }
        return;
    }
    if (act === 'delete') {
        const extra = article && article.status === 'REJECTED'
            ? '这篇文章已被驳回，删除后不可恢复。' : '删除后不可恢复。';
        if (await confirmDialog(extra, { okText: '删除', danger: true })) {
            doAction(id, 'delete', '已删除');
        }
        return;
    }
    if (act === 'approve') {
        if (await confirmDialog('通过后文章立即出现在前台。', { okText: '通过' })) {
            doAction(id, 'approve', '已通过并发布', { opinion: null });
        }
        return;
    }
    if (act === 'reject') return promptText('驳回文章', '审核意见', '写清楚问题，作者能看到', (opinion) =>
        doAction(id, 'reject', '已驳回', { opinion }));
    if (act === 'offline') return promptText('下架文章', '下架原因', '原因会记录在文章上', (reason) =>
        doAction(id, 'offline', '已下架', { reason }));
}

async function doAction(id, act, okMsg, body) {
    try {
        const url = `/api/articles/${id}/${act}`;
        if (act === 'delete') await api.del(url);
        else await api.post(url, body === undefined ? {} : body);
        toast(okMsg, 'ok');
        state.pageNum = Math.max(1, state.pageNum);
        await load();
    } catch (e) {
        toast(e.message, 'err');
    }
}

function promptText(title, label, hint, onOk) {
    const m = modal({
        title,
        body: `<div class="field"><label>${esc(label)}<span class="req">*</span></label>
               <textarea id="prompt-text" rows="3" placeholder="${esc(hint)}"></textarea></div>`,
        footer: `<button class="btn" data-act="cancel">取消</button>
                 <button class="btn btn-primary" data-act="ok">确定</button>`,
    });
    m.root.querySelector('[data-act=cancel]').onclick = m.close;
    m.root.querySelector('[data-act=ok]').onclick = () => {
        const v = m.root.querySelector('#prompt-text').value.trim();
        if (!v) { toast('这一项必须填写', 'err'); return; }
        m.close();
        onOk(v);
    };
}

/* ---------------- 批量 ---------------- */

function syncBulkBar() {
    const bar = document.getElementById('bulk-bar');
    const n = state.selected.size;
    document.getElementById('bulk-count').textContent = n;
    bar.style.display = n ? 'flex' : 'none';
}

async function onBulk(kind) {
    if (kind === 'clear') {
        state.selected.clear();
        document.querySelectorAll('#article-body [data-check]').forEach((c) => { c.checked = false; });
        document.getElementById('check-all').checked = false;
        syncBulkBar();
        return;
    }
    const ids = [...state.selected];
    if (!ids.length) return;

    if (kind === 'approve') {
        if (!await confirmDialog(`将 ${ids.length} 篇稿件标记为已发布？只有待审核状态的成功，其余会逐条返回失败原因。`, { okText: '批量通过' })) return;
        await runBatch('/api/articles/batch/approve', { ids, opinion: null }, '批量通过');
    }
    if (kind === 'offline') {
        promptText('批量下架', '下架原因', '同一条原因会记录到每一篇文章', async (reason) => {
            await runBatch('/api/articles/batch/offline', { ids, reason }, '批量下架');
        });
    }
}

async function runBatch(url, body, label) {
    try {
        const r = await api.post(url, body);
        // failed 是"失败 id 的数组"，reasons 是 {id: 原因}；空数组也是真值，不能用 if (!r.failed) 判断成功
        const failedIds = r.failed || [];
        if (!failedIds.length) {
            toast(`${label}完成：成功 ${r.success} 篇`, 'ok');
        } else {
            const reasons = r.reasons || {};
            const items = failedIds.map((id) => `<li>#${id}　${esc(reasons[id] || reasons[String(id)] || '')}</li>`).join('');
            modal({
                title: `${label}结果`,
                body: `<p style="margin-top:0">成功 <b>${r.success}</b> 篇，失败 <b style="color:var(--danger)">${failedIds.length}</b> 篇。</p>
                       <ul style="font-size:13px;color:var(--ink-2);line-height:1.8">${items}</ul>`,
            });
        }
        state.selected.clear();
        await load();
    } catch (e) {
        toast(e.message, 'err');
    }
}
