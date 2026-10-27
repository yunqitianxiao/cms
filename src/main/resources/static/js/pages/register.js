import { api } from '../core/api.js';

const form = document.getElementById('register-form');
const errBox = document.getElementById('reg-error');
const btn = document.getElementById('btn-reg');

form.onsubmit = async (e) => {
    e.preventDefault();
    const username = document.getElementById('username').value.trim();
    const nickname = document.getElementById('nickname').value.trim();
    const password = document.getElementById('password').value;
    const password2 = document.getElementById('password2').value;
    const email = document.getElementById('email').value.trim();

    const showError = (msg) => {
        errBox.textContent = msg;
        errBox.style.display = 'block';
    };

    errBox.style.display = 'none';
    if (password !== password2) {
        showError('两次输入的密码不一致');
        return;
    }

    btn.disabled = true;
    btn.textContent = '正在创建…';
    try {
        await api.post('/api/auth/register', { username, password, nickname, email: email || null });
        location.replace('/login?u=' + encodeURIComponent(username));
    } catch (err) {
        showError(err.message);
        btn.disabled = false;
        btn.textContent = '注册';
    }
};
