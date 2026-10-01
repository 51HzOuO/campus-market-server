const { request } = require('../../utils/request');

Page({
  data: { id: '', order: null, loading: false, currentUserId: null },

  onLoad(options) {
    const user = wx.getStorageSync('userInfo') || {};
    this.setData({ id: options.id || '', currentUserId: user.userId || user.id || null });
    this.load();
  },

  load() {
    if (!this.data.id) return;
    this.setData({ loading: true });
    request({
      url: '/errand/detail/' + this.data.id,
      method: 'GET',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          this.setData({ order: this.formatOrder(res.data.data) });
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '订单不存在', icon: 'none' });
        }
      },
      fail: () => wx.showToast({ title: '加载失败', icon: 'none' }),
      complete: () => this.setData({ loading: false })
    });
  },

  formatOrder(order) {
    if (!order) return order;
    return Object.assign({}, order, {
      priceText: Number(order.price || 0).toFixed(2),
      createText: order.createTime ? String(order.createTime).replace('T', ' ').slice(0, 16) : '',
      acceptedText: order.acceptedTime ? String(order.acceptedTime).replace('T', ' ').slice(0, 16) : ''
    });
  },

  goBack() { wx.navigateBack({ delta: 1 }); },

  action(e) {
    const action = e.currentTarget.dataset.action;
    const id = this.data.id;
    if (action === 'cancel') {
      wx.showModal({ title: '取消订单', content: '确定取消这笔跑腿订单吗？', success: (r) => { if (r.confirm) this.sendAction(action, id); } });
      return;
    }
    if (action === 'pay') {
      wx.showModal({ title: '确认支付', content: '本次将支付 ¥' + this.data.order.priceText + '，确认继续吗？', success: (r) => { if (r.confirm) this.sendAction(action, id); } });
      return;
    }
    this.sendAction(action, id);
  },

  sendAction(action, id) {
    const names = { accept: '接单成功', pay: '支付成功', complete: '已提交完成', confirm: '订单已完成', cancel: '订单已取消' };
    request({
      url: '/errand/' + id + '/' + action,
      method: 'POST',
      success: (res) => {
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: names[action] || '操作成功', icon: 'success' });
          this.setData({ order: this.formatOrder(res.data.data) });
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '操作失败', icon: 'none' });
        }
      },
      fail: () => wx.showToast({ title: '网络错误，请重试', icon: 'none' })
    });
  }
});
