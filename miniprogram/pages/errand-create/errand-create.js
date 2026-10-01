const { request } = require('../../utils/request');

Page({
  data: {
    title: '',
    description: '',
    pickupLocation: '',
    deliveryLocation: '',
    distanceKm: '',
    weightKg: '',
    urgent: 0,
    contactPhone: '',
    price: '0.00',
    submitting: false,
    calculating: false
  },

  goBack() {
    wx.navigateBack({ delta: 1 });
  },

  input(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ [field]: e.detail.value });
    if (field === 'distanceKm' || field === 'weightKg') this.calculate();
  },

  toggleUrgent() {
    this.setData({ urgent: this.data.urgent === 1 ? 0 : 1 });
    this.calculate();
  },

  calculate() {
    const distance = Number(this.data.distanceKm);
    const weight = Number(this.data.weightKg);
    if (!Number.isFinite(distance) || !Number.isFinite(weight) || distance < 0 || weight < 0) {
      this.setData({ price: '0.00' });
      return;
    }
    if (this.data.calculating) return;
    this.setData({ calculating: true });
    request({
      url: '/errand/calculate',
      method: 'POST',
      data: { distanceKm: distance, weightKg: weight, urgent: this.data.urgent },
      success: (res) => {
        if (res.data && res.data.code === 200 && res.data.data) {
          this.setData({ price: Number(res.data.data.price || 0).toFixed(2) });
        }
      },
      complete: () => this.setData({ calculating: false })
    });
  },

  submit() {
    const d = this.data;
    if (d.submitting) return;
    if (!d.title.trim() || !d.description.trim() || !d.pickupLocation.trim() || !d.deliveryLocation.trim()) {
      wx.showToast({ title: '请填写完整需求', icon: 'none' });
      return;
    }
    const distance = Number(d.distanceKm);
    const weight = Number(d.weightKg);
    if (!Number.isFinite(distance) || distance < 0 || !Number.isFinite(weight) || weight < 0) {
      wx.showToast({ title: '请填写正确的距离和重量', icon: 'none' });
      return;
    }
    this.setData({ submitting: true });
    request({
      url: '/errand/create',
      method: 'POST',
      data: {
        title: d.title.trim(),
        description: d.description.trim(),
        pickupLocation: d.pickupLocation.trim(),
        deliveryLocation: d.deliveryLocation.trim(),
        distanceKm: distance,
        weightKg: weight,
        urgent: d.urgent,
        contactPhone: d.contactPhone.trim()
      },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          wx.showModal({
            title: '提交成功',
            content: '需求已提交审核，审核通过后会出现在可接单列表。',
            showCancel: false,
            success: () => wx.navigateBack({ delta: 1 })
          });
        } else {
          wx.showToast({ title: (res.data && res.data.message) || '发布失败', icon: 'none' });
        }
      },
      fail: () => wx.showToast({ title: '网络错误，请重试', icon: 'none' }),
      complete: () => this.setData({ submitting: false })
    });
  }
});
