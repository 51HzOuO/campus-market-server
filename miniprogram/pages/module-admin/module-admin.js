const { request } = require('../../utils/request');

Page({
  data: { types: ['帖子', '二手商品', '跑腿订单', '社团'], typeValues: ['post', 'market', 'errand', 'club'], typeIndex: 0, standalone: false, items: [], loading: false },
  onLoad(options) { const hasType = options && this.data.typeValues.indexOf(options.type) >= 0; const index = hasType ? this.data.typeValues.indexOf(options.type) : 0; this.setData({ typeIndex: index, standalone: Boolean(hasType) }); this.load(); },
  changeType(e) {
    const value = e.currentTarget && e.currentTarget.dataset.index !== undefined
      ? e.currentTarget.dataset.index : e.detail.value;
    this.setData({ typeIndex: Number(value), items: [] });
    this.load();
  },
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
        this.setData({ items: data.map((item) => this.formatItem(item, type)) });
      },
      complete: () => this.setData({ loading: false })
    }));
  },
  formatItem(item, type) {
    const audit = Number(item.auditStatus);
    let statusText = '展示中';
    if (type === 'post') statusText = Number(item.isTop) === 1 ? '已置顶' : '正常';
    else if (type === 'club') statusText = ({ 0: '待审核', 1: '已通过', 2: '未通过', 3: '已下架' })[Number(item.status)] || '未知状态';
    else if (audit === 0) statusText = '待审核';
    else if (audit === 2) statusText = '未通过';
    else if (type === 'market') statusText = ({ 0: '已下架', 1: '在售', 2: '已售出' })[Number(item.status)] || '未知状态';
    else statusText = ({ 1: '待接单', 2: '已接单', 3: '进行中', 4: '待确认', 5: '已完成', 6: '已取消' })[Number(item.status)] || '未知状态';
    let cover = '';
    if (type === 'market' && item.images) {
      try { const images = JSON.parse(item.images); cover = Array.isArray(images) ? images[0] || '' : ''; } catch (_) {}
    }
    const status = Number(item.status);
    const canToggle = type === 'post'
      || (type === 'market' && audit === 1 && [0, 1].includes(status))
      || (type === 'errand' && audit === 1 && [1, 6].includes(status))
      || (type === 'club' && [1, 3].includes(status));
    let actionText = '';
    if (type === 'post') actionText = Number(item.isTop) === 1 ? '取消置顶' : '设为置顶';
    else if (type === 'market') actionText = status === 0 ? '恢复在售' : '下架商品';
    else if (type === 'errand') actionText = status === 6 ? '恢复接单' : '取消订单';
    else actionText = status === 3 ? '恢复展示' : '下架社团';
    return Object.assign({}, item, { statusText, cover, canToggle, actionText });
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
    if (!item.canToggle) {
      wx.showToast({ title: '当前状态不可直接上下架', icon: 'none' });
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
