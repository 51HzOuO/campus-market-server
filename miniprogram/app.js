const { BASE_URL } = require('./utils/request');

App({
  globalData: {
    baseUrl: BASE_URL,
    userInfo: null
  },

  onLaunch() {
    this.tryLogin();
  },

  tryLogin(callback) {
    wx.login({
      success: (loginRes) => {
        if (!loginRes.code) {
          if (callback) callback(false);
          return;
        }
        wx.request({
          url: BASE_URL + '/auth/login',
          method: 'POST',
          data: { code: loginRes.code },
          header: { 'Content-Type': 'application/json' },
          success: (res) => {
            if (res.data && res.data.code === 200 && res.data.data) {
              const data = res.data.data;
              wx.setStorageSync('token', data.token);
              wx.setStorageSync('userInfo', data);
              this.globalData.userInfo = data;
              if (callback) callback(true, data);
            } else if (callback) {
              callback(false);
            }
          },
          fail: () => callback && callback(false)
        });
      },
      fail: () => callback && callback(false)
    });
  }
});
