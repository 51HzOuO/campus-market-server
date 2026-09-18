const { request } = require('../../utils/request');

const BASE_URL = 'https://shturl.cc/6yhbRYwXpob6wFJvTlKwoHjGcaQ4rMBZ5pUjEIgwooqozFkz83DI40Ug';

Page({
  data: {
    statusBarHeight: 20,
    navBarTotalHeight: 64,
    userInfo: {
      nickname: '',
      avatar: '',
      activityLevel: '新手上路',
      activityColor: '#999999',
      activityScore: 0,
      studentNo: ''
    },
    postCount: 0,
    // 昵称编辑
    editingNickname: false,
    tempNickname: '',
    // 学号编辑
    editingStudentNo: false,
    tempStudentNo: ''
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
    this.loadUserInfo();
  },

  onShow() {
    this.loadUserInfo();
  },

  goBack() {
    wx.navigateBack({ delta: 1 });
  },

  /**
   * 加载用户信息
   */
  loadUserInfo() {
    request({
      url: '/user/info',
      method: 'GET',
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          const data = res.data.data;
          this.setData({
            userInfo: {
              nickname: data.nickname || '校园用户',
              avatar: data.avatar || '',
              activityLevel: data.activityLevel || '新手上路',
              activityColor: data.activityColor || '#999999',
              activityScore: data.activityScore || 0,
              studentNo: data.studentNo || ''
            },
            postCount: data.postCount || 0
          });
        }
      },
      fail: () => {
        // 静默失败
      }
    });
  },

  // ==================== 头像上传 ====================

  /**
   * 选择并上传头像
   */
  chooseAvatar() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempFilePath = res.tempFilePaths[0];
        this.uploadAvatar(tempFilePath);
      }
    });
  },

  /**
   * 上传头像到后端
   */
  uploadAvatar(filePath) {
    wx.showLoading({ title: '上传中...' });

    const token = wx.getStorageSync('token') || '';

    wx.uploadFile({
      url: BASE_URL + '/user/upload-avatar',
      filePath: filePath,
      name: 'file',
      header: {
        'Authorization': token ? ('Bearer ' + token) : ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          try {
            const data = JSON.parse(res.data);
            if (data.code === 200 && data.data && data.data.url) {
              wx.showToast({ title: '头像已更新', icon: 'success' });
              this.setData({ 'userInfo.avatar': data.data.url });
            } else {
              const msg = data.message || '上传失败';
              wx.showToast({ title: msg, icon: 'none' });
            }
          } catch (e) {
            wx.showToast({ title: '解析失败', icon: 'none' });
          }
        } else {
          wx.showToast({ title: '上传失败', icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' });
      },
      complete: () => {
        wx.hideLoading();
      }
    });
  },

  // ==================== 昵称编辑 ====================

  editNickname() {
    if (this.data.editingNickname) return;
    this.setData({
      editingNickname: true,
      tempNickname: this.data.userInfo.nickname || ''
    });
  },

  onNicknameInput(e) {
    this.setData({ tempNickname: e.detail.value });
  },

  saveNickname() {
    const nickname = this.data.tempNickname.trim();
    if (!nickname) {
      wx.showToast({ title: '昵称不能为空', icon: 'none' });
      return;
    }
    if (nickname.length > 20) {
      wx.showToast({ title: '昵称不能超过20个字', icon: 'none' });
      return;
    }
    if (nickname === this.data.userInfo.nickname) {
      this.setData({ editingNickname: false });
      return;
    }

    request({
      url: '/user/update',
      method: 'POST',
      data: { nickname: nickname },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          wx.showToast({ title: '修改成功', icon: 'success' });
          this.setData({
            editingNickname: false,
            'userInfo.nickname': nickname
          });
        } else {
          const msg = (res.data && res.data.message) || '修改失败';
          wx.showToast({ title: msg, icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  },

  cancelNicknameEdit() {
    this.setData({ editingNickname: false });
  },

  // ==================== 学号编辑 ====================

  editStudentNo() {
    if (this.data.editingStudentNo) return;
    this.setData({
      editingStudentNo: true,
      tempStudentNo: this.data.userInfo.studentNo || ''
    });
  },

  onStudentNoInput(e) {
    this.setData({ tempStudentNo: e.detail.value });
  },

  saveStudentNo() {
    const studentNo = this.data.tempStudentNo.trim();
    if (studentNo.length > 20) {
      wx.showToast({ title: '学号不能超过20位', icon: 'none' });
      return;
    }
    if (studentNo === this.data.userInfo.studentNo) {
      this.setData({ editingStudentNo: false });
      return;
    }

    request({
      url: '/user/update',
      method: 'POST',
      data: { studentNo: studentNo },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 200) {
          wx.showToast({ title: '修改成功', icon: 'success' });
          this.setData({
            editingStudentNo: false,
            'userInfo.studentNo': studentNo
          });
        } else {
          const msg = (res.data && res.data.message) || '修改失败';
          wx.showToast({ title: msg, icon: 'none' });
        }
      },
      fail: () => {
        wx.showToast({ title: '网络错误', icon: 'none' });
      }
    });
  },

  cancelStudentNoEdit() {
    this.setData({ editingStudentNo: false });
  }
});
