Page({
  data: {
    statusBarHeight: 20,
    navBarTotalHeight: 64
  },

  onLoad() {
    try {
      const sysInfo = wx.getSystemInfoSync();
      const statusBarHeight = sysInfo.statusBarHeight || 20;
      this.setData({
        statusBarHeight: statusBarHeight,
        navBarTotalHeight: statusBarHeight + 44
      });
    } catch (e) {
      // ignore
    }
  },

  goBack() {
    wx.navigateBack({ delta: 1 });
  }
});
