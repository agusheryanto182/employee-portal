# 0004. Invite link onboarding instead of emailing passwords

- **Status:** Accepted
- **Date:** 2026-11-16

## Context
Employees cannot register themselves; an admin registers them in the backoffice. The new employee needs a way to get a first password. The first idea was for the admin to set a temporary password, and then to email that password to the employee.

## Decision
The admin creates the user with name, email and role only. The system:
1. creates the user as `PENDING` without a password,
2. generates a random one-time token, stores only its SHA-256 hash, valid for 24 hours,
3. emails a link `/activate?token=...` to the employee,
4. lets the employee choose a password on that page, which activates the account and consumes the token.

Admin-triggered password resets use the same mechanism with a 1-hour token.

As a fallback, "resend invite" and "reset password" return the link to the admin, who can copy it and share it another way if the email does not arrive.

Emails go through SMTP: **Mailpit** locally (fake inbox, nothing is really sent) and **Brevo** in production (free, no domain needed).

## Alternatives considered
- **Email the password:** the password stays in the inbox forever, can be forwarded, and is exposed if the mailbox is compromised. The system (and the admin) would know the user's password. This is widely considered bad practice.
- **Admin sets a temporary password and tells the employee, forced change on first login:** no email service needed, but the admin still knows the password and has to share it through another channel.
- **Resend as the email API:** good developer experience, but without an own domain it can only send to the account owner's address.
- **Gmail SMTP with an app password:** works, but ties the project to a personal mailbox.

## Consequences
- Nobody but the employee ever knows their password.
- No "force password change" logic is needed.
- Shows integration with an external service (SMTP provider), which is in the job description.
- More work: token table, email templates, activation page, expiry and resend handling.
- Emails may land in spam; the copy-link fallback covers that, which also protects live demos.
