const { request } = require('../../utils/request');

const MODULES = ['跑腿代办', '二手售卖', '日常分享', '校友求助'];

Page({
  data: {
    statusBarHeight: 20,
    navBarTotalHeight: 64,
    modules: MODULES,
    selectedModule: -1,
    title: '',
    content: '',
    images: [],
    canPublish: false,
    submitting: false
  },

  onLoad() {
    try {
      const sysInfo = wx.getSystemInfoSync();
      const statusBarHeight = sysInfo.statusBarHeight || 20;
      const navBarTotalHeight = statusBarHeight + 44;
      this.setData({ statusBarHeight, navBarTotalHeight });
    } catch (e) {
      // ignore
    }
  },

  /**
   * 返回上一页
   */
  goBack() {
    wx.navigateBack({ delta: 1 });
  },

  /**
   * 选择模块
   */
  selectModule(e) {
    const index = e.currentTarget.dataset.index;
    this.setData({ selectedModule: index });
    this.checkCanPublish();
  },

  /**
   * 标题输入
   */
  onTitleInput(e) {
    this.setData({ title: e.detail.value });
    this.checkCanPublish();
  },

  /**
   * 内容输入
   */
  onContentInput(e) {
    this.setData({ content: e.detail.value });
    this.checkCanPublish();
  },

  /**
   * 检查是否满足发布条件
   */
  checkCanPublish() {
    const { selectedModule, title, content } = this.data;
    const canPublish = selectedModule >= 0
      && title.trim().length > 0
      && content.trim().length >= 10;
    this.setData({ canPublish });
  },

  /**
   * 选择图片
   */
  chooseImage() {
    const remaining = 9 - this.data.images.length;
    if (remaining <= 0) return;

    wx.chooseMedia({
      count: remaining,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      sizeType: ['compressed'],
      success: (res) => {
        const newPaths = res.tempFiles.map(f => f.tempFilePath);
        this.setData({
          images: [...this.data.images, ...newPaths]
        });
      }
    });
  },

  /**
   * 预览图片
   */
  previewImage(e) {
    const index = e.currentTarget.dataset.index;
    wx.previewImage({
      current: this.data.images[index],
      urls: this.data.images
    });
  },

  /**
   * 删除图片
   */
  deleteImage(e) {
    const index = e.currentTarget.dataset.index;
    const images = [...this.data.images];
    images.splice(index, 1);
    this.setData({ images });
  },

  /**
   * 提交帖子
   */
  submitPost() {
    if (!this.data.canPublish || this.data.submitting) return;

    const { selectedModule, title, content, images } = this.data;

    // 校验
    if (selectedModule < 0) {
      wx.showToast({ title: '请选择模块', icon: 'none' });
      return;
    }
    if (!title.trim()) {
      wx.showToast({ title: '请输入标题', icon: 'none' });
      return;
    }
    if (content.trim().length < 10) {
      wx.showToast({ title: '内容至少10个字', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });

    request({
      url: '/post/create',
      method: 'POST',
      data: {
        module: selectedModule,
        title: title.trim(),
        content: content.trim(),
        images: images.length > 0 ? JSON.stringify(images) : '[]'
      },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '发布成功', icon: 'success' });
          setTimeout(() => {
            wx.navigateBack({ delta: 1 });
          }, 1000);
        } else {
          const msg = (res.data && res.data.message) || '发布失败';
          wx.showToast({ title: msg, icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络错误，请重试', icon: 'none' });
      },
      complete: () => {
        this.setData({ submitting: false });
      }
    });
  },

  /**
   * 跳转用户协议
   */
  goAgreement() {
    wx.showToast({ title: '用户协议页面开发中', icon: 'none' });
  },

  /**
   * 跳转社区规范
   */
  goDisclaimer() {
    wx.showToast({ title: '社区规范页面开发中', icon: 'none' });
  }
});
