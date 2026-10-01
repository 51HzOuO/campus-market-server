const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

function loadPage(name, services = {}) {
  let definition;
  const toasts = [];
  const loading = [];
  const context = {
    Page: value => { definition = value; },
    require: moduleName => {
      assert.equal(moduleName, '../../utils/request');
      return {
        toAbsoluteUrl: value => value ? (/^https?:\/\//.test(value) ? value : 'https://api.example.test' + value) : '',
        ...services
      };
    },
    setTimeout: () => 0,
    wx: {
      showToast: options => toasts.push(options),
      showLoading: options => loading.push(options),
      hideLoading: () => loading.push(null)
    }
  };
  const filename = path.join(__dirname, '../../miniprogram/pages', name, name + '.js');
  vm.runInNewContext(readFileSync(filename, 'utf8'), context, { filename });
  const page = {
    ...definition,
    data: JSON.parse(JSON.stringify(definition.data)),
    setData(changes) {
      for (const [key, value] of Object.entries(changes)) {
        const parts = key.split('.');
        let target = this.data;
        for (const part of parts.slice(0, -1)) target = target[part];
        target[parts[parts.length - 1]] = value;
      }
    }
  };
  return { page, toasts, loading };
}

function fillPost(page) {
  page.setData({
    selectedModule: 2,
    title: '校园生活',
    content: '这是用于验证发布流程的校园生活分享。',
    images: ['wxfile://first.jpg', 'wxfile://second.jpg'],
    canPublish: true
  });
}

test('post waits for every upload and sends server image URLs in order', async () => {
  const events = [];
  const requests = [];
  const { page } = loadPage('publish', {
    uploadImage: async filePath => {
      events.push(filePath);
      return filePath.endsWith('first.jpg')
        ? { fullUrl: 'https://cdn.example.test/first.jpg' }
        : { url: '/uploads/second.jpg' };
    },
    requestAsync: async options => { events.push('create'); requests.push(options); }
  });
  fillPost(page);

  await page.submitPost();

  assert.deepEqual(events, ['wxfile://first.jpg', 'wxfile://second.jpg', 'create']);
  assert.equal(requests[0].url, '/post/create');
  assert.deepEqual(JSON.parse(requests[0].data.images), [
    'https://cdn.example.test/first.jpg', 'https://api.example.test/uploads/second.jpg'
  ]);
  assert.equal(page.data.submitting, false);
});

test('failed image upload prevents posting, preserves form, and permits a retry', async () => {
  let failUpload = true;
  let creates = 0;
  const { page, toasts } = loadPage('publish', {
    uploadImage: async () => {
      if (failUpload) throw new Error('图片超过大小限制');
      return { url: '/uploads/retry.jpg' };
    },
    requestAsync: async () => { creates++; }
  });
  fillPost(page);
  const before = JSON.stringify({ title: page.data.title, content: page.data.content, images: page.data.images });

  await page.submitPost();

  assert.equal(creates, 0);
  assert.equal(page.data.submitting, false);
  assert.equal(toasts[0].title, '图片超过大小限制');
  assert.equal(JSON.stringify({ title: page.data.title, content: page.data.content, images: page.data.images }), before);
  failUpload = false;
  await page.submitPost();
  assert.equal(creates, 1);
  assert.equal(page.data.submitting, false);
});

test('double taps during upload do not submit duplicate posts', async () => {
  let completeUpload;
  let uploads = 0;
  let creates = 0;
  const { page } = loadPage('publish', {
    uploadImage: () => {
      uploads++;
      return new Promise(resolve => { completeUpload = resolve; });
    },
    requestAsync: async () => { creates++; }
  });
  fillPost(page);
  page.setData({ images: ['wxfile://first.jpg'] });
  const firstSubmit = page.submitPost();
  await page.submitPost();
  assert.equal(uploads, 1);
  assert.equal(creates, 0);
  assert.equal(page.data.submitting, true);
  completeUpload({ url: '/uploads/first.jpg' });
  await firstSubmit;
  assert.equal(creates, 1);
});

test('post rejection retains the server error and unlocks submission', async () => {
  const { page, toasts } = loadPage('publish', {
    uploadImage: async () => ({ url: '/uploads/post.jpg' }),
    requestAsync: async () => { throw new Error('内容审核暂不可用，请稍后重试'); }
  });
  fillPost(page);
  await page.submitPost();
  assert.equal(page.data.submitting, false);
  assert.equal(page.data.title, '校园生活');
  assert.equal(toasts[0].title, '内容审核暂不可用，请稍后重试');
});

test('missing upload URLs cannot produce a post with invalid image references', async () => {
  let creates = 0;
  const { page, toasts } = loadPage('publish', {
    uploadImage: async () => ({}),
    requestAsync: async () => { creates++; }
  });
  fillPost(page);
  await page.submitPost();
  assert.equal(creates, 0);
  assert.equal(page.data.submitting, false);
  assert.match(toasts[0].title, /未返回有效地址/);
});

test('market publishing uses shared upload and preserves the failure for retry', async () => {
  let failUpload = true;
  const requests = [];
  const { page, toasts } = loadPage('market-publish', {
    uploadImage: async () => {
      if (failUpload) throw new Error('请先完成微信登录');
      return { url: '/uploads/item.jpg' };
    },
    requestAsync: async options => { requests.push(options); }
  });
  page.setData({ title: '二手教材', description: '书籍无缺页', price: '12.50', images: ['wxfile://item.jpg'] });
  await page.submit();
  assert.equal(requests.length, 0);
  assert.equal(page.data.submitting, false);
  assert.equal(page.data.title, '二手教材');
  assert.equal(toasts[0].title, '请先完成微信登录');
  failUpload = false;
  await page.submit();
  assert.equal(requests.length, 1);
  assert.equal(requests[0].url, '/second-hand/create');
  assert.equal(requests[0].data.price, 12.5);
  assert.deepEqual(JSON.parse(requests[0].data.images), ['https://api.example.test/uploads/item.jpg']);
});

test('avatar uploads share the authenticated helper and normalize returned paths', async () => {
  const calls = [];
  const { page, loading } = loadPage('profile', {
    uploadImage: async (...args) => { calls.push(args); return { url: '/uploads/avatar.jpg' }; }
  });
  await page.uploadAvatar('wxfile://avatar.jpg');
  assert.deepEqual(calls, [['wxfile://avatar.jpg', '/user/upload-avatar']]);
  assert.equal(page.data.userInfo.avatar, 'https://api.example.test/uploads/avatar.jpg');
  assert.equal(page.data.uploadingAvatar, false);
  assert.equal(loading[loading.length - 1], null);
});

test('avatar failure preserves the current avatar and allows another upload', async () => {
  const { page, toasts, loading } = loadPage('profile', {
    uploadImage: async () => { throw new Error('上传服务不可用，请稍后重试'); }
  });
  page.setData({ 'userInfo.avatar': 'https://cdn.example.test/existing.jpg' });
  await page.uploadAvatar('wxfile://avatar.jpg');
  assert.equal(page.data.userInfo.avatar, 'https://cdn.example.test/existing.jpg');
  assert.equal(page.data.uploadingAvatar, false);
  assert.equal(toasts[0].title, '上传服务不可用，请稍后重试');
  assert.equal(loading[loading.length - 1], null);
});
