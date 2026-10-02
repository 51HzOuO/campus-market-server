const { request } = require('../../utils/request');

Page({
  data: { types: ['帖子', '二手商品', '跑腿订单', '社团'], typeValues: ['post', 'market', 'errand', 'club'], typeIndex: 0, standalone: false, items: [], loading: false },
  onLoad(options) { const hasType = options && this.data.typeValues.indexOf(options.type) >= 0; const index = hasType ? this.data.typeValues.indexOf(options.type) : 0; this.setData({ typeIndex: index, standalone: Boolean(hasType) }); this.load(); },
  changeType(e) { this.setData({ typeIndex: Number(e.detail.value), items: [] }); this.load(); },
  load() {
    this.setData({ loading: true });
    const type = this.data.typeValues[this.data.typeIndex];
    const options = type === 'post'
      ? { url: '/admin/posts', data: { page: 1, size: 50 }, method: 'GET' }
      : { url: '/admin/modules/list', data: { type }, method: 'GET' };
    request(Object.assign(options, {
      success: (r) => {
        if (!r.data || r.data.code !== 200) return;
        const data = type === 'post' ? ((r.data.data && r.data.data.records) || []) : (r.data.data || []);
        this.setData({ items: data });
      },
      complete: () => this.setData({ loading: false })
    }));
  },
  toggle(e) {
    const item = e.currentTarget.dataset.item;
    const type = this.data.typeValues[this.data.typeIndex];
    if (type === 'post') {
      request({ url: '/admin/top', method: 'POST', data: { postId: item.id }, success: (r) => {
        if (r.data && r.data.code === 200) { wx.showToast({ title: r.data.data || '已更新', icon: 'success' }); this.load(); }
      }});
      return;
    }
    const hideStatus = type === 'errand' ? 6 : (type === 'market' ? 0 : 3);
    const restoreStatus = type === 'market' ? 1 : 1;
    const status = Number(item.status) === hideStatus ? restoreStatus : hideStatus;
    request({ url: '/admin/modules/status', method: 'POST', data: { type, id: item.id, status }, success: (r) => { if (r.data && r.data.code === 200) { wx.showToast({ title: '已更新', icon: 'success' }); this.load(); } } });
  },
  deletePost(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({ title: '删除帖子', content: '确定删除这条帖子吗？', success: (r) => {
      if (!r.confirm) return;
      request({ url: '/post/delete', method: 'POST', data: { postId: id }, success: (res) => {
        if (res.data && res.data.code === 200) { wx.showToast({ title: '已删除', icon: 'success' }); this.load(); }
      }});
    }});
  },
  goBack() { wx.navigateBack({ delta: 1 }); }
});
