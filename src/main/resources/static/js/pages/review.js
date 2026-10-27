import { me, bootReady } from '../core/boot.js';
import { api } from '../core/api.js';
import { esc, fmtTime, toast, modal, confirmDialog, renderPager, loadingRow, emptyRow } from '../core/ui.js';
import { showArticleDetail } from '../components/article-detail.js';

bootReady.then(() => (me ? main() : null)).catch((e) => console.error('审核页初始化失败', e));

const state = { pageNum: 1, records: [], selected: new Set() };
const COLS = 6;

async function main() {
    document.getElementById('btn-refresh').onclick = () => load();
    document.getElementById('btn-batch-approve').onclick = batchApprove;
    document.getElementById('check-all').onchange = (e) => {
        state.records.forEach((r) => {
            if (r.authorId === me.id) return;   // 自己的稿件不参与批量
            if (e.target.checked) state.selected.add(r.id); else state.selected.delete(r.id);
        });
        document.querySelectorAll('#review-body [data-check]').forEach((c) => {
            if (!c.disabled) c.checked = e.target.checked;
        });
        sync();
    };
    document.getElementById('review-body').addEventListener('click', onClick);
    await load();
}

async function load() {
    const tbody = document.getElementById('review-body');
    tbody.innerHTML = loadingRow(COLS);
    state.selected.clear();
    sync();

    try {
        const page = await api.get('/api/articles', {
            status: 'PENDING', pageNum: state.pageNum, pageSize: 10, sort: 'createTime,asc',
        });
        state.records = page.records;
        document.getElementById('total').textContent = page.total;
        document.getElementById('pending-hint').textContent = `还有 ${page.total} 篇等你处理`;
        render(page.records);
        renderPager(document.getElementById('pager'), page, (n) => { state.pageNum = n; load(); });
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="${COLS}"><div class="empty">加载失败：${esc(e.message)}</div></td></tr>`;
    }
}

function render(rows) {
    const tbody = document.getElementById('review-body');
    if (!rows.length) {
        tbody.innerHTML = emptyRow(COLS, '没有待审核的稿件，这里是清的');
        return;
    }
    tbody.innerHTML = rows.map((a) => {
        const mine = a.authorId === me.id;
        return `<tr>
            <td class="col-check">
                <input type="checkbox" data-check="${a.id}" ${mine ? 'disabled title="不能审核自己的稿件"' : ''}>
            </td>
            <td class="title-cell">
                <a href="#" data-act="view" data-id="${a.id}">${esc(a.title)}</a>
                ${mine ? '<span class="badge grey" style="margin-left:6px">我提交的</span>' : ''}
                ${a.summary ? `<div class="muted ellipsis" style="font-size:12px">${esc(a.summary)}</div>` : ''}
            </td>
            <td class="nowrap">${esc(a.categoryName || '—')}</td>
            <td class="nowrap">${esc(a.authorName || '—')}</td>
            <td class="muted nowrap">${fmtTime(a.updateTime)}</td>
            <td class="actions">
                <button class="btn-link" data-act="view" data-id="${a.id}">读全文</button>
                ${mine ? '<span class="muted" style="font-size:12px">不能自审</span>' : `
                    <button class="btn-link" data-act="approve" data-id="${a.id}">通过</button>
                    <button class="btn-link danger" data-act="reject" data-id="${a.id}">驳回</button>`}
            </td>
        </tr>`;
    }).join('');

    tbody.querySelectorAll('[data-check]').forEach((c) => {
        c.onchange = () => {
            const id = Number(c.dataset.check);
            if (c.checked) state.selected.add(id); else state.selected.delete(id);
            sync();
        };
    });
}

function sync() {
    document.getElementById('sel-count').textContent = state.selected.size;
    document.getElementById('btn-batch-approve').disabled = state.selected.size === 0;
}

async function onClick(e) {
    const el = e.target.closest('[data-act]');
    if (!el) return;
    e.preventDefault();
    const id = Number(el.dataset.id);

    if (el.dataset.act === 'view') return showArticleDetail(id);
    if (el.dataset.act === 'approve') {
        if (await confirmDialog('通过后文章立即出现在前台。', { okText: '通过' })) {
            await call(id, 'approve', '已通过并发布');
        }
        return;
    }
    if (el.dataset.act === 'reject') {
        promptOpinion((opinion) => call(id, 'reject', '已驳回', { opinion }));
    }
}

function promptOpinion(onOk) {
    const m = modal({
        title: '驳回稿件',
        body: `<div class="field">
                   <label>审核意见<span class="req">*</span></label>
                   <textarea id="opinion" rows="4" placeholder="写具体一点：哪里有问题、应该怎么改。作者会看到这段话。"></textarea>
                   <div class="desc">例如：第三段的引用没有出处，请补充后可重新提交。</div>
               </div>`,
        footer: `<button class="btn" data-act="cancel">取消</button>
                 <button class="btn btn-primary" data-act="ok">确认驳回</button>`,
    });
    m.root.querySelector('[data-act=cancel]').onclick = m.close;
    m.root.querySelector('[data-act=ok]').onclick = () => {
        const v = m.root.querySelector('#opinion').value.trim();
        if (!v) { toast('驳回必须填写意见', 'err'); return; }
        m.close();
        onOk(v);
    };
}

async function call(id, act, okMsg, body) {
    try {
        await api.post(`/api/articles/${id}/${act}`, body || {});
        toast(okMsg, 'ok');
        await load();
    } catch (e) {
        toast(e.message, 'err');
    }
}

async function batchApprove() {
    const ids = [...state.selected];
    if (!ids.length) return;
    if (!await confirmDialog(`通过选中的 ${ids.length} 篇稿件？失败的会逐条说明原因。`, { okText: '批量通过' })) return;
    try {
        const r = await api.post('/api/articles/batch/approve', { ids, opinion: null });
        const failedIds = r.failed || [];
        if (!failedIds.length) {
            toast(`批量通过完成：成功 ${r.success} 篇`, 'ok');
        } else {
            const reasons = r.reasons || {};
            modal({
                title: '批量审核结果',
                body: `<p style="margin-top:0">成功 <b>${r.success}</b> 篇，失败 <b style="color:var(--danger)">${failedIds.length}</b> 篇。</p>
                       <ul style="font-size:13px;line-height:1.8">${failedIds.map((id) =>
                    `<li>#${id}　${esc(reasons[id] || reasons[String(id)] || '')}</li>`).join('')}</ul>`,
            });
        }
        await load();
    } catch (e) {
        toast(e.message, 'err');
    }
}
