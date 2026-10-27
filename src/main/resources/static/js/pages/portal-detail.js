import { api } from '../core/api.js';
import { esc, fmtTime } from '../core/ui.js';

const id = document.body.dataset.articleId;
const view = document.getElementById('article-view');

async function main() {
    if (!id) {
        view.innerHTML = '<div class="empty">缺少文章编号</div>';
        return;
    }
    try {
        const a = await api.get(`/api/portal/articles/${id}`);
        document.title = a.title + ' · 云章 CMS';
        view.innerHTML = `
            <div class="cat" style="color:var(--brand);font-size:12.5px">${esc(a.categoryName || '未分类')}</div>
            <h1 style="margin-top:8px">${esc(a.title)}</h1>
            <div class="meta">
                <span>作者 ${esc(a.authorName || '佚名')}</span>
                <span>${fmtTime(a.publishTime || a.createTime)}</span>
                <span>阅读 ${a.viewCount ?? 0}</span>
            </div>
            ${a.summary ? `<p style="color:var(--muted);font-size:14.5px;margin:22px 0 0;padding-left:12px;border-left:3px solid var(--line-strong)">${esc(a.summary)}</p>` : ''}
            <div class="body">${esc(a.content || '')}</div>`;
    } catch (e) {
        view.innerHTML = `<div class="empty"><div class="big">◌</div>${esc(e.message)}<br>
            <a href="/portal">回到文章列表</a></div>`;
    }
}

main();
