App({
  onLaunch() {
    // 初始化云开发
    if (!wx.cloud) {
      console.error('请使用 2.2.3 或以上的基础库以使用云能力');
    } else {
      wx.cloud.init({
        env: 'prod-d9g22ewvw948428fc',
        traceUser: true
      });
    }
    
    this.login();
  },

  login() {
    wx.login({
      success: (res) => {
        if (res.code) {
          wx.cloud.callContainer({
            config: {
              env: 'prod-d9g22ewvw948428fc'
            },
            service: 'springboot-4xrc',
            path: '/auth/login',
            method: 'POST',
            data: { code: res.code },
            success: (response) => {
              if (response.data.code === 200) {
                const token = response.data.data.token;
                wx.setStorageSync('token', token);
                wx.setStorageSync('userInfo', response.data.data);
                console.log('登录成功', response.data.data);
              }
            },
            fail: (err) => {
              console.error('登录请求失败', err);
            }
          });
        }
      }
    });
  },

  globalData: {
    token: null
  }
});
