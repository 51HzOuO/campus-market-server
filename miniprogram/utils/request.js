const BASE_URL = 'http://localhost:8080';

/**
 * 封装 wx.request，自动携带 token
 * @param {Object} options - { url, method, data, success, fail, complete }
 */
function request(options) {
  const token = wx.getStorageSync('token') || '';

  wx.request({
    url: BASE_URL + options.url,
    method: options.method || 'GET',
    data: options.data || {},
    header: Object.assign({
      'Content-Type': 'application/json',
      'Authorization': token ? ('Bearer ' + token) : ''
    }, options.header || {}),
    success: (res) => {
      // 401 未登录，跳转或提示
      if (res.data && res.data.code === 401) {
        wx.showToast({ title: '请先登录', icon: 'none' });
        if (typeof options.fail === 'function') {
          options.fail(res);
        }
        return;
      }
      if (typeof options.success === 'function') {
        options.success(res);
      }
    },
    fail: (res) => {
      if (typeof options.fail === 'function') {
        options.fail(res);
      }
    },
    complete: (res) => {
      if (typeof options.complete === 'function') {
        options.complete(res);
      }
    }
  });
}

module.exports = { request };
