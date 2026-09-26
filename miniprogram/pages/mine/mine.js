const { request } = require('../../utils/request');

Page({
  data: {
    userInfo: {
      nickname: '',
      avatar: '',
      activityLevel: '新手上路',
      activityColor: '#999999',
      activityScore: 0,
      studentNo: '',
      role: 0
    },
    menuItems: [
      { label: '我的帖子', action: 'myPosts' },
      { label: '我的点赞', action: 'myLikes' },
      { label: '个人资料', action: 'profile' },
      { label: '用户协议', action: 'agreement' },
      { label: '免责声明', action: 'disclaimer' },
      { label: '关于我们', action: 'about' }
    ],
    adminMenu: null
  },

  onLoad() {
    this.loadUserInfo();
  },

  onShow() {
    this.loadUserInfo();
  },

  /**
   * 点击登录
   */
  doLogin() {
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
                wx.showToast({ title: '登录成功', icon: 'success' });
                this.loadUserInfo();
              }
            },
            fail: () => {
              wx.showToast({ title: '登录失败，请重试', icon: 'none' });
            }
          });
        }
      }
    });
  },

  /**
   * 加载用户信息
   */
  loadUserInfo() {
    request({
      url: '/user/info',
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          const data = res.data.data;
          const role = data.role || 0;
          
          let adminMenu = null;
          if (role === 1) {
            adminMenu = { label: '管理后台', action: 'admin' };
          } else if (role === 2) {
            adminMenu = { label: '审核管理', action: 'audit' };
          }
          
          this.setData({
            userInfo: {
              nickname: data.nickname || '校园用户',
              avatar: data.avatar || '',
              activityLevel: data.activityLevel || '新手上路',
              activityColor: data.activityColor || '#999999',
              activityScore: data.activityScore || 0,
              studentNo: data.studentNo || '',
              role: role
            },
            adminMenu: adminMenu
          });
        }
      },
      fail: () => {
        // 静默失败
      }
    });
  },

  /**
   * 菜单点击
   */
  onMenuTap(e) {
    const index = e.currentTarget.dataset.index;
    const item = this.data.menuItems[index];
    if (!item) return;

    switch (item.action) {
      case 'myPosts':
        wx.navigateTo({ url: '/pages/my-posts/my-posts' });
        break;
      case 'myLikes':
        wx.navigateTo({ url: '/pages/my-likes/my-likes' });
        break;
      case 'profile':
        wx.navigateTo({ url: '/pages/profile/profile' });
        break;
      case 'agreement':
        wx.navigateTo({ url: '/pages/agreement/agreement' });
        break;
      case 'disclaimer':
        wx.navigateTo({ url: '/pages/disclaimer/disclaimer' });
        break;
      case 'about':
        wx.navigateTo({ url: '/pages/about/about' });
        break;
    }
  },

  onAdminTap() {
    const role = this.data.userInfo.role;
    if (role === 1) {
      wx.navigateTo({ url: '/pages/admin/admin' });
    } else if (role === 2) {
      wx.navigateTo({ url: '/pages/audit/audit' });
    }
  },

  /**
   * 退出登录
   */
  logout() {
    wx.showModal({
      title: '提示',
      content: '确定要退出登录吗？',
      confirmColor: '#FF4D4F',
      success: (res) => {
        if (res.confirm) {
          wx.removeStorageSync('token');
          wx.removeStorageSync('userInfo');
          this.setData({
            userInfo: {
              nickname: '',
              avatar: '',
              activityLevel: '新手上路',
              activityColor: '#999999',
              activityScore: 0,
              studentNo: ''
            }
          });
          wx.showToast({ title: '已退出登录', icon: 'none' });
        }
      }
    });
  }
});
