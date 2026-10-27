import { me, bootReady } from '../core/boot.js';
import { api } from '../core/api.js';
import { esc, fmtTime, toast, modal, renderPager, loadingRow, emptyRow, debounce } from '../core/ui.js';

// 顶层 await 会让下面的 TYPE_LABEL/state 停在暂时性死区
bootReady.then(() => (me ? main() : null)).catch((e) => console.error('日志页初始化失败', e));

const TYPE_LABEL = {
    LOGIN: '登录', ARTICLE: '文章', AUDIT: '审核',
    CATEGORY: '分类', USER: '用户', ROLE: '角色', AI_QUERY: '引导问答',
};
const RESULT_LABEL = { SUCCESS: '成功', DENIED: '被拦截', FAIL: '失败' };

const state = { pageNum: 1, records: [] };
const COLS = 8;

async function main() {
    document.getElementById('btn-search').onclick = () => { state.pageNum = 1; load(); };
    document.getElementById('btn-reset').onclick = () => {
        ['f-keyword', 'f-type', 'f-result', 'f-start', 'f-end'].forEach((id) => {
            document.getElementById(id).value = '';
        });
        state.pageNum = 1;
        load();
    };
    document.getElementById('f-type').onchange = () => { state.pageNum = 1; load(); };
    document.getElementById('f-result').onchange = () => { state.pageNum = 1; load(); };
    document.getElementById('f-keyword').oninput = debounce(() => { state.pageNum = 1; load(); }, 350);
    document.getElementById('log-body').addEventListener('click', onClick);
    await load();
}

async function load() {
    const tbody = document.getElementById('log-body');
    tbody.innerHTML = loadingRow(COLS);
    try {
        const page = await api.get('/api/logs', {
            pageNum: state.pageNum,
            pageSize: 15,
            keyword: document.getElementById('f-keyword').value.trim(),
            logType: document.getElementById('f-type').value,
            result: document.getElementById('f-result').value,
            startDate: document.getElementById('f-start').value,
            endDate: document.getElementById('f-end').value,
        });
        state.records = page.records;
        render(page.records);
        renderPager(document.getElementById('pager'), page, (n) => { state.pageNum = n; load(); });
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="${COLS}"><div class="empty">加载失败：${esc(e.message)}</div></td></tr>`;
    }
}

function render(rows) {
    const tbody = document.getElementById('log-body');
    if (!rows.length) {
        tbody.innerHTML = emptyRow(COLS, '没有符合条件的记录');
        return;
    }
    tbody.innerHTML = rows.map((l) => `
        <tr>
            <td class="muted nowrap mono">${fmtTime(l.createTime, true)}</td>
            <td class="nowrap">${esc(l.username || '—')}</td>
            <td><span class="badge grey">${esc(TYPE_LABEL[l.logType] || l.logType)}</span></td>
            <td>${esc(l.operation)}</td>
            <td><span class="badge ${esc(l.result)}">${esc(RESULT_LABEL[l.result] || l.result)}</span></td>
            <td class="num">${l.costMs === null || l.costMs === undefined ? '—' : l.costMs + ' ms'}</td>
            <td class="muted mono">${esc(l.ip || '—')}</td>
            <td class="actions"><button class="btn-link" data-id="${l.id}">查看</button></td>
        </tr>`).join('');
}

function onClick(e) {
    const el = e.target.closest('[data-id]');
    if (!el) return;
    const log = state.records.find((r) => r.id === Number(el.dataset.id));
    if (log) showDetail(log);
}

function showDetail(l) {
    let params = l.params || '';
    try {
        params = JSON.stringify(JSON.parse(l.params), null, 2);
    } catch (e) { /* 不是 JSON 就原样展示 */ }

    modal({
        title: '操作详情',
        wide: true,
        body: `
            <dl class="kv">
                <dt>时间</dt><dd class="mono">${fmtTime(l.createTime, true)}</dd>
                <dt>操作人</dt><dd>${esc(l.username || '—')}（id=${esc(l.userId ?? '—')}）</dd>
                <dt>类型</dt><dd>${esc(TYPE_LABEL[l.logType] || l.logType)}</dd>
                <dt>操作</dt><dd>${esc(l.operation)}</dd>
                <dt>结果</dt><dd><span class="badge ${esc(l.result)}">${esc(RESULT_LABEL[l.result] || l.result)}</span>
                    ${l.errorMsg ? `<span style="color:var(--danger);margin-left:8px">${esc(l.errorMsg)}</span>` : ''}</dd>
                <dt>接口</dt><dd class="mono">${esc(l.httpMethod || '')} ${esc(l.uri || '—')}</dd>
                <dt>方法</dt><dd class="mono">${esc(l.method || '—')}</dd>
                <dt>耗时</dt><dd>${l.costMs ?? '—'} ms</dd>
                <dt>IP</dt><dd class="mono">${esc(l.ip || '—')}</dd>
            </dl>
            <div style="margin-top:16px">
                <div class="lbl" style="font-size:11px;color:#9aa3b2;margin-bottom:6px">入参（已脱敏）</div>
                <pre class="code">${esc(params || '无')}</pre>
            </div>`,
    });
}
