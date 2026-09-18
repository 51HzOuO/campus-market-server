const { request } = require('../../utils/request');

Page({
  data: {
    statusBarHeight: 20,
    navBarTotalHeight: 64,
    keyword: '',
    posts: [],
    loading: false,
    noMore: false,
    page: 1,
    size: 20,
    hasSearched: false
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
      // ignore
    }
  },

  onReachBottom() {
    if (!this.data.noMore && !this.data.loading && this.data.hasSearched) {
      this.loadMore();
    }
  },

  goBack() {
    wx.navigateBack({ delta: 1 });
  },

  onInput(e) {
    this.setData({ keyword: e.detail.value });
  },

  onConfirm() {
    this.doSearch(true);
  },

  onClear() {
    this.setData({ keyword: '', posts: [], hasSearched: false, page: 1, noMore: false });
  },

  doSearch(refresh) {
    const keyword = this.data.keyword.trim();
    if (!keyword) {
      wx.showToast({ title: '请输入搜索关键词', icon: 'none' });
      return;
    }

    if (this.data.loading) return;

    const page = refresh ? 1 : this.data.page;
    this.setData({ loading: true, hasSearched: true });

    request({
      url: '/post/search',
      data: { keyword, page, size: this.data.size },
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          const pageData = res.data.data;
          const list = (pageData.records || []).map(p => this.formatPost(p));
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
        wx.showToast({ title: '搜索失败', icon: 'none' });
      }
    });
  },

  loadMore() {
    this.doSearch(false);
  },

  formatPost(post) {
    return Object.assign({}, post, {
      timeAgo: post.createTimeStr || this.formatTime(post.createTime),
      excerpt: post.content && post.content.length > 50 
        ? post.content.substring(0, 50) + '...' 
        : post.content
    });
  },

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
  },

  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/detail/detail?id=' + id });
  }
});
