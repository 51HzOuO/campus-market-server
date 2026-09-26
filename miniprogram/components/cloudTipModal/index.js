const BASE_URL = 'http://localhost:8080';

const CATEGORIES = ['最新', '跑腿代办', '二手售卖', '日常分享', '校友求助'];

const FUNC_ENTRIES = [
  { title: '跑腿代办', desc: '帮取快递 代办小事', icon: '\uD83C\uDFC3', module: 0 },
  { title: '二手售卖', desc: '闲置物品 低价转让', icon: '\uD83D\uDED2', module: 1 },
  { title: '日常分享', desc: '校园生活 随手分享', icon: '\uD83C\uDF1F', module: 2 },
  { title: '校友求助', desc: '问题咨询 经验求助', icon: '\uD83E\uDD1D', module: 3 }
];

Page({
  data: {
    statusBarHeight: 20,
    navBarTotalHeight: 64,
    schoolName: '我的学校',
    funcEntries: FUNC_ENTRIES,
    categories: CATEGORIES,
    pinnedPosts: [],
    posts: [],
    currentTab: 0,
    page: 1,
    size: 10,
    loading: false,
    noMore: false
  },

  onLoad() {
    // 获取状态栏高度，用于自定义导航栏定位
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
    this.loadPosts(true);
  },

  onPullDownRefresh() {
    Promise.all([
      this.loadPinnedPosts(),
      this.loadPosts(true)
    ]).then(() => {
      wx.stopPullDownRefresh();
    });
  },

  onReachBottom() {
    if (!this.data.noMore && !this.data.loading) {
      this.loadPosts(false);
    }
  },

  // ==================== 数据加载 ====================

  /**
   * 加载置顶帖子（最多 3 条）
   */
  loadPinnedPosts() {
    return wx.request({
      url: BASE_URL + '/post/list',
      data: { page: 1, size: 20 },
      method: 'GET'
    }).then(res => {
      if (res.statusCode === 200 && res.data.code === 200) {
        const records = res.data.data.records || [];
        const pinned = records
          .filter(p => p.isTop === 1)
          .slice(0, 3)
          .map(p => this.formatPost(p));
        this.setData({ pinnedPosts: pinned });
      }
    }).catch(() => {});
  },

  /**
   * 加载帖子列表
   */
  loadPosts(refresh) {
    if (this.data.loading) return Promise.resolve();

    const page = refresh ? 1 : this.data.page;
    const params = { page, size: this.data.size };

    // currentTab > 0 时带 module（tab 0=最新 不带 module）
    if (this.data.currentTab > 0) {
      params.module = this.data.currentTab - 1;
    }

    this.setData({ loading: true });

    return wx.request({
      url: BASE_URL + '/post/list',
      data: params,
      method: 'GET'
    }).then(res => {
      if (res.statusCode === 200 && res.data.code === 200) {
        const pageData = res.data.data;
        const list = (pageData.records || [])
          .filter(p => p.isTop !== 1) // 列表中不重复显示置顶帖
          .map(p => this.formatPost(p));
        const noMore = page >= pageData.pages;

        this.setData({
          posts: refresh ? list : this.data.posts.concat(list),
          page: page + 1,
          noMore,
          loading: false
        });
      } else {
        this.setData({ loading: false });
      }
    }).catch(() => {
      this.setData({ loading: false });
      wx.showToast({ title: '加载失败', icon: 'none' });
    });
  },

  // ==================== 交互事件 ====================

  /**
   * 切换分类标签
   */
  switchTab(e) {
    const index = e.currentTarget.dataset.index;
    if (index === this.data.currentTab) return;
    this.setData({ currentTab: index, posts: [], page: 1, noMore: false });
    this.loadPosts(true);
  },

  /**
   * 点击功能入口 → 跳到对应模块列表
   */
  goModule(e) {
    const module = e.currentTarget.dataset.module;
    const tabIndex = module + 1; // categories 中 1~4 对应 module 0~3
    this.setData({ currentTab: tabIndex, posts: [], page: 1, noMore: false });
    this.loadPosts(true);
    // 滚动到分类标签区域
    wx.pageScrollTo({ selector: '.cat-tabs', duration: 300 });
  },

  /**
   * 切换学校
   */
  switchSchool() {
    wx.showToast({ title: '学校切换暂未开放', icon: 'none' });
  },

  /**
   * 搜索
   */
  goSearch() {
    wx.showToast({ title: '搜索功能开发中', icon: 'none' });
  },

  /**
   * 跳转帖子详情
   */
  goDetail(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/detail/detail?id=' + id });
  },

  /**
   * 跳转发帖
   */
  goCreate() {
    wx.navigateTo({ url: '/pages/create/create' });
  },

  /**
   * 跳转评论
   */
  goComment(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: '/pages/detail/detail?id=' + id + '&focus=comment' });
  },

  /**
   * 点赞（占位）
   */
  toggleLike(e) {
    wx.showToast({ title: '点赞功能开发中', icon: 'none' });
  },

  // ==================== 工具方法 ====================

  /**
   * 格式化帖子数据
   */
  formatPost(post) {
    return Object.assign({}, post, {
      timeAgo: this.formatTime(post.createTime)
    });
  },

  /**
   * 时间 → 相对文本
   */
  formatTime(timeStr) {
    if (!timeStr) return '';
    const date = new Date(timeStr.replace(/-/g, '/'));
    const diff = Date.now() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return '刚刚发布';
    if (minutes < 60) return minutes + '分钟前';
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return hours + '小时前';
    const days = Math.floor(hours / 24);
    if (days < 30) return days + '天前';
    return date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate();
  }
});
