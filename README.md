# Campus Market Server

## Configuration

The application does not contain database or WeChat credentials. Set these
environment variables before starting it:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | MySQL JDBC connection URL |
| `DB_USERNAME` | MySQL application user |
| `DB_PASSWORD` | MySQL application user password |
| `WX_APP_ID` | WeChat Mini Program app ID |
| `WX_APP_SECRET` | WeChat Mini Program app secret |
| `APP_PUBLIC_URL` | Your production HTTPS API domain, used in image/avatar URLs |
| `WECHAT_API_URL` | Optional HTTPS `jscode2session` endpoint (defaults to WeChat) |

With a CloudBase managed MySQL binding, the `prod` profile accepts
`MYSQL_ADDRESS`, `MYSQL_USERNAME`, and `MYSQL_PASSWORD`; it defaults to the
`campus_market` database. Set `MYSQL_JDBC_URL` when the database name differs.
When both variable sets are present, the `MYSQL_*` values are used in `prod`
and `DB_*` remains the fallback for other profiles. To use a complete custom
`DB_URL` while keeping the `prod` profile, set it as `MYSQL_JDBC_URL`.

For local development, copy `.env.example` to an untracked `.env` file and
export the values in your shell. Docker deployments can pass the same values
with `--env-file` or with individual `-e` options. The application intentionally
fails to start when a required variable is missing.

If `/auth/login` returns `502` and the server log reports `WeChat login service
request failed`, the deployed service cannot reach the WeChat API. Check the
CloudBase service's outbound DNS/HTTPS access, or set `WECHAT_API_URL` to an
approved HTTPS proxy forwarding the same query parameters. Wrong credentials
are reported separately as a configuration mismatch.

Execute [docs/module-schema.sql](docs/module-schema.sql) once in the same
MySQL database before opening the new pages. New business content is created
as pending review and becomes visible after an auditor approves it.

The new pages are available from the home screen: **跑腿代办**, **二手售卖**,
and **社团活动**. Use **我的** to open the auditor or administrator entry,
then choose the business review or content management link.

Uploaded images are currently stored under `/tmp/uploads` and `/tmp/avatars`
inside the CloudBase container. They are validated and served immediately, but
the container filesystem is ephemeral: configure CloudBase/COS object storage
before production use if images must survive a service restart or redeploy.

跑腿支付当前为演示模式：点击支付不会调用微信支付或产生扣款，会直接将
订单标记为“交易成功（模拟支付）”。

## WeChat Developer Tools

Import the repository root (the directory containing `project.config.json`) in
WeChat Developer Tools. Its `miniprogramRoot` already points to `miniprogram/`,
where the pages are registered in `app.json`. Replace the `appid` in
`project.config.json` if this is a different Mini Program. Before an official
release, map your own HTTPS domain to the service, set that same domain as
`APP_PUBLIC_URL`, replace `BASE_URL` in `miniprogram/utils/request.js`, and add
only that custom domain to the Mini Program `request`, `uploadFile`, and
`downloadFile` legal-domain allowlists. The `*.sh.run.tcloudbase.com` address
is for testing and should not be used as the production allowlist domain.
Log in from the **我的** tab. The backend must have the environment variables
configured and the module SQL executed before testing create, join, purchase,
or order flows.

The root `Dockerfile` uses a multi-stage build and runs Maven inside the image,
so CloudBase can build directly from a clean Git checkout; no ignored
`target/` directory or prebuilt JAR needs to be committed.

## GitHub Actions / deployment

Add the five variables above under **Settings > Secrets and variables > Actions**
as repository or environment secrets. Pass them to the deployed process as
runtime environment variables; GitHub Actions secrets are not automatically
available to a server after deployment.

The credentials that were previously committed must be revoked and replaced.
Removing them from the current file does not remove them from existing Git
objects, forks, or clones. After rotating them, rewrite the repository history
with an approved history-cleaning procedure and force-push the rewritten
branches, then ask collaborators to re-clone.
