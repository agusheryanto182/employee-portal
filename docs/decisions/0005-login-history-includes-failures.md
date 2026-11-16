# 0005. Login history records failed attempts too

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
The brief says every sign in has a log history per user. It does not say whether failed attempts count.

## Decision
Record **every** login attempt in `login_history`: timestamp, success flag, failure reason, IP address, user agent, the attempted email, and the user ID when the email matches an account.
- Employees see their own history, including failed attempts on their account.
- Admins see everyone's history, with filters by user, result and date range.
- The client response for a failed login stays generic ("Invalid email or password"); the specific reason is only stored.

## Alternatives considered
- **Successful logins only:** satisfies the brief literally, but misses the most useful security signal.
- **Separate audit log for failures:** cleaner separation, but two tables and two screens for one concept.

## Consequences
- Employees can notice someone trying to get into their account ("a failed login from a device I don't recognise").
- Admins can spot brute-force attempts and support users who cannot sign in (pending or inactive account).
- The table grows with every attempt, including attacks. Rate limiting keeps it bounded, and a retention policy could be added later.
- Attempts with an unknown email have no `user_id`; they are visible to admins only.
- IP addresses are personal data, so the history is only shown to the account owner and admins.
