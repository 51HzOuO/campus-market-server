const { request } = require('../../utils/request');

Page({
  data: {
    clubId: null,
    activities: [],
    title: '',
    content: '',
    startTime: '',
    location: '',
    maxParticipants: ''
  },

  onLoad(options) {
    this.setData({ clubId: options.clubId || null });
    this.load();
  },

  load() {
    if (!this.data.clubId) return;
    request({
      url: '/club/activity/list',
      method: 'GET',
      data: { clubId: this.data.clubId },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          const activities = (res.data.data || []).map(activity => Object.assign({}, activity, {
            // Accept the canonical field and older compatible response names.
            joined: [activity.joined, activity.isJoined, activity.joinedByCurrentUser]
              .some(value => value === true || value === 1 || value === '1'),
            actioning: false
          }));
          this.setData({ activities });
        }
      },
      fail: (error) => {
        wx.showToast({ title: (error && error.message) || '活动加载失败，请重试', icon: 'none' });
      }
    });
  },

  toggleActivity(e) {
    const activityId = Number(e.currentTarget.dataset.id);
    const index = this.data.activities.findIndex(activity => Number(activity.id) === activityId);
    if (!activityId || index < 0) return;
    const activity = this.data.activities[index];
    if (activity.actioning) return;

    const joining = !activity.joined;
    this.setData({ [`activities[${index}].actioning`]: true });
    request({
      url: joining ? '/club/activity/join' : '/club/activity/leave',
      method: 'POST',
      data: { activityId },
      showError: false,
      success: (res) => {
        if (res.data && res.data.code === 200) {
          this.setData({
            [`activities[${index}].joined`]: joining,
            [`activities[${index}].actioning`]: false
          });
          wx.showToast({ title: joining ? '报名成功' : '已取消报名', icon: 'success' });
        } else {
          this.setData({ [`activities[${index}].actioning`]: false });
          wx.showToast({ title: (res.data && res.data.message) || '操作失败', icon: 'none' });
        }
      },
      fail: (error) => {
        this.setData({ [`activities[${index}].actioning`]: false });
        wx.showToast({ title: (error && error.message) || '操作失败，请重试', icon: 'none' });
      }
    });
  },

  // Keep the original handler name available to callers that still use it.
  joinActivity(e) {
    this.toggleActivity(e);
  },

  input(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value });
  },

  submit() {
    const startTime = (this.data.startTime || '').trim().replace(' ', 'T');
    request({
      url: '/club/activity/create',
      method: 'POST',
      data: {
        clubId: this.data.clubId,
        title: this.data.title,
        content: this.data.content,
        startTime,
        location: this.data.location,
        maxParticipants: this.data.maxParticipants ? Number(this.data.maxParticipants) : null
      },
      success: (res) => {
        if (res.data && res.data.code === 200) {
          wx.showToast({ title: '已提交审核', icon: 'success' });
          this.setData({ title: '', content: '', startTime: '', location: '', maxParticipants: '' });
        }
      }
    });
  }
});
