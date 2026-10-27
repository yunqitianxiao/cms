import { api } from '../core/api.js';
import { esc, fmtTime, statusBadge, modal } from '../core/ui.js';

/** 文章详情弹窗，列表页与审核页共用 */
export async function showArticleDetail(id) {
    const m = modal({ title: '稿件详情', body: '<div class="loading"><span class="spinner"></span>加载中</div>', wide: true });
    try {
        const a = await api.get(`/api/articles/${id}`);
        m.body.innerHTML = `
            <h2 style="font-size:20px;line-height:1.4">${esc(a.title)}</h2>
            <div style="display:flex;flex-wrap:wrap;gap:10px;align-items:center;margin:12px 0 16px">
                ${statusBadge(a.status)}
                <span class="badge grey">${esc(a.categoryName || '未分类')}</span>
                <span class="muted" style="font-size:12.5px">作者 ${esc(a.authorName || '—')}</span>
                <span class="muted" style="font-size:12.5px">更新于 ${fmtTime(a.updateTime)}</span>
            </div>
            ${a.summary ? `<p class="muted" style="margin:0 0 16px">${esc(a.summary)}</p>` : ''}
            ${a.auditOpinion ? `<div style="background:${a.status === 'REJECTED' ? 'var(--danger-soft)' : 'var(--brand-soft)'};
                    color:${a.status === 'REJECTED' ? 'var(--danger)' : 'var(--brand-ink)'};
                    padding:10px 12px;border-radius:9px;font-size:13px;margin-bottom:16px">
                    <b>审核意见：</b>${esc(a.auditOpinion)}</div>` : ''}
            ${a.status === 'OFFLINE' && a.offlineReason ? `<div style="background:#eef0f3;padding:10px 12px;border-radius:9px;font-size:13px;margin-bottom:16px">
                    <b>下架原因：</b>${esc(a.offlineReason)}</div>` : ''}
            <div style="white-space:pre-wrap;line-height:1.9;font-size:14.5px;color:#1f2937">${esc(a.content || '')}</div>`;
    } catch (e) {
        m.body.innerHTML = `<div class="empty">加载失败：${esc(e.message)}</div>`;
    }
}
