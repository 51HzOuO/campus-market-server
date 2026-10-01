const { BASE_URL, requestAsync } = require('./utils/request');

App({
  globalData: { baseUrl: BASE_URL, userInfo: null, loginError: '' },
  _loginPromise: null,
  _sessionVersion: 0,

  onLaunch() {
    this.globalData.userInfo = wx.getStorageSync('userInfo') || null;
    this.ensureLogin().catch(error => { this.globalData.loginError = error.message; });
  },

  // 启动登录、手动登录与受保护请求共用一个 Promise，防止相互覆盖 token。
  ensureLogin(options = {}) {
    if (this._loginPromise) return this._loginPromise;
    if (!options.force && wx.getStorageSync('token')) {
      return Promise.resolve(wx.getStorageSync('userInfo') || {});
    }
    const version = this._sessionVersion;
    const login = new Promise((resolve, reject) => {
      wx.login({
        timeout: 10000,
        success: result => result.code ? resolve(result.code) : reject(new Error('微信未返回登录凭证，请检查开发者工具登录和 AppID')),
        fail: () => reject(new Error('微信登录调用失败，请检查开发者工具登录和 AppID'))
      });
    }).then(code => requestAsync({ url: '/auth/login', method: 'POST', data: { code }, auth: false }))
      .then(response => {
        const user = response.data.data;
        if (!user || !user.token) throw new Error('登录响应缺少凭证，请检查后端版本');
        if (version !== this._sessionVersion) throw new Error('本次登录已取消');
        wx.setStorageSync('token', user.token);
        wx.setStorageSync('userInfo', user);
        this.globalData.userInfo = user;
        this.globalData.loginError = '';
        return user;
      });
    this._loginPromise = login.then(user => {
      this._loginPromise = null;
      return user;
    }, error => {
      this._loginPromise = null;
      if (version === this._sessionVersion) {
        this.clearSession();
        this.globalData.loginError = error.message;
      }
      throw error;
    });
    return this._loginPromise;
  },

  tryLogin(callback) {
    return this.ensureLogin({ force: true }).then(user => {
      if (callback) callback(true, user);
      return user;
    }, error => {
      if (callback) callback(false, null, error.message);
      return null;
    });
  },

  clearSession() {
    this._sessionVersion++;
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    this.globalData.userInfo = null;
  }
});
