const { request } = require('../../utils/request');

const MODULE_LABELS = ['跑腿代办', '二手售卖', '日常分享', '校友求助'];

Page({
  data: {
    statusBarHeight: 20,
    navBarTotalHeight: 64,
    postId: null,
    post: null,
    imageList: [],
    moduleLabels: MODULE_LABELS,
    comments: [],
    commentText: '',
    replyTo: '',
    replyToId: null,
    liked: false,
    loadingComments: false,
    showReport: false,
    reportReason: ''
  },

  onLoad(options) {
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

    if (options.id) {
      this.setData({ postId: options.id });
      this.loadPostDetail(options.id);
      this.loadComments(options.id);
    }
  },

  goBack() {
    wx.navigateBack({ delta: 1 });
  },

  /**
   * 加载帖子详情
   */
  loadPostDetail(id) {
    request({
      url: '/post/detail/' + id,
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          const post = res.data.data;
          post.timeAgo = post.createTimeStr || this.formatTime(post.createTime);

          // 解析图片
          let imageList = [];
          if (post.images) {
            try {
              const parsed = JSON.parse(post.images);
              if (Array.isArray(parsed)) {
                imageList = parsed;
              }
            } catch (e) {
              // ignore
            }
          }

          this.setData({
            post: post,
            imageList: imageList,
            liked: post.isLiked || false
          });
        } else {
          wx.showToast({ title: '帖子不存在', icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  /**
   * 加载评论列表
   */
  loadComments(postId) {
    this.setData({ loadingComments: true });

    request({
      url: '/comment/list',
      data: { postId: postId },
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          const comments = (res.data.data || []).map(c => {
            c.timeAgo = c.createTimeStr || this.formatTime(c.createTime);
            return c;
          });
          this.setData({ comments: comments });
        }
      },
      fail: () => {
        // 静默失败
      },
      complete: () => {
        this.setData({ loadingComments: false });
      }
    });
  },

  /**
   * 预览图片
   */
  previewImage(e) {
    const index = e.currentTarget.dataset.index;
    wx.previewImage({
      current: this.data.imageList[index],
      urls: this.data.imageList
    });
  },

  /**
   * 点赞/取消点赞
   */
  toggleLike() {
    if (!this.data.post) return;

    request({
      url: '/post/like',
      method: 'POST',
      data: { postId: this.data.postId },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          const result = res.data.data;
          this.setData({
            liked: result.liked,
            'post.likeCount': result.likeCount
          });
        }
      },
      fail: () => {
        wx.showToast({ title: '操作失败', icon: 'none' });
      }
    });
  },

  /**
   * 评论输入
   */
  onCommentInput(e) {
    this.setData({ commentText: e.detail.value });
  },

  /**
   * 回复评论
   */
  replyComment(e) {
    const userId = e.currentTarget.dataset.id;
    const userName = e.currentTarget.dataset.user;
    this.setData({
      replyTo: userName,
      replyToId: userId
    });
  },

  /**
   * 提交评论
   */
  submitComment() {
    const content = this.data.commentText.trim();
    if (!content) {
      wx.showToast({ title: '请输入评论内容', icon: 'none' });
      return;
    }

    if (!this.data.postId) return;

    request({
      url: '/comment/add',
      method: 'POST',
      data: {
        postId: this.data.postId,
        content: content,
        replyToId: this.data.replyToId
      },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          wx.showToast({ title: '评论成功', icon: 'success' });
          this.setData({
            commentText: '',
            replyTo: '',
            replyToId: null
          });
          // 刷新评论列表和帖子详情
          this.loadComments(this.data.postId);
          this.loadPostDetail(this.data.postId);
        } else {
          const msg = (res.data && res.data.message) || '评论失败';
          wx.showToast({ title: msg, icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
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
  },

  showReportDialog: function () {
    this.setData({ showReport: true, reportReason: '' });
  },

  hideReportDialog: function () {
    this.setData({ showReport: false, reportReason: '' });
  },

  stopEvent: function () {
    // 阻止事件冒泡
  },

  selectReason: function (e) {
    var reason = e.currentTarget.dataset.reason;
    this.setData({ reportReason: reason });
  },

  submitReport: function () {
    var that = this;
    var reason = this.data.reportReason;
    if (!reason) {
      wx.showToast({ title: '请选择举报原因', icon: 'none' });
      return;
    }

    request({
      url: '/report/add',
      method: 'POST',
      data: {
        postId: that.data.postId,
        reason: reason
      },
      success: function (res) {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          wx.showToast({ title: '举报成功', icon: 'success' });
          that.hideReportDialog();
        } else {
          var msg = (res.data && res.data.message) || '举报失败';
          wx.showToast({ title: msg, icon: 'none' });
        }
      },
      fail: function () {
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  }
});
