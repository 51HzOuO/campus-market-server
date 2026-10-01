const { request, BASE_URL } = require('../../utils/request');
const CATEGORIES = ['数码', '书籍', '生活用品', '服饰', '其他'];

Page({
  data: { categories: CATEGORIES, categoryIndex: 0, title: '', description: '', price: '', location: '', contact: '', images: [], submitting: false },
  input(e) { this.setData({ [e.currentTarget.dataset.field]: e.detail.value }); },
  selectCategory(e) { this.setData({ categoryIndex: Number(e.currentTarget.dataset.index) }); },
  chooseImage() {
    const count = 9 - this.data.images.length; if (count <= 0) return;
    wx.chooseMedia({ count, mediaType: ['image'], sourceType: ['album', 'camera'], sizeType: ['compressed'], success: (res) => {
      const paths = res.tempFiles.map(f => f.tempFilePath); this.setData({ images: this.data.images.concat(paths) });
    } });
  },
  deleteImage(e) { const images = this.data.images.slice(); images.splice(Number(e.currentTarget.dataset.index), 1); this.setData({ images }); },
  uploadImage(path) {
    return new Promise((resolve, reject) => wx.uploadFile({ url: BASE_URL + '/upload/image', filePath: path, name: 'file', header: { Authorization: 'Bearer ' + (wx.getStorageSync('token') || '') }, success: (res) => { try { const body = JSON.parse(res.data); if (body.code === 200) resolve(body.data.fullUrl || body.data.url); else reject(new Error(body.message)); } catch (e) { reject(e); } }, fail: reject }));
  },
  submit() {
    const d = this.data; if (d.submitting) return;
    if (!d.title.trim() || !d.description.trim() || d.price === '') return wx.showToast({ title: '请填写标题、描述和价格', icon: 'none' });
    const price = Number(d.price); if (Number.isNaN(price) || price < 0) return wx.showToast({ title: '请输入有效价格', icon: 'none' });
    this.setData({ submitting: true });
    Promise.all(d.images.map(path => this.uploadImage(path))).then(images => new Promise((resolve, reject) => request({ url: '/second-hand/create', method: 'POST', data: { title: d.title.trim(), description: d.description.trim(), price, category: CATEGORIES[d.categoryIndex], location: d.location.trim(), contact: d.contact.trim(), images: JSON.stringify(images) }, success: resolve, fail: reject }))).then(res => {
      if (res.data && res.data.code === 200) { wx.showToast({ title: '已提交审核', icon: 'success' }); setTimeout(() => wx.navigateBack({ delta: 1 }), 900); }
      else wx.showToast({ title: (res.data && res.data.message) || '发布失败', icon: 'none' });
    }).catch(() => wx.showToast({ title: '图片上传或网络失败', icon: 'none' })).then(() => this.setData({ submitting: false }));
  }
});
