const { request } = require('../../utils/request');

Page({
  data: {
    statusBarHeight: 20,
    navBarTotalHeight: 64,
    posts: [],
    loading: true
  },

  onLoad() {
    try {
      const sysInfo = wx.getSystemInfoSync();
      const statusBarHeight = sysInfo.statusBarHeight || 20;
      this.setData({
        statusBarHeight: statusBarHeight,
        navBarTotalHeight: statusBarHeight + 44
      });
    } catch (e) {
      // ignore
    }
    this.loadLikedPosts();
  },

  onShow() {
    this.loadLikedPosts();
  },

  goBack() {
    wx.navigateBack({ delta: 1 });
  },

  /**
   * 加载我点赞的帖子
   */
  loadLikedPosts() {
    this.setData({ loading: true });

    request({
      url: '/post/liked',
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          const posts = (res.data.data || []).map(p => {
            p.timeAgo = p.createTimeStr || this.formatTime(p.createTime);
            return p;
          });
          this.setData({ posts: posts });
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

  /**
   * 跳转帖子详情
   */
  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/detail/detail?id=' + id });
  },

  /**
   * 跳转首页
   */
  goHome() {
    wx.switchTab({ url: '/pages/index/index' });
  },

  /**
   * 格式化时间
   */
  formatTime(timeVal) {
    if (!timeVal) return '';
    let date;
    if (typeof timeVal === 'number') {
      date = new Date(timeVal);
    } else if (Array.isArray(timeVal)) {
      date = new Date(timeVal[0], timeVal[1] - 1, timeVal[2], timeVal[3] || 0, timeVal[4] || 0, timeVal[5] || 0);
    } else if (typeof timeVal === 'string') {
      date = new Date(timeVal.replace(/-/g, '/'));
    } else {
      date = new Date(timeVal);
    }
    if (isNaN(date.getTime())) return '';
    const diff = Date.now() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return '刚刚';
    if (minutes < 60) return minutes + '分钟前';
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return hours + '小时前';
    const days = Math.floor(hours / 24);
    if (days < 30) return days + '天前';
    return (date.getMonth() + 1) + '-' + date.getDate();
  }
});
