const { request } = require('../../utils/request');

Page({
  data: {
    messages: [],
    stats: {
      postCount: 0,
      totalLikes: 0,
      totalComments: 0
    },
    loading: true
  },

  onLoad() {
    this.loadStats();
    this.loadMessages();
  },

  onShow() {
    this.loadStats();
    this.loadMessages();
  },

  /**
   * 加载消息通知列表
   */
  loadMessages() {
    request({
      url: '/notice/list',
      method: 'GET',
      success: (res) => {
        console.log('notice/list 响应:', res);
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          const messages = (res.data.data || []).map(item => {
            return {
              id: item.id,
              postId: item.postId,
              avatar: item.avatar || '',
              text: item.content,
              timeAgo: item.createTimeStr || this.formatTime(item.createTime),
              isRead: item.isRead === 1
            };
          });
          this.setData({ messages: messages });
        } else {
          console.log('notice/list 返回异常:', res.data);
          this.setData({ messages: [] });
        }
      },
      fail: (err) => {
        console.log('notice/list 请求失败:', err);
        this.setData({ messages: [] });
      },
      complete: () => {
        this.setData({ loading: false });
      }
    });
  },

  /**
   * 加载帖子统计数据
   */
  loadStats() {
    request({
      url: '/user/info',
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          const data = res.data.data;
          this.setData({
            stats: {
              postCount: data.postCount || 0,
              totalLikes: data.likeCount || 0,
              totalComments: data.commentCount || 0
            }
          });
        }
      },
      fail: () => {
        // 静默失败
      }
    });
  },

  /**
   * 点击消息 → 跳转对应帖子
   */
  goPost(e) {
    const id = e.currentTarget.dataset.id;
    if (id) {
      wx.navigateTo({ url: '/pages/detail/detail?id=' + id });
    }
  },

  /**
   * 跳转发帖
   */
  goCreate() {
    wx.navigateTo({ url: '/pages/publish/publish' });
  },

  /**
   * 格式化时间
   */
  formatTime(timeVal) {
    if (!timeVal) return '';
    let date;
    if (typeof timeVal === 'number') {
      date = new Date(timeVal);
    } else if (Array.isArray(timeVal)) {
      date = new Date(timeVal[0], timeVal[1] - 1, timeVal[2], timeVal[3] || 0, timeVal[4] || 0, timeVal[5] || 0);
    } else if (typeof timeVal === 'string') {
      date = new Date(timeVal.replace(/-/g, '/'));
    } else {
      date = new Date(timeVal);
    }
    if (isNaN(date.getTime())) return '';
    const diff = Date.now() - date.getTime();
    const minutes = Math.floor(diff / 60000);
    if (minutes < 1) return '刚刚';
    if (minutes < 60) return minutes + '分钟前';
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return hours + '小时前';
    const days = Math.floor(hours / 24);
    if (days < 30) return days + '天前';
    return (date.getMonth() + 1) + '-' + date.getDate();
  }
});
