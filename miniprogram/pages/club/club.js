const { request } = require('../../utils/request');
Page({
  data: { clubs: [], loading: false },
  onShow() { this.load(); },
  load() { this.setData({ loading: true }); request({ url: '/club/list', method: 'GET', success: r => { if (r.data && r.data.code === 200) this.setData({ clubs: r.data.data || [] }); }, complete: () => this.setData({ loading: false }) }); },
  goCreate() { wx.navigateTo({ url: '/pages/club-create/club-create' }); },
  detail(e) { wx.navigateTo({ url: '/pages/club-detail/club-detail?id=' + e.currentTarget.dataset.id }); }
});
