const { request } = require('../../utils/request');

const CATEGORIES = ['全部', '数码', '书籍', '生活用品', '服饰', '其他'];

Page({
  data: {
    categories: CATEGORIES,
    categoryIndex: 0,
    keyword: '',
    items: [],
    page: 1,
    noMore: false,
    loading: false
  },

  onLoad() { this.loadItems(true); },
  onShow() { if (this.data.items.length) this.loadItems(true); },
  onPullDownRefresh() { this.loadItems(true, () => wx.stopPullDownRefresh()); },
  onReachBottom() { if (!this.data.loading && !this.data.noMore) this.loadItems(false); },

  onKeywordInput(e) { this.setData({ keyword: e.detail.value }); },
  search() { this.loadItems(true); },
  selectCategory(e) {
    const index = Number(e.currentTarget.dataset.index);
    this.setData({ categoryIndex: index });
    this.loadItems(true);
  },
  goPublish() { wx.navigateTo({ url: '/pages/market-publish/market-publish' }); },
  goDetail(e) { wx.navigateTo({ url: '/pages/market-detail/market-detail?id=' + e.currentTarget.dataset.id }); },

  loadItems(refresh, callback) {
    if (this.data.loading) return;
    const page = refresh ? 1 : this.data.page;
    const params = { page, size: 10 };
    if (this.data.categoryIndex > 0) params.category = CATEGORIES[this.data.categoryIndex];
    if (this.data.keyword.trim()) params.keyword = this.data.keyword.trim();
    this.setData({ loading: true });
    request({
      url: '/second-hand/list', method: 'GET', data: params,
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const pageData = res.data.data || {};
          const records = (pageData.records || []).map(item => this.formatItem(item));
          this.setData({
            items: refresh ? records : this.data.items.concat(records),
            page: page + 1,
            noMore: pageData.pages ? page >= pageData.pages : records.length < 10
          });
        } else if (refresh) this.setData({ items: [], noMore: true });
      },
      fail: () => wx.showToast({ title: '加载失败，请重试', icon: 'none' }),
      complete: () => { this.setData({ loading: false }); if (callback) callback(); }
    });
  },

  formatItem(item) {
    let image = '';
    try { const images = item.images ? JSON.parse(item.images) : []; image = images[0] || ''; } catch (e) { image = ''; }
    return Object.assign({}, item, { cover: image });
  }
});
