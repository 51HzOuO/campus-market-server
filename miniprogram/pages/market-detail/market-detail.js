const { request } = require('../../utils/request');

Page({
  data: { id: null, item: null, images: [], loading: true },
  onLoad(options) { if (options.id) { this.setData({ id: options.id }); this.loadDetail(); } },
  loadDetail() {
    request({ url: '/second-hand/detail/' + this.data.id, method: 'GET', success: (res) => {
      if (res.data && res.data.code === 200) {
        const item = res.data.data;
        let images = [];
        try { images = item.images ? JSON.parse(item.images) : []; } catch (e) { images = []; }
        this.setData({ item, images });
      } else wx.showToast({ title: (res.data && res.data.message) || '商品不存在', icon: 'none' });
    }, complete: () => this.setData({ loading: false }) });
  },
  previewImage(e) { wx.previewImage({ current: e.currentTarget.dataset.src, urls: this.data.images }); },
  buy() {
    if (!this.data.item || this.data.item.status !== 1) return;
    wx.showModal({ title: '确认购买', content: '请先与卖家确认商品和交付方式', success: (res) => {
      if (!res.confirm) return;
      request({ url: '/second-hand/buy', method: 'POST', data: { itemId: this.data.id }, success: (r) => {
        if (r.data && r.data.code === 200) { wx.showToast({ title: '购买成功', icon: 'success' }); this.setData({ item: r.data.data }); }
        else wx.showToast({ title: (r.data && r.data.message) || '购买失败', icon: 'none' });
      } });
    } });
  },
  offShelf() {
    request({ url: '/second-hand/off-shelf', method: 'POST', data: { itemId: this.data.id }, success: (r) => {
      if (r.data && r.data.code === 200) { wx.showToast({ title: '已下架', icon: 'success' }); this.loadDetail(); }
      else wx.showToast({ title: (r.data && r.data.message) || '操作失败', icon: 'none' });
    } });
  }
});
