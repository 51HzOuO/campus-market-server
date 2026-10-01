const { requestAsync, uploadImage, toAbsoluteUrl } = require('../../utils/request');
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
  async submit() {
    const d = this.data; if (d.submitting) return;
    if (!d.title.trim() || !d.description.trim() || d.price === '') return wx.showToast({ title: '请填写标题、描述和价格', icon: 'none' });
    const price = Number(d.price); if (!Number.isFinite(price) || price < 0) return wx.showToast({ title: '请输入有效价格', icon: 'none' });
    this.setData({ submitting: true });
    try {
      const images = [];
      for (const filePath of d.images) {
        const image = await uploadImage(filePath);
        const url = image && (image.fullUrl || toAbsoluteUrl(image.url));
        if (!url) throw new Error('图片上传未返回有效地址，请重试');
        images.push(url);
      }
      await requestAsync({
        url: '/second-hand/create',
        method: 'POST',
        data: { title: d.title.trim(), description: d.description.trim(), price, category: CATEGORIES[d.categoryIndex], location: d.location.trim(), contact: d.contact.trim(), images: JSON.stringify(images) }
      });
      wx.showToast({ title: '已提交审核', icon: 'success' });
      setTimeout(() => wx.navigateBack({ delta: 1 }), 900);
    } catch (err) {
      if (!err || !err.notified) {
        wx.showToast({ title: (err && err.message) || '发布失败，请重试', icon: 'none' });
      }
    } finally {
      this.setData({ submitting: false });
    }
  }
});
