const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '../../miniprogram');
const domain = 'https://springboot-4xrc-316010-10-1490372189.sh.run.tcloudbase.com';

function harness(options = {}) {
  const storage = new Map(Object.entries(options.storage || {}));
  const requests = [], uploads = [], toasts = [], loginCalls = [], logs = [];
  let app;
  const ok = (params, data = {}) => params.success({ statusCode: 200, data: { code: 200, data, message: 'success' } });
  const wx = {
    getStorageSync: key => storage.get(key),
    setStorageSync: (key, value) => storage.set(key, value),
    removeStorageSync: key => storage.delete(key),
    showToast: value => toasts.push(value),
    login: params => {
      loginCalls.push(params);
      if (options.login) options.login(params);
      else queueMicrotask(() => params.success({ code: 'mock-code' }));
    },
    request: params => {
      requests.push(params);
      queueMicrotask(() => {
        if (options.request) options.request(params, ok);
        else if (params.url.endsWith('/auth/login')) ok(params, { userId: 7, token: 'new-token' });
        else ok(params, []);
      });
    },
    uploadFile: params => {
      uploads.push(params);
      queueMicrotask(() => {
        if (options.upload) options.upload(params);
        else params.success({ statusCode: 200, data: JSON.stringify({ code: 200, data: { url: '/uploads/a.png' } }) });
      });
    }
  };
  const cache = new Map();
  function load(relative) {
    if (cache.has(relative)) return cache.get(relative);
    const module = { exports: {} };
    const context = {
      module, exports: module.exports, wx, console: { warn: (...args) => logs.push(args) },
      getApp: () => app, App: definition => { app = definition; },
      require: spec => {
        if (spec === './utils/request') return load('utils/request.js');
        throw new Error('Unexpected module ' + spec);
      }
    };
    vm.runInNewContext(fs.readFileSync(path.join(root, relative), 'utf8'), context, { filename: relative });
    cache.set(relative, module.exports);
    return module.exports;
  }
  const api = load('utils/request.js');
  load('app.js');
  return { api, app, wx, storage, requests, uploads, loginCalls, toasts, logs };
}

test('public list uses the confirmed direct service domain without forcing login', async () => {
  const h = harness();
  await h.api.requestAsync({ url: '/post/list', data: { page: 1 } });
  assert.equal(h.requests[0].url, domain + '/post/list');
  assert.equal(h.loginCalls.length, 0);
  assert.equal(h.api.toAbsoluteUrl('/uploads/a.png'), domain + '/uploads/a.png');
});

test('startup, manual login and protected request share one login and wait for token', async () => {
  const h = harness({ login: () => {} });
  h.app.onLaunch();
  let callbackUser;
  const manual = h.app.tryLogin((success, user) => { assert.equal(success, true); callbackUser = user; });
  const create = h.api.requestAsync({ url: '/post/create', method: 'POST', data: { title: 'test' } });
  await Promise.resolve();
  assert.equal(h.requests.length, 0);
  assert.equal(h.loginCalls.length, 1);
  h.loginCalls[0].success({ code: 'mock-code' });
  await Promise.all([manual, create]);
  assert.equal(h.requests.filter(x => x.url.endsWith('/auth/login')).length, 1);
  assert.equal(h.requests.find(x => x.url.endsWith('/post/create')).header.Authorization, 'Bearer new-token');
  assert.equal(callbackUser.userId, 7);
});

test('stored login token is reused on launch', async () => {
  const h = harness({ storage: { token: 'valid-token', userInfo: { userId: 7 } } });
  h.app.onLaunch();
  await h.api.requestAsync({ url: '/user/info' });
  assert.equal(h.loginCalls.length, 0);
  assert.equal(h.requests[0].header.Authorization, 'Bearer valid-token');
});

test('concurrent 401 responses refresh once and complete each request once', async () => {
  const h = harness({ storage: { token: 'expired' }, request: (p, ok) => {
    if (p.url.endsWith('/auth/login')) return ok(p, { token: 'refreshed', userId: 7 });
    if (p.header.Authorization === 'Bearer expired') return p.success({ statusCode: 401, data: { code: 401 } });
    ok(p, { accepted: true });
  } });
  let complete = 0, success = 0;
  const make = () => h.api.request({ url: '/post/create', method: 'POST', success: r => { assert.equal(r.data.code, 200); success++; }, complete: () => complete++ });
  await Promise.all([make(), make()]);
  assert.equal(h.loginCalls.length, 1);
  assert.equal(success, 2);
  assert.equal(complete, 2);
  assert.equal(h.requests.filter(x => x.url.endsWith('/post/create')).length, 4);
});

test('second 401 fails without looping and clears invalid session', async () => {
  const h = harness({ storage: { token: 'expired' }, request: (p, ok) => {
    if (p.url.endsWith('/auth/login')) return ok(p, { token: 'still-invalid', userId: 7 });
    p.success({ statusCode: 401, data: { code: 401 } });
  } });
  await assert.rejects(h.api.requestAsync({ url: '/user/info' }), error => error.code === 401);
  assert.equal(h.requests.filter(x => x.url.endsWith('/user/info')).length, 2);
  assert.equal(h.loginCalls.length, 1);
  assert.equal(h.storage.has('token'), false);
});

test('DNS failure reports the cause and does not replay a create request', async () => {
  const h = harness({ storage: { token: 'valid' }, request: p => p.fail({ errMsg: 'request:fail net::ERR_NAME_NOT_RESOLVED' }) });
  let complete = 0, failure;
  await h.api.request({ url: '/post/create', method: 'POST', fail: error => { failure = error; }, complete: () => complete++ });
  assert.match(failure.message, /域名无法解析/);
  assert.equal(h.requests.length, 1);
  assert.equal(complete, 1);
});

test('timeout does not replay a possibly committed write', async () => {
  const h = harness({ storage: { token: 'valid' }, request: p => p.fail({ errMsg: 'request:fail timeout' }) });
  await assert.rejects(h.api.requestAsync({ url: '/post/create', method: 'POST' }), /响应超时/);
  assert.equal(h.requests.length, 1);
  assert.equal(h.loginCalls.length, 0);
});

test('404 and HTML response are diagnosed instead of mistaken for success', async () => {
  const missing = harness({ request: p => p.success({ statusCode: 404, data: '<html>not found</html>' }) });
  await assert.rejects(missing.api.requestAsync({ url: '/club/list' }), /HTTP 404/);
  const html = harness({ request: p => p.success({ statusCode: 200, data: '<html>landing page</html>' }) });
  await assert.rejects(html.api.requestAsync({ url: '/club/list' }), /返回格式异常/);
});

test('upload waits for login, uses direct domain, and parses JSON string', async () => {
  const h = harness({ login: () => {} });
  const uploaded = h.api.uploadImage('wxfile://temporary.png');
  await Promise.resolve();
  assert.equal(h.uploads.length, 0);
  h.loginCalls[0].success({ code: 'mock-code' });
  const data = await uploaded;
  assert.equal(data.url, '/uploads/a.png');
  assert.equal(h.uploads[0].url, domain + '/upload/image');
  assert.equal(h.uploads[0].header.Authorization, 'Bearer new-token');
  assert.equal(h.uploads[0].header['Content-Type'], undefined);
});

test('batch image upload keeps selection order while limiting concurrency', async () => {
  const h = harness({ login: () => {} });
  const active = { value: 0, max: 0 };
  h.wx.uploadFile = params => {
    h.uploads.push(params);
    active.value++;
    active.max = Math.max(active.max, active.value);
    setTimeout(() => {
      active.value--;
      params.success({ statusCode: 200, data: JSON.stringify({ code: 200, data: { url: '/' + params.filePath.split('/').pop() } }) });
    }, params.filePath.endsWith('a.png') ? 10 : 1);
  };
  const pending = h.api.uploadImages(['a.png', 'b.png', 'c.png', 'd.png'], '/upload/image', 2);
  await Promise.resolve();
  h.loginCalls[0].success({ code: 'mock-code' });
  const result = await pending;
  assert.equal(active.max, 2);
  assert.deepEqual(Array.from(result, item => item.url), ['/a.png', '/b.png', '/c.png', '/d.png']);
});

test('login errors preserve the backend reason and do not store a token', async () => {
  const h = harness({ request: p => p.success({ statusCode: 200, data: { code: 400, message: '微信 AppID 配置错误' } }) });
  let failure;
  await h.app.tryLogin((success, user, message) => { assert.equal(success, false); failure = message; });
  assert.equal(failure, '微信 AppID 配置错误');
  assert.equal(h.storage.has('token'), false);
});

test('logout during a pending login does not restore the session', async () => {
  const h = harness({ login: () => {} });
  const login = h.app.ensureLogin();
  h.app.clearSession();
  h.loginCalls[0].success({ code: 'mock-code' });
  await assert.rejects(login, /登录已取消/);
  assert.equal(h.storage.has('token'), false);
  assert.equal(h.app.globalData.userInfo, null);
});

test('my errands requires a token although public errand list does not', async () => {
  const h = harness();
  await h.api.requestAsync({ url: '/errand/list', data: { scope: 'mine' } });
  assert.equal(h.loginCalls.length, 1);
  assert.equal(h.requests.at(-1).header.Authorization, 'Bearer new-token');
});

test('API wrapper refuses third-party absolute URLs before attaching credentials', async () => {
  const h = harness({ storage: { token: 'valid' } });
  await assert.rejects(h.api.requestAsync({ url: 'https://example.invalid/post/list' }), /接口路径配置错误/);
  assert.equal(h.requests.length, 0);
});
