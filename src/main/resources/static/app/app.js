const state = { user: null, accounts: [], orders: [], plugins: [] };
const $ = (q, root = document) => root.querySelector(q);
const $$ = (q, root = document) => [...root.querySelectorAll(q)];

function toast(msg) {
  const el = $('#toast');
  el.textContent = msg;
  el.classList.remove('hidden');
  setTimeout(() => el.classList.add('hidden'), 2400);
}

async function api(url, options = {}) {
  const res = await fetch(url, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }
  });
  const json = await res.json();
  if (json.code && json.code !== 0) throw new Error(json.msg || '请求失败');
  return json;
}

function formData(form) {
  return Object.fromEntries(new FormData(form).entries());
}

async function init() {
  bindNav();
  bindForms();
  try {
    const profile = await api('/api/user/profile', { method: 'POST', body: '{}' });
    state.user = profile.data;
    showApp();
  } catch {
    $('#loginPanel').classList.remove('hidden');
    $('#appPanel').classList.add('hidden');
  }
}

function showApp() {
  $('#loginPanel').classList.add('hidden');
  $('#appPanel').classList.remove('hidden');
  $('#userName').textContent = state.user.nickname || state.user.username;
  $('#merchantPid').value = state.user.pid;
  $('#merchantKey').value = state.user.secretKey;
  $('[name="nickname"]', $('#settingsForm')).value = state.user.nickname || '';
  refreshAll();
}

function bindNav() {
  $$('.nav').forEach(btn => btn.addEventListener('click', () => {
    $$('.nav').forEach(x => x.classList.remove('active'));
    btn.classList.add('active');
    const view = btn.dataset.view;
    $$('.view').forEach(panel => panel.classList.toggle('active', panel.dataset.panel === view));
    const titles = {
      dashboard: ['工作台', '查看收款概览和系统状态'],
      orders: ['订单', '查询、补单和重新通知'],
      accounts: ['收款账号', '维护账户和收款通道'],
      plugins: ['插件', '查看内置支付插件'],
      settings: ['商户设置', '查看 PID、密钥和账户资料'],
      docs: ['接口文档', '易支付兼容接口说明']
    };
    $('#pageTitle').textContent = titles[view][0];
    $('#pageHint').textContent = titles[view][1];
  }));
}

function bindForms() {
  $('#loginForm').addEventListener('submit', async e => {
    e.preventDefault();
    try {
      await api('/api/user/login', { method: 'POST', body: JSON.stringify(formData(e.target)) });
      const profile = await api('/api/user/profile', { method: 'POST', body: '{}' });
      state.user = profile.data;
      showApp();
    } catch (err) { toast(err.message); }
  });

  $('#logoutBtn').addEventListener('click', async () => {
    await api('/api/user/logout', { method: 'POST', body: '{}' });
    location.reload();
  });

  $('#accountForm').addEventListener('submit', async e => {
    e.preventDefault();
    const body = formData(e.target);
    body.state = Number(body.state);
    body.pattern = Number(body.pattern);
    if (body.id) body.id = Number(body.id);
    await api('/api/payManage/' + (body.id ? 'editAccount' : 'addAccount'), { method: 'POST', body: JSON.stringify(body) });
    e.target.reset();
    toast('账号已保存');
    loadAccounts();
  });

  $('#channelForm').addEventListener('submit', async e => {
    e.preventDefault();
    const body = formData(e.target);
    body.accountId = Number(body.accountId);
    body.state = Number(body.state);
    if (body.id) body.id = Number(body.id);
    await api('/api/payManage/' + (body.id ? 'editChannel' : 'addChannel'), { method: 'POST', body: JSON.stringify(body) });
    e.target.reset();
    toast('通道已保存');
    loadAccounts();
  });

  $('#settingsForm').addEventListener('submit', async e => {
    e.preventDefault();
    await api('/api/user/editUser', { method: 'POST', body: JSON.stringify(formData(e.target)) });
    toast('资料已保存');
  });

  $('#resetKeyBtn').addEventListener('click', async () => {
    const res = await api('/api/user/resetKey', { method: 'POST', body: '{}' });
    $('#merchantKey').value = res.data;
    toast('密钥已重置');
  });

  $('#cleanTimeoutBtn').addEventListener('click', async () => {
    const res = await api('/api/order/batchTimeout', { method: 'POST', body: '{}' });
    toast(`已清理 ${res.data} 个订单`);
    loadOrders();
  });

  $('#testPayForm').addEventListener('submit', async e => {
    e.preventDefault();
    const body = formData(e.target);
    body.pid = String(state.user.pid);
    body.out_trade_no ||= 'T' + Date.now();
    body.sign_type = 'MD5';
    body.sign = sign(body, state.user.secretKey);
    const params = new URLSearchParams(body);
    const res = await fetch('/mapi', { method: 'POST', body: params });
    const data = await res.json();
    if (data.code === 1) {
      window.open(data.payurl, '_blank');
      loadOrders();
    } else {
      toast(data.msg || '下单失败');
    }
  });
}

async function refreshAll() {
  await Promise.all([loadDashboard(), loadOrders(), loadAccounts(), loadPlugins()]);
}

async function loadDashboard() {
  const data = await api('/api/console/orderinfo?time=0');
  $('#metricOrders').textContent = data.ordernum || 0;
  $('#metricMoney').textContent = data.totalmoney || '0.00';
  $('#metricWx').textContent = data.wxpay?.money || '0.00';
  $('#metricAli').textContent = data.alipay?.money || '0.00';
}

async function loadOrders() {
  const res = await api('/api/order/getOrders?page=1&limit=100');
  state.orders = res.data || [];
  $('#ordersBody').innerHTML = state.orders.map(order => `
    <tr>
      <td>${order.id}</td>
      <td><a href="/Pay/console/${order.orderId}" target="_blank">${order.orderId}</a></td>
      <td>${order.outTradeNo || ''}</td>
      <td>${order.type || ''}</td>
      <td>${order.money || ''}</td>
      <td>${order.reallyPrice || ''}</td>
      <td><span class="tag ${order.state === 1 ? 'ok' : 'wait'}">${order.state === 1 ? '已支付' : '待支付'}</span></td>
      <td>
        <button class="ghost" onclick="markPaid(${order.id})">补单</button>
        <button class="ghost" onclick="redoNotify(${order.id})">通知</button>
        <button class="danger" onclick="deleteOrder(${order.id})">删除</button>
      </td>
    </tr>`).join('');
}

async function loadAccounts() {
  const res = await api('/api/payManage/getPayAccount?page=1&limit=100');
  state.accounts = res.data || [];
  $('#channelAccount').innerHTML = state.accounts.map(a => `<option value="${a.id}">${a.platform} / ${a.account}</option>`).join('');
  const blocks = [];
  for (const account of state.accounts) {
    const channels = await api('/api/payManage/getChannelList', { method: 'POST', body: JSON.stringify({ aid: String(account.id) }) });
    blocks.push(`<article class="item">
      <div class="item-head">
        <div><strong>${account.platform} / ${account.account}</strong><div class="muted">ID ${account.id} · ${account.state === 1 ? '启用' : '停用'} · ${account.pattern === 1 ? '连续监听' : '单次监听'}</div></div>
        <button class="danger" onclick="deleteAccount(${account.id})">删除</button>
      </div>
      <div class="channels">${(channels.data || []).map(c => `<div class="channel-row"><span>${c.channel} · ${c.type}</span><span>${c.state === 1 ? '启用' : '停用'}</span></div>`).join('') || '<span class="muted">暂无通道</span>'}</div>
    </article>`);
  }
  $('#accountsList').innerHTML = blocks.join('');
}

async function loadPlugins() {
  const res = await api('/api/plugin/list');
  state.plugins = res.data || [];
  $('#pluginList').innerHTML = state.plugins.map(p => `<article class="plugin-card"><div><strong>${p.name}</strong><div class="muted">${p.platform} · ${p.className}</div></div><span class="tag ok">已安装</span></article>`).join('');
}

async function markPaid(id) {
  await api('/api/order/doPayOrder', { method: 'POST', body: JSON.stringify({ id: String(id), state: '1', platform_order: 'manual' }) });
  toast('订单已补单');
  loadOrders();
  loadDashboard();
}

async function redoNotify(id) {
  await api('/api/order/redoPayOrder', { method: 'POST', body: JSON.stringify({ id: String(id) }) });
  toast('已重新通知');
}

async function deleteOrder(id) {
  await api('/api/order/deleteOrder', { method: 'POST', body: JSON.stringify({ id: String(id) }) });
  toast('订单已删除');
  loadOrders();
}

async function deleteAccount(id) {
  await api('/api/payManage/delAccount', { method: 'POST', body: JSON.stringify({ ids: [id] }) });
  toast('账号已删除');
  loadAccounts();
}

function sign(params, key) {
  const pairs = Object.entries(params)
    .filter(([k, v]) => k !== 'sign' && k !== 'sign_type' && v !== undefined && v !== null && String(v) !== '')
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([k, v]) => `${k}=${v}`);
  return md5(pairs.join('&') + key);
}

function md5(input) {
  function cmn(q, a, b, x, s, t) { a = add32(add32(a, q), add32(x, t)); return add32((a << s) | (a >>> (32 - s)), b); }
  function ff(a, b, c, d, x, s, t) { return cmn((b & c) | ((~b) & d), a, b, x, s, t); }
  function gg(a, b, c, d, x, s, t) { return cmn((b & d) | (c & (~d)), a, b, x, s, t); }
  function hh(a, b, c, d, x, s, t) { return cmn(b ^ c ^ d, a, b, x, s, t); }
  function ii(a, b, c, d, x, s, t) { return cmn(c ^ (b | (~d)), a, b, x, s, t); }
  function md5cycle(x, k) {
    let [a, b, c, d] = x;
    a = ff(a, b, c, d, k[0], 7, -680876936); d = ff(d, a, b, c, k[1], 12, -389564586); c = ff(c, d, a, b, k[2], 17, 606105819); b = ff(b, c, d, a, k[3], 22, -1044525330);
    a = ff(a, b, c, d, k[4], 7, -176418897); d = ff(d, a, b, c, k[5], 12, 1200080426); c = ff(c, d, a, b, k[6], 17, -1473231341); b = ff(b, c, d, a, k[7], 22, -45705983);
    a = ff(a, b, c, d, k[8], 7, 1770035416); d = ff(d, a, b, c, k[9], 12, -1958414417); c = ff(c, d, a, b, k[10], 17, -42063); b = ff(b, c, d, a, k[11], 22, -1990404162);
    a = ff(a, b, c, d, k[12], 7, 1804603682); d = ff(d, a, b, c, k[13], 12, -40341101); c = ff(c, d, a, b, k[14], 17, -1502002290); b = ff(b, c, d, a, k[15], 22, 1236535329);
    a = gg(a, b, c, d, k[1], 5, -165796510); d = gg(d, a, b, c, k[6], 9, -1069501632); c = gg(c, d, a, b, k[11], 14, 643717713); b = gg(b, c, d, a, k[0], 20, -373897302);
    a = gg(a, b, c, d, k[5], 5, -701558691); d = gg(d, a, b, c, k[10], 9, 38016083); c = gg(c, d, a, b, k[15], 14, -660478335); b = gg(b, c, d, a, k[4], 20, -405537848);
    a = gg(a, b, c, d, k[9], 5, 568446438); d = gg(d, a, b, c, k[14], 9, -1019803690); c = gg(c, d, a, b, k[3], 14, -187363961); b = gg(b, c, d, a, k[8], 20, 1163531501);
    a = gg(a, b, c, d, k[13], 5, -1444681467); d = gg(d, a, b, c, k[2], 9, -51403784); c = gg(c, d, a, b, k[7], 14, 1735328473); b = gg(b, c, d, a, k[12], 20, -1926607734);
    a = hh(a, b, c, d, k[5], 4, -378558); d = hh(d, a, b, c, k[8], 11, -2022574463); c = hh(c, d, a, b, k[11], 16, 1839030562); b = hh(b, c, d, a, k[14], 23, -35309556);
    a = hh(a, b, c, d, k[1], 4, -1530992060); d = hh(d, a, b, c, k[4], 11, 1272893353); c = hh(c, d, a, b, k[7], 16, -155497632); b = hh(b, c, d, a, k[10], 23, -1094730640);
    a = hh(a, b, c, d, k[13], 4, 681279174); d = hh(d, a, b, c, k[0], 11, -358537222); c = hh(c, d, a, b, k[3], 16, -722521979); b = hh(b, c, d, a, k[6], 23, 76029189);
    a = hh(a, b, c, d, k[9], 4, -640364487); d = hh(d, a, b, c, k[12], 11, -421815835); c = hh(c, d, a, b, k[15], 16, 530742520); b = hh(b, c, d, a, k[2], 23, -995338651);
    a = ii(a, b, c, d, k[0], 6, -198630844); d = ii(d, a, b, c, k[7], 10, 1126891415); c = ii(c, d, a, b, k[14], 15, -1416354905); b = ii(b, c, d, a, k[5], 21, -57434055);
    a = ii(a, b, c, d, k[12], 6, 1700485571); d = ii(d, a, b, c, k[3], 10, -1894986606); c = ii(c, d, a, b, k[10], 15, -1051523); b = ii(b, c, d, a, k[1], 21, -2054922799);
    a = ii(a, b, c, d, k[8], 6, 1873313359); d = ii(d, a, b, c, k[15], 10, -30611744); c = ii(c, d, a, b, k[6], 15, -1560198380); b = ii(b, c, d, a, k[13], 21, 1309151649);
    a = ii(a, b, c, d, k[4], 6, -145523070); d = ii(d, a, b, c, k[11], 10, -1120210379); c = ii(c, d, a, b, k[2], 15, 718787259); b = ii(b, c, d, a, k[9], 21, -343485551);
    x[0] = add32(a, x[0]); x[1] = add32(b, x[1]); x[2] = add32(c, x[2]); x[3] = add32(d, x[3]);
  }
  function md5blk(s) { const md5blks = []; for (let i = 0; i < 64; i += 4) md5blks[i >> 2] = s.charCodeAt(i) + (s.charCodeAt(i + 1) << 8) + (s.charCodeAt(i + 2) << 16) + (s.charCodeAt(i + 3) << 24); return md5blks; }
  function md51(s) { let n = s.length, state = [1732584193, -271733879, -1732584194, 271733878], i; for (i = 64; i <= n; i += 64) md5cycle(state, md5blk(s.substring(i - 64, i))); s = s.substring(i - 64); const tail = Array(16).fill(0); for (i = 0; i < s.length; i++) tail[i >> 2] |= s.charCodeAt(i) << ((i % 4) << 3); tail[i >> 2] |= 0x80 << ((i % 4) << 3); if (i > 55) { md5cycle(state, tail); tail.fill(0); } tail[14] = n * 8; md5cycle(state, tail); return state; }
  function rhex(n) { let s = ''; for (let j = 0; j < 4; j++) s += ((n >> (j * 8 + 4)) & 0x0F).toString(16) + ((n >> (j * 8)) & 0x0F).toString(16); return s; }
  function hex(x) { return x.map(rhex).join(''); }
  function add32(a, b) { return (a + b) & 0xFFFFFFFF; }
  return hex(md51(unescape(encodeURIComponent(input))));
}

init();
