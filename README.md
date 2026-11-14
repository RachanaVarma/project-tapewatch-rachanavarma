# Project Tapewatch

Market surveillance API prototype for synthetic order and trade events. It implements event ingestion, duplicate suppression, owner pseudonymization, event-time WT-1 self-trade, WT-2 round-trip and in-memory SP-1 layering rules, PostgreSQL persistence, a seeded replay, append-only analyst dispositions/audit, and a small React analyst queue.

## Architecture

```text
JSON feed -> REST validation -> idempotent event store -> WT-1 / WT-2 / SP-1 rules
                                               |             |
                                               v             v
                                         PostgreSQL     RollingWindowStore
                                               |
                                      masked alert API -> React queue
```

Rule code depends on `RollingWindowStore`; the delivered implementation is in-memory. Account-to-owner mappings load from `src/main/resources/accounts.csv`. Client pseudonyms use HMAC-SHA256 with `MASKING_SECRET`; raw identifiers are not serialized by alert endpoints.

The alert list uses `idx_alerts_list` on `(event_time DESC, rule_id, status, instrument)`. Disposition history is appended to `alert_audit` and exposed at `GET /api/v1/alerts/{id}/audit`.

## Run

Requirements: Java 17+, Maven 3.9+, Docker Compose, Node 18+.

```sh
cp .env.example .env       # PowerShell: Copy-Item .env.example .env
docker compose up --build -d
python scripts/generate_data.py
python scripts/replay.py
```

Start the UI in another terminal after `npm install`:

```sh
cd ui
# PowerShell: $env:VITE_API_BASE_URL = "http://localhost:8080"
npm run dev
```

In a clean checkout, run `npm test` from `ui` for the React Testing Library checks.

## GitHub submission

The local repository has a `main` foundation and the `feat/surveillance-review` branch with focused commits. To meet the public-repository and pull-request steps:

1. Create a **new, empty, public** GitHub repository after the assignment start time. Do not initialize it with a README.
2. From this directory, add its URL and push both branches:

   ```sh
   git remote add origin https://github.com/<your-username>/<new-repo>.git
   git push -u origin main
   git push -u origin feat/surveillance-review
   ```

3. Open a pull request from `feat/surveillance-review` into `main`, then merge it.
4. Reply to the hiring email with the repository URL before the deadline.

The API listens at `http://localhost:8080`. `POST /api/v1/events` accepts an array of `ORDER_NEW`, `ORDER_CANCEL`, and `TRADE` event objects. Duplicate event IDs are ignored. Validation errors return HTTP 400 with invalid field names. Alerts are available at `GET /api/v1/alerts` (filters: `ruleId`, `status`, `instrument`, and `page`/`size`) and `GET /api/v1/alerts/{id}`.

`data/events.json` contains 2,127 deterministic synthetic events. `data/expected_alerts.json` is the planted ground truth: 3 WT-1, 3 WT-2, 3 SP-1 and 4 near-miss decoys. Replay prints per-rule matches and extra-alert counts. The feed is intentionally synthetic.

## Tests

Run `mvn test` for backend unit tests. They use mocks and the in-memory window store, so they do not require Docker or a live database. Run UI tests from `ui` with `npm test`.

## Design decisions and gaps

- All rule window comparisons use event timestamps; no wall-clock timestamp participates in detection.
- WT-2 compares quantity and price as a percentage of the larger value and uses the configured thresholds in `application.yml`.
- PostgreSQL stores accepted events and alert evidence IDs; Flyway owns the schema.
- WT-1, WT-2 and SP-1 run against the in-memory `RollingWindowStore`; an SP-1 cooldown suppresses repeated alerts. Redis-backed shared state, Redis TTL, Redis integration tests, a load script, and measured load numbers are not implemented. The in-memory store is per process and must not be used for multi-instance production operation. The UI supports queue filters, server-side pagination, evidence review, audit history, escalation, and closing false positives.
- The replay and local setup are provided, but this environment did not have a Java/Maven runtime, so no measured test or latency output is claimed. Run the documented commands and paste real output into this README before submission.
- Next: Redis sorted-set implementation with TTL and cross-instance cooldown, Redis integration tests, load measurement, and UI escalation controls.

## AI usage

See [AI_USAGE.md](AI_USAGE.md) for tools used, prompts, and review notes.
