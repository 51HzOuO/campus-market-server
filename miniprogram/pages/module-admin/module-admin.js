const { request } = require('../../utils/request');

Page({
  data: { types: ['跑腿订单', '二手商品', '社团'], typeValues: ['errand', 'market', 'club'], typeIndex: 0, standalone: false, items: [], loading: false },
  onLoad(options) { const hasType = options && this.data.typeValues.indexOf(options.type) >= 0; const index = hasType ? this.data.typeValues.indexOf(options.type) : 0; this.setData({ typeIndex: index, standalone: Boolean(hasType) }); this.load(); },
  changeType(e) { this.setData({ typeIndex: Number(e.detail.value), items: [] }); this.load(); },
  load() { this.setData({ loading: true }); request({ url: '/admin/modules/list', data: { type: this.data.typeValues[this.data.typeIndex] }, method: 'GET', success: (r) => { if (r.data && r.data.code === 200) this.setData({ items: r.data.data || [] }); }, complete: () => this.setData({ loading: false }) }); },
  toggle(e) {
    const item = e.currentTarget.dataset.item;
    const type = this.data.typeValues[this.data.typeIndex];
    const hideStatus = type === 'errand' ? 6 : (type === 'market' ? 0 : 3);
    const restoreStatus = type === 'market' ? 1 : 1;
    const status = Number(item.status) === hideStatus ? restoreStatus : hideStatus;
    request({ url: '/admin/modules/status', method: 'POST', data: { type, id: item.id, status }, success: (r) => { if (r.data && r.data.code === 200) { wx.showToast({ title: '已更新', icon: 'success' }); this.load(); } } });
  },
  goBack() { wx.navigateBack({ delta: 1 }); }
});
