const { request } = require('../../utils/request');

Page({
  data: {
    statusBarHeight: 20,
    navBarTotalHeight: 64,
    reports: [],
    loading: false
  },

  onLoad() {
    try {
      const sysInfo = wx.getSystemInfoSync();
      const statusBarHeight = sysInfo.statusBarHeight || 20;
      this.setData({
        statusBarHeight,
        navBarTotalHeight: statusBarHeight + 44
      });
    } catch (e) {}
    this.loadReports();
  },

  goBack() {
    wx.navigateBack({ delta: 1 });
  },

  loadReports() {
    this.setData({ loading: true });
    request({
      url: '/report/list',
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          this.setData({ reports: res.data.data || [] });
        }
      },
      fail: () => {
        wx.showToast({ title: '加载失败', icon: 'none' });
      },
      complete: () => {
        this.setData({ loading: false });
      }
    });
  },

  handleReport(e) {
    const reportId = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认',
      content: '确定标记为已处理吗？',
      success: (res) => {
        if (res.confirm) {
          request({
            url: '/report/handle',
            data: { reportId },
            method: 'POST',
            success: (res) => {
              if (res.statusCode === 200 && res.data && res.data.code === 200) {
                wx.showToast({ title: '已处理', icon: 'success' });
                this.loadReports();
              }
            }
          });
        }
      }
    });
  },

  deletePost(e) {
    const postId = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认',
      content: '确定要删除该帖子吗？',
      success: (res) => {
        if (res.confirm) {
          request({
            url: '/post/delete',
            data: { postId },
            method: 'POST',
            success: (res) => {
              if (res.statusCode === 200 && res.data && res.data.code === 200) {
                wx.showToast({ title: '已删除', icon: 'success' });
                this.loadReports();
              }
            }
          });
        }
      }
    });
  },

  goPost(e) {
    const postId = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/detail/detail?id=' + postId });
  }
});
