import { me, bootReady, can } from '../core/boot.js';
import { api } from '../core/api.js';
import { esc, fmtTime, toast, statusLabel } from '../core/ui.js';

// 顶层 await 会让下面的 const/let 停在暂时性死区，main() 里读它们必抛 ReferenceError
bootReady.then(() => (me ? main() : null)).catch((e) => console.error('写稿页初始化失败', e));

const articleId = new URLSearchParams(location.search).get('id');
let version = null;
let editable = true;

async function main() {
    bindEditor();
    await loadCategories();
    if (articleId) {
        await loadArticle();
    } else {
        document.getElementById('meta-author').textContent = me.nickname || me.username;
    }
}

/* ---------------- 分类 ---------------- */

async function loadCategories() {
    const sel = document.getElementById('article-category');
    try {
        const roots = await api.get('/api/categories/tree');
        const flat = [];
        const walk = (nodes) => nodes.forEach((n) => {
            flat.push(n);
            if (n.children && n.children.length) walk(n.children);
        });
        walk(roots);
        sel.innerHTML = '<option value="">请选择分类</option>' + flat.map((c) =>
            `<option value="${c.id}" ${c.status !== 1 ? 'disabled' : ''}>${'　'.repeat((c.level || 1) - 1)}${esc(c.name)}${c.status !== 1 ? '（已停用）' : ''}</option>`
        ).join('');
    } catch (e) {
        sel.innerHTML = '<option value="">分类加载失败</option>';
        toast('分类加载失败：' + e.message, 'err');
    }
}

/* ---------------- 载入已有文章 ---------------- */

async function loadArticle() {
    try {
        const a = await api.get(`/api/articles/${articleId}`);
        version = a.version;

        document.getElementById('page-title').textContent = '编辑文章';
        document.getElementById('article-title').value = a.title || '';
        document.getElementById('article-summary').value = a.summary || '';
        document.getElementById('article-content').value = a.content || '';
        document.getElementById('article-category').value = String(a.categoryId || '');
        document.getElementById('meta-status').textContent = statusLabel(a.status);
        document.getElementById('meta-author').textContent = a.authorName || '—';
        document.getElementById('meta-update').textContent = fmtTime(a.updateTime);
        updateWordCount();

        const mine = a.authorId === me.id;
        editable = (a.status === 'DRAFT' || a.status === 'REJECTED') && (mine || can('article:update:any'));
        if (!editable) {
            lockForm(`这篇文章当前是「${statusLabel(a.status)}」，不能编辑。
                已发布的请先下架；待审核的请先撤回；已下架的可以重新提交审核。`);
        }

        if (a.status === 'REJECTED' && a.auditOpinion) {
            showNotice(`审核意见：${a.auditOpinion}`, 'danger');
        } else if (a.status === 'OFFLINE' && a.offlineReason) {
            showNotice(`下架原因：${a.offlineReason}`, 'warn');
        }
    } catch (e) {
        toast('文章加载失败：' + e.message, 'err');
        lockForm('文章加载失败：' + e.message);
    }
}

function showNotice(text, kind) {
    const n = document.getElementById('notice');
    n.textContent = text;
    n.style.display = 'block';
    n.style.background = kind === 'danger' ? 'var(--danger-soft)' : 'var(--warn-soft)';
    n.style.color = kind === 'danger' ? 'var(--danger)' : 'var(--warn)';
}

function lockForm(reason) {
    ['article-title', 'article-summary', 'article-content', 'article-category', 'btn-save', 'btn-save-submit']
        .forEach((id) => { const el = document.getElementById(id); if (el) el.disabled = true; });
    showNotice(reason, 'warn');
}

/* ---------------- 编辑 ---------------- */

function bindEditor() {
    const content = document.getElementById('article-content');
    content.oninput = updateWordCount;

    document.getElementById('btn-save').onclick = () => save(false);
    document.getElementById('btn-save-submit').onclick = () => save(true);
}

function updateWordCount() {
    const n = document.getElementById('article-content').value.replace(/\s/g, '').length;
    document.getElementById('word-count').textContent = `${n} 字`;
}

async function save(thenSubmit) {
    if (!editable) return;
    const title = document.getElementById('article-title').value.trim();
    const summary = document.getElementById('article-summary').value.trim();
    const content = document.getElementById('article-content').value;
    const categoryId = document.getElementById('article-category').value;

    if (!title) return toast('标题不能为空', 'err');
    if (title.length > 200) return toast('标题最多 200 字', 'err');
    if (!categoryId) return toast('请选择分类', 'err');
    if (!content.trim()) return toast('正文不能为空', 'err');

    const body = { title, summary: summary || null, content, categoryId: Number(categoryId) };
    const btn = thenSubmit ? document.getElementById('btn-save-submit') : document.getElementById('btn-save');
    btn.disabled = true;

    try {
        let id = articleId;
        if (id) {
            body.version = version;
            await api.put(`/api/articles/${id}`, body);
        } else {
            id = await api.post('/api/articles', body);
        }

        if (thenSubmit) {
            await api.post(`/api/articles/${id}/submit`);
            toast('已提交审核，等待审核员处理', 'ok');
            setTimeout(() => location.href = '/app/articles?mine=1', 400);
        } else {
            toast('草稿已保存', 'ok');
            if (!articleId) {
                setTimeout(() => location.href = `/app/article-edit?id=${id}`, 300);
            } else {
                await loadArticle();
            }
        }
    } catch (e) {
        toast(e.message, 'err');
    } finally {
        btn.disabled = false;
    }
}
