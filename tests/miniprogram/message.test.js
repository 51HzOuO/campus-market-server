const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');

function loadMessagePage(request) {
  let definition;
  const navigations = [];
  const toasts = [];
  const context = {
    Page: value => { definition = value; },
    require: () => ({ request }),
    wx: {
      navigateTo: options => navigations.push(options),
      showToast: options => toasts.push(options)
    },
    console: { warn() {} }
  };
  const filename = path.join(__dirname, '../../miniprogram/pages/message/message.js');
  vm.runInNewContext(fs.readFileSync(filename, 'utf8'), context, { filename });
  const page = { ...definition, data: { ...definition.data }, setData(changes) { Object.assign(this.data, changes); } };
  return { page, navigations, toasts };
}

test('message screen requests notifications once on show and maps returned notices', () => {
  const calls = [];
  const { page } = loadMessagePage(options => {
    calls.push(options.url);
    if (options.url === '/notice/list') options.success({ statusCode: 200, data: { code: 200, data: [{ id: 2, postId: 9, content: '新评论', isRead: 0 }] } });
    else options.success({ statusCode: 200, data: { code: 200, data: {} } });
  });
  page.onShow();
  assert.equal(calls.filter(url => url === '/notice/list').length, 1);
  assert.equal(page.data.messages[0].postId, 9);
});

test('message tap opens its post and reports missing ids or navigation failure', () => {
  const { page, navigations, toasts } = loadMessagePage(() => {});
  page.goPost({ currentTarget: { dataset: { id: 42 } } });
  assert.equal(navigations[0].url, '/pages/detail/detail?id=42');
  navigations[0].fail(new Error('blocked'));
  page.goPost({ currentTarget: { dataset: {} } });
  assert.equal(toasts[0].title, '打开帖子失败，请返回重试');
  assert.equal(toasts[1].title, '这条通知没有关联帖子');
});
