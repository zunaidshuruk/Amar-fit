# Making notifications arrive within minutes (not hours)

**The problem:** messages, friend requests and challenge results are sent by a small job on GitHub (`push-notifications.yml`). It is scheduled every 5 minutes, but GitHub throttles scheduled jobs on free plans: over the six days checked (2026-10-11 to 2026-10-17) it ran only every 2 to 10 hours (median about 4.5 hours). So these notifications can arrive hours late. The app is not at fault, and Firebase Cloud Functions are not an option on the free plan.

**The fix:** a free outside timer that asks GitHub to start the job every 5 minutes. GitHub starts jobs it is asked for directly (a "dispatch") without the throttling. The job already supports this (`workflow_dispatch`), is safe to run often (every message and request is marked as sent, so nothing is sent twice) and now runs one at a time (`concurrency`). The repository is public, so the minutes are free.

## Steps for Zunaid (about 10 minutes, nothing to install)

### 1. Create a GitHub token that can only start this job
1. GitHub, top right profile picture, **Settings**, left menu bottom **Developer settings**, **Personal access tokens**, **Fine-grained tokens**, **Generate new token**.
2. Name: `kardiq-timer`. Expiration: 1 year (put a reminder in your calendar to renew it).
3. **Repository access:** *Only select repositories*, choose **zunaidshuruk/Amar-fit**.
4. **Permissions, Repository permissions:** set **Actions** to **Read and write**. Leave everything else as it is (Metadata read-only is added automatically).
5. **Generate token** and copy it now (GitHub shows it only once). Treat it like a password.

### 2. Create the timer on cron-job.org (free)
1. Sign up at **cron-job.org** (free plan allows a job every minute).
2. **Create cronjob**. Title: `KardIQ push`.
3. **URL:** `https://api.github.com/repos/zunaidshuruk/Amar-fit/actions/workflows/push-notifications.yml/dispatches`
4. **Execution schedule:** every 5 minutes.
5. Open **Advanced**:
   * **Request method:** POST
   * **Headers** (add three):
     * `Authorization` = `Bearer ` followed by your token (with the word Bearer and a space)
     * `Accept` = `application/vnd.github+json`
     * `X-GitHub-Api-Version` = `2022-11-28`
   * **Request body:** `{"ref":"main"}`
6. Tick **Notify me on failure**, then **Create**.
7. Click **Test run**. A good answer is **HTTP 204** (no content). 401 or 403 means the token or its Actions permission is wrong; 404 means the URL has a typo.

### 3. Check that it works
GitHub, the repository, **Actions**, **push-notifications**: within 10 minutes you should see runs whose trigger is **workflow_dispatch**, about every 5 minutes. Tell Claude when it is on and Claude will measure the real gaps from the run history and record them here.

## Notes
* The old schedule stays as a backup.
* If the token is ever exposed, delete it on GitHub (Settings, Developer settings) and make a new one; the timer then needs the new value.
* If you would rather not use an outside service, the other free option is a Cloudflare Worker with a cron trigger that makes the same request (more setup).
* Result log: (empty until the timer is on).
