const { request } = require('../../utils/request');

Page({
  data: { types: ['跑腿订单', '二手商品', '社团申请', '社团活动'], typeValues: ['errand', 'market', 'club', 'activity'], typeIndex: 0, items: [], loading: false },
  onLoad() { this.load(); },
  changeType(e) { this.setData({ typeIndex: Number(e.detail.value), items: [] }); this.load(); },
  load() {
    this.setData({ loading: true });
    request({ url: '/module-audit/list', data: { type: this.data.typeValues[this.data.typeIndex] }, method: 'GET',
      success: (res) => { if (res.data && res.data.code === 200) this.setData({ items: res.data.data || [] }); },
      complete: () => this.setData({ loading: false }) });
  },
  review(e) {
    const id = e.currentTarget.dataset.id;
    const status = Number(e.currentTarget.dataset.status);
    wx.showModal({ title: status === 1 ? '通过审核' : '驳回审核', editable: true, placeholderText: '备注（可选）',
      success: (res) => { if (!res.confirm) return; request({ url: '/module-audit/review', method: 'POST', data: { type: this.data.typeValues[this.data.typeIndex], id, status, remark: res.content || '' }, success: (r) => { if (r.data && r.data.code === 200) { wx.showToast({ title: '已处理', icon: 'success' }); this.load(); } } }); } });
  },
  goBack() { wx.navigateBack({ delta: 1 }); }
});
