const { request } = require('../../utils/request');
Page({
  data: { id: null, club: {}, activities: [], joined: false },
  onLoad(options) { this.setData({ id: options.id }); this.load(); },
  load() { request({ url: '/club/detail/' + this.data.id, method: 'GET', success: r => { if (r.data && r.data.code === 200) this.setData(r.data.data); } }); },
  join() { request({ url: this.data.joined ? '/club/leave' : '/club/join', method: 'POST', data: { clubId: this.data.id }, success: r => { if (r.data && r.data.code === 200) { wx.showToast({ title: r.data.data, icon: 'success' }); this.load(); } } }); },
  activities() { wx.navigateTo({ url: '/pages/club-activity/club-activity?clubId=' + this.data.id }); }
});
