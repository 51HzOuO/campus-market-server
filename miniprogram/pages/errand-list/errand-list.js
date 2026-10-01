const { request } = require('../../utils/request');

Page({
  data: {
    orders: [],
    scope: 'available',
    loading: false,
    emptyText: '暂无可接的跑腿需求'
  },

  onShow() {
    this.load();
  },

  onPullDownRefresh() {
    this.load(() => wx.stopPullDownRefresh());
  },

  switchScope(e) {
    const scope = e.currentTarget.dataset.scope;
    if (scope === this.data.scope) return;
    this.setData({ scope, orders: [], emptyText: scope === 'mine' ? '还没有自己的订单' : '暂无可接的跑腿需求' });
    this.load();
  },

  load(done) {
    if (this.data.loading) return;
    this.setData({ loading: true });
    request({
      url: '/errand/list',
      method: 'GET',
      data: { scope: this.data.scope },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          this.setData({ orders: (res.data.data || []).map(this.formatOrder) });
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '加载失败', icon: 'none' });
        }
      },
      fail: () => wx.showToast({ title: '网络错误，请重试', icon: 'none' }),
      complete: () => {
        this.setData({ loading: false });
        if (typeof done === 'function') done();
      }
    });
  },

  formatOrder(order) {
    return Object.assign({}, order, {
      priceText: order.price == null ? '0.00' : Number(order.price).toFixed(2),
      timeText: order.createTime ? String(order.createTime).replace('T', ' ').slice(0, 16) : ''
    });
  },

  goCreate() {
    wx.navigateTo({ url: '/pages/errand-create/errand-create' });
  },

  goDetail(e) {
    wx.navigateTo({ url: '/pages/errand-detail/errand-detail?id=' + e.currentTarget.dataset.id });
  }
});
