# Security Policy

VocaTa is a personal open-source project that integrates account data, AI provider keys,
WebSocket streaming, PostgreSQL, Redis, and third-party STT / LLM / TTS services. Treat
security reports as private until a fix or mitigation has been released.

## Supported Versions

The project has not published stable versioned releases yet. Security fixes target:

| Target | Supported |
| --- | --- |
| `develop` branch | Yes |
| Active release / hotfix branches | Yes, when present |
| Old feature branches | No |

## Reporting a Vulnerability

Do not open a public issue for a suspected vulnerability.

Report privately through one of these channels:

- Open a private GitHub security advisory if repository access allows it.
- Contact the maintainer privately and include the minimum details needed to reproduce.

Please include:

- affected branch or commit
- affected module, endpoint, WebSocket path, or deployment flow
- reproduction steps or proof-of-concept notes
- expected impact, such as token exposure, auth bypass, data write, injection, SSRF, or secret leakage
- whether the issue is already public or actively exploitable

## Handling Expectations

- Initial triage target: within 7 days.
- Critical issues involving auth bypass, token exposure, or remote code execution should be prioritized before feature work.
- Accepted reports should receive a fix plan, mitigation, or a clear explanation when the behavior is not considered a vulnerability.
- Public disclosure should wait until a fix is merged or a mitigation is documented.

## Security Baselines

Before merging security-sensitive changes:

- run the relevant validation script from `docs/验证清单.md`
- check that no secrets, tokens, passwords, private keys, or production-only config are committed
- keep real env files such as `.env` and `.env.*` untracked; only `.env.example` may be committed as a template
- review auth and permission checks when touching `/api/client/**`, `/api/admin/**`, Sa-Token config, or `UserContext`
- document deployment or rollback risk in the PR
