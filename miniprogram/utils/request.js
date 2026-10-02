// Keep request configuration in this module so the mini-program startup bundle
// cannot omit a separate config module needed by app.js.
const BASE_URL = 'https://springboot-4xrc-316010-10-1490372189.sh.run.tcloudbase.com';
const REQUEST_TIMEOUT = 20000;

function toAbsoluteUrl(path) {
  if (!path) return '';
  if (/^https?:\/\//i.test(path)) return path;
  return BASE_URL.replace(/\/+$/, '') + '/' + String(path).replace(/^\/+/, '');
}

function appInstance() {
  return typeof getApp === 'function' ? getApp() : null;
}

function errorOf(message, code, response, kind) {
  const error = new Error(message);
  error.code = code;
  error.data = { code, message };
  error.statusCode = response ? response.statusCode : 0;
  error.response = response;
  error.kind = kind;
  return error;
}

function networkError(failure) {
  const detail = String((failure && failure.errMsg) || '');
  let message = '无法连接后端，请检查网络和云托管服务';
  if (/not in domain list|合法域名/i.test(detail)) {
    message = '请求域名未获允许，请在小程序后台配置云托管域名';
  } else if (/name_not_resolved|resolve|dns/i.test(detail)) {
    message = '后端域名无法解析，请检查网络及服务域名';
  } else if (/timeout|timed out/i.test(detail)) {
    message = '后端响应超时，请稍后重试并检查云托管服务';
  } else if (/ssl|certificate/i.test(detail)) {
    message = '后端 HTTPS 证书校验失败，请检查服务域名和证书';
  }
  return errorOf(message, 0, null, 'network');
}

function normalizeResponse(raw) {
  let body = raw.data;
  if (typeof body === 'string') {
    try { body = JSON.parse(body); } catch (_) { body = null; }
  }
  const status = raw.statusCode;
  let code = status;
  let message;
  if (status === 401) message = '登录已失效，请重新登录';
  else if (status === 403) message = '无访问权限，请检查账号状态或云托管访问设置';
  else if (status === 404) message = '接口不存在（HTTP 404），请部署最新后端';
  else if (status === 413) message = '图片过大，请压缩后重新上传';
  else if (status >= 500) message = '服务端错误（HTTP ' + status + '），请检查云托管日志和数据库';
  else if (status < 200 || status >= 300) message = '请求失败（HTTP ' + status + '）';
  else if (!body || typeof body !== 'object' || typeof body.code !== 'number') {
    code = 502;
    message = '后端返回格式异常，请确认配置的是服务域名';
  }
  if (message) return Object.assign({}, raw, { data: { code, message } });
  return Object.assign({}, raw, { data: body });
}

function isPublic(options) {
  if ((options.method || 'GET').toUpperCase() !== 'GET') return false;
  const path = options.url.split('?')[0];
  if (path === '/errand/list' && ((options.data || {}).scope === 'mine' || /[?&]scope=mine(?:&|$)/.test(options.url))) return false;
  return ['/post/list', '/post/hot', '/post/search', '/comment/list', '/errand/list',
    '/second-hand/list', '/club/list', '/club/activity/list'].includes(path)
    || /^\/(post|errand|second-hand|club|club\/activity)\/detail\/\d+$/.test(path);
}

async function tokenFor(options) {
  if (options.auth === false) return '';
  const app = appInstance();
  if (app && !isPublic(options)) await app.ensureLogin();
  return wx.getStorageSync('token') || '';
}

function clearSession() {
  const app = appInstance();
  if (app && app.clearSession) app.clearSession();
  else { wx.removeStorageSync('token'); wx.removeStorageSync('userInfo'); }
}

// 只在明确的 401（鉴权失败）后重新登录并重放一次；超时不重放写入请求。
async function perform(options, uploadPath) {
  if (!/^\/(?!\/)/.test(options.url)) throw errorOf('接口路径配置错误', 400, null, 'config');
  let token = await tokenFor(options);
  for (let attempt = 0; attempt < 2; attempt++) {
    const raw = await new Promise((resolve, reject) => {
      const header = Object.assign({}, options.header || {});
      if (token) header.Authorization = 'Bearer ' + token;
      const params = {
        url: toAbsoluteUrl(options.url), timeout: REQUEST_TIMEOUT, header,
        success: resolve, fail: failure => reject(networkError(failure))
      };
      if (uploadPath) {
        wx.uploadFile(Object.assign(params, { filePath: uploadPath, name: 'file' }));
      } else {
        header['Content-Type'] = 'application/json';
        wx.request(Object.assign(params, { method: options.method || 'GET', data: options.data || {} }));
      }
    });
    const res = normalizeResponse(raw);
    if (res.data.code === 401 && options.auth !== false) {
      const app = appInstance();
      if (attempt === 0 && app && app.ensureLogin) {
        const currentToken = wx.getStorageSync('token') || '';
        if (!currentToken || currentToken === token) await app.ensureLogin({ force: true });
        token = wx.getStorageSync('token') || '';
        continue;
      }
      if ((wx.getStorageSync('token') || '') === token) clearSession();
    }
    if (res.data.code !== 200 || res.statusCode < 200 || res.statusCode >= 300) {
      throw errorOf(res.data.message || '操作失败，请重试', res.data.code, res, 'response');
    }
    return res;
  }
}

let lastError = '';
let lastErrorTime = 0;
function showError(error) {
  const message = error.message || '操作失败，请重试';
  if (message !== lastError || Date.now() - lastErrorTime > 2000) {
    wx.showToast({ title: message, icon: 'none', duration: 4000 });
    lastError = message;
    lastErrorTime = Date.now();
  }
}

async function requestAsync(options) {
  try { return await perform(options); }
  catch (error) {
    // 不记录请求头、登录 code、token、表单内容或完整响应。
    console.warn('[campus-api]', options.url.split('?')[0], error.statusCode || 0, error.code || 0);
    throw error;
  }
}

// 兼容已有页面：HTTP/业务错误仍交给 success 判断，网络错误交给 fail。
// complete 在登录等待/重试结束后只执行一次。
async function request(options) {
  let result;
  try {
    result = await requestAsync(options);
  } catch (error) {
    result = error.response || error;
    try {
      if (error.response && options.success) options.success(error.response);
      else if (options.fail) options.fail(error);
      if (options.showError !== false) showError(error);
    } finally {
      if (options.complete) options.complete(result);
    }
    return result;
  }
  try { if (options.success) options.success(result); }
  finally { if (options.complete) options.complete(result); }
  return result;
}

async function uploadImage(filePath, url = '/upload/image') {
  const response = await perform({ url, method: 'POST' }, filePath);
  const data = response.data.data;
  if (!data || (!data.fullUrl && !data.url)) throw errorOf('图片上传响应缺少地址，请检查后端', 502, null, 'response');
  return data;
}

// Keep uploads fast without opening nine connections at once on mobile data.
async function uploadImages(filePaths, url = '/upload/image', concurrency = 3) {
  const paths = Array.isArray(filePaths) ? filePaths : [];
  const results = new Array(paths.length);
  let cursor = 0;
  let failure = null;
  const worker = async () => {
    while (!failure && cursor < paths.length) {
      const index = cursor++;
      try {
        results[index] = await uploadImage(paths[index], url);
      } catch (error) {
        failure = error;
      }
    }
  };
  const workers = Array.from({ length: Math.min(Math.max(concurrency, 1), paths.length) }, worker);
  await Promise.all(workers);
  if (failure) throw failure;
  return results;
}

module.exports = { request, requestAsync, uploadImage, uploadImages, toAbsoluteUrl, BASE_URL };
