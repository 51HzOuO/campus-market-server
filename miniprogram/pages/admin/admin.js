const { request } = require('../../utils/request');

Page({
  data: {
    statusBarHeight: 20,
    navBarTotalHeight: 64,
    stats: {},
    users: [],
    posts: [],
    currentUserId: null,
    roleOptions: ['普通用户', '审核员'],
    currentTab: 0,
    loading: false,
    page: 1,
    noMore: false
  },

  onLoad: function () {
    var that = this;
    try {
      var sysInfo = wx.getSystemInfoSync();
      var statusBarHeight = sysInfo.statusBarHeight || 20;
      that.setData({
        statusBarHeight: statusBarHeight,
        navBarTotalHeight: statusBarHeight + 44
      });
    } catch (e) {}
    var userInfo = wx.getStorageSync('userInfo') || {};
    that.setData({ currentUserId: userInfo.id || userInfo.userId || null });
    that.loadStats();
  },

  onShow: function () {
    var tab = this.data.currentTab;
    if (tab === 1) this.loadUsers(true);
    if (tab === 2) this.loadPosts(true);
  },

  onReachBottom: function () {
    if (this.data.noMore || this.data.loading) return;
    var tab = this.data.currentTab;
    if (tab === 1) this.loadUsers(false);
    if (tab === 2) this.loadPosts(false);
  },

  goBack: function () {
    wx.navigateBack({ delta: 1 });
  },

  goModuleAdmin: function (e) {
    var type = e && e.currentTarget && e.currentTarget.dataset.type || 'errand';
    wx.navigateTo({ url: '/pages/module-admin/module-admin?type=' + type });
  },

  switchTab: function (e) {
    var tab = parseInt(e.currentTarget.dataset.tab);
    if (tab === this.data.currentTab) return;
    this.setData({
      currentTab: tab,
      users: [],
      posts: [],
      page: 1,
      noMore: false,
      loading: false
    });
    if (tab === 0) this.loadStats();
    if (tab === 1) this.loadUsers(true);
    if (tab === 2) this.loadPosts(true);
  },

  loadStats: function () {
    var that = this;
    request({
      url: '/admin/stats',
      method: 'GET',
      success: function (res) {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          that.setData({ stats: res.data.data });
        }
      }
    });
  },

  loadUsers: function (refresh) {
    if (this.data.loading) return;
    var that = this;
    var page = refresh ? 1 : this.data.page;
    that.setData({ loading: true });

    request({
      url: '/admin/users',
      data: { page: page, size: 20 },
      method: 'GET',
      success: function (res) {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          var pageData = res.data.data;
          var list = (pageData.records || []).map(function (user) {
            user.roleIndex = Number(user.role) === 2 ? 1 : 0;
            user.canManage = Number(user.role) !== 1 && Number(user.id) !== Number(that.data.currentUserId);
            return user;
          });
          that.setData({
            users: refresh ? list : that.data.users.concat(list),
            page: page + 1,
            noMore: page >= pageData.pages,
            loading: false
          });
        } else {
          that.setData({ loading: false });
        }
      },
      fail: function () {
        that.setData({ loading: false });
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  loadPosts: function (refresh) {
    if (this.data.loading) return;
    var that = this;
    var page = refresh ? 1 : this.data.page;
    that.setData({ loading: true });

    request({
      url: '/admin/posts',
      data: { page: page, size: 20 },
      method: 'GET',
      success: function (res) {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          var pageData = res.data.data;
          var list = pageData.records || [];
          that.setData({
            posts: refresh ? list : that.data.posts.concat(list),
            page: page + 1,
            noMore: page >= pageData.pages,
            loading: false
          });
        } else {
          that.setData({ loading: false });
        }
      },
      fail: function () {
        that.setData({ loading: false });
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  banUser: function (e) {
    var that = this;
    var userId = e.currentTarget.dataset.id;
    var idx = parseInt(e.currentTarget.dataset.idx);
    var user = this.data.users[idx];
    if (!user || !user.canManage) {
      wx.showToast({ title: '管理员账号不能操作封禁', icon: 'none' });
      return;
    }
    var action = user.status === 0 ? '封禁' : '解封';

    wx.showModal({
      title: '确认',
      content: '确定要' + action + '该用户吗？',
      success: function (res) {
        if (res.confirm) {
          request({
            url: '/admin/ban',
            data: { userId: userId },
            method: 'POST',
            success: function (res) {
              if (res.statusCode === 200 && res.data && res.data.code === 200) {
                wx.showToast({ title: res.data.data, icon: 'success' });
                that.loadUsers(true);
              }
            }
          });
        }
      }
    });
  },

  changeRole: function (e) {
    var idx = parseInt(e.currentTarget.dataset.idx);
    var user = this.data.users[idx];
    if (!user || !user.canManage) return;
    var role = Number(e.detail.value) === 1 ? 2 : 0;
    if (role === Number(user.role)) return;
    request({
      url: '/user/admin/update-role',
      data: { userId: user.id, role: role },
      method: 'POST',
      success: function (res) {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          wx.showToast({ title: '角色已更新', icon: 'success' });
          this.loadUsers(true);
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '角色修改失败', icon: 'none' });
        }
      }.bind(this),
      fail: function () { wx.showToast({ title: '网络错误，请重试', icon: 'none' }); }
    });
  },

  topPost: function (e) {
    var that = this;
    var postId = e.currentTarget.dataset.id;

    request({
      url: '/admin/top',
      data: { postId: postId },
      method: 'POST',
      success: function (res) {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          wx.showToast({ title: res.data.data, icon: 'success' });
          that.loadPosts(true);
        }
      }
    });
  },

  deletePost: function (e) {
    var that = this;
    var postId = e.currentTarget.dataset.id;

    wx.showModal({
      title: '确认',
      content: '确定要删除该帖子吗？',
      success: function (res) {
        if (res.confirm) {
          request({
            url: '/post/delete',
            data: { postId: postId },
            method: 'POST',
            success: function (res) {
              if (res.statusCode === 200 && res.data && res.data.code === 200) {
                wx.showToast({ title: '已删除', icon: 'success' });
                that.loadPosts(true);
              }
            }
          });
        }
      }
    });
  }
});
