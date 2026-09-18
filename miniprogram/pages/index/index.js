const { request } = require('../../utils/request');

const CATEGORIES = ['最新', '跑腿代办', '二手售卖', '日常分享', '校友求助'];

const FUNC_ENTRIES = [
  { title: '跑腿代办', desc: '帮取快递 代办小事', icon: '🚴', module: 0 },
  { title: '二手售卖', desc: '闲置物品 低价转让', icon: '🛒', module: 1 },
  { title: '日常分享', desc: '校园生活 随手分享', icon: '🌟', module: 2 },
  { title: '校友求助', desc: '问题咨询 经验求助', icon: '🤝', module: 3 }
];

Page({
  data: {
    statusBarHeight: 20,
    navBarTotalHeight: 64,
    schoolName: '我的学校',
    funcEntries: FUNC_ENTRIES,
    categories: CATEGORIES,
    hotPosts: [],
    pinnedPosts: [],
    posts: [],
    currentTab: 0,
    page: 1,
    size: 10,
    loading: false,
    noMore: false
  },

  onLoad() {
    try {
      const sysInfo = wx.getSystemInfoSync();
      const statusBarHeight = sysInfo.statusBarHeight || 20;
      this.setData({
        statusBarHeight,
        navBarTotalHeight: statusBarHeight + 44
      });
    } catch (e) {
      // 默认值已设置
    }
    this.loadPinnedPosts();
    this.loadHotPosts();
    this.loadPosts(true);
  },

  onPullDownRefresh() {
    // 先加载置顶，完成后再加载列表，最后停止刷新动画
    this.loadPinnedPosts();
    this.loadPosts(true, function () {
      wx.stopPullDownRefresh();
    });
  },

  onReachBottom() {
    if (!this.data.noMore && !this.data.loading) {
      this.loadPosts(false);
    }
  },

  onShow() {
    // 从其他页返回时刷新数据（如发帖页）
    this.loadPinnedPosts();
    this.loadHotPosts();
    this.loadPosts(true);
  },

  // ==================== 数据加载 ====================

  loadPinnedPosts() {
    request({
      url: '/post/list',
      data: { page: 1, size: 20 },
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          const records = res.data.data.records || [];
          const pinned = records
            .filter(p => p.isTop === 1)
            .slice(0, 3)
            .map(p => this.formatPost(p));
          this.setData({ pinnedPosts: pinned });
        }
      },
      fail: () => {
        // 静默失败
      }
    });
  },

  loadHotPosts() {
    request({
      url: '/post/hot',
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          this.setData({ hotPosts: res.data.data || [] });
        }
      },
      fail: () => {
        // 静默失败
      }
    });
  },

  loadPosts(refresh, callback) {
    if (this.data.loading) return;

    const page = refresh ? 1 : this.data.page;
    const params = { page: page, size: this.data.size };

    if (this.data.currentTab > 0) {
      params.module = this.data.currentTab - 1;
    }

    this.setData({ loading: true });

    request({
      url: '/post/list',
      data: params,
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          const pageData = res.data.data;
          const list = (pageData.records || [])
            .filter(p => p.isTop !== 1)
            .map(p => this.formatPost(p));
          const noMore = page >= pageData.pages;

          this.setData({
            posts: refresh ? list : this.data.posts.concat(list),
            page: page + 1,
            noMore: noMore,
            loading: false
          });
        } else {
          this.setData({ loading: false });
        }
      },
      fail: () => {
        this.setData({ loading: false });
        wx.showToast({ title: '加载失败', icon: 'none' });
      },
      complete: () => {
        if (typeof callback === 'function') {
          callback();
        }
      }
    });
  },

  // ==================== 交互事件 ====================

  switchTab(e) {
    const index = e.currentTarget.dataset.index;
    if (index === this.data.currentTab) return;
    this.setData({ currentTab: index, posts: [], page: 1, noMore: false });
    this.loadPosts(true);
  },

  goModule(e) {
    const module = e.currentTarget.dataset.module;
    const tabIndex = module + 1;
    this.setData({ currentTab: tabIndex, posts: [], page: 1, noMore: false });
    this.loadPosts(true);
    wx.pageScrollTo({ selector: '.cat-tabs', duration: 300 });
  },

  switchSchool() {
    wx.showToast({ title: '学校切换暂未开放', icon: 'none' });
  },

  goSearch() {
    wx.navigateTo({ url: '/pages/search/search' });
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/detail/detail?id=' + id });
  },

  goCreate() {
    wx.navigateTo({ url: '/pages/publish/publish' });
  },

  goComment(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/detail/detail?id=' + id + '&focus=comment' });
  },

  toggleLike(e) {
    wx.showToast({ title: '点赞功能开发中', icon: 'none' });
  },

  // ==================== 工具方法 ====================

  formatPost(post) {
    return Object.assign({}, post, {
      timeAgo: post.createTimeStr || this.formatTime(post.createTime)
    });
  },

  formatTime(timeVal) {
    if (!timeVal) return '';
    let date;
    if (typeof timeVal === 'number') {
      // 时间戳（毫秒）
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
