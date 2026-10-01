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

For local development, copy `.env.example` to an untracked `.env` file and
export the values in your shell. Docker deployments can pass the same values
with `--env-file` or with individual `-e` options. The application intentionally
fails to start when a required variable is missing.

Execute [docs/module-schema.sql](docs/module-schema.sql) once in the same
MySQL database before opening the new pages. New business content is created
as pending review and becomes visible after an auditor approves it.

The new pages are available from the home screen: **跑腿代办**, **二手售卖**,
and **社团活动**. Use **我的** to open the auditor or administrator entry,
then choose the business review or content management link.

## WeChat Developer Tools

Import the repository root (the directory containing `project.config.json`) in
WeChat Developer Tools. Its `miniprogramRoot` already points to `miniprogram/`,
where the pages are registered in `app.json`. Replace the `appid` in
`project.config.json` if this is a different Mini Program, then set the
backend domain to the deployed service URL in `miniprogram/utils/request.js`.
Log in from the **我的** tab. The backend must have the environment variables
configured and the module SQL executed before testing create, join, purchase,
or order flows.

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
