# CryptoX — Backend

A full-stack cryptocurrency trading platform backend built with Spring Boot. Supports JWT authentication with email-based 2FA, real-time market data, wallet management, order execution with P&L tracking, payment gateway integration, an AI chatbot grounded in live data, and a role-based admin dashboard.

**Frontend repo:** [cryptox-frontend](https://github.com/PriyaMhapralkar/cryptox-frontend)

## Tech Stack

- **Backend:** Spring Boot 4, Spring Security, Spring Data JPA (Hibernate), Java Mail Sender

- **Database:** MySQL 8

- **Auth:** JWT (jjwt), BCrypt password hashing

- **Testing:** JUnit 5, Mockito

- **External APIs:**

  - [CoinGecko](https://www.coingecko.com/en/api) — live market data (Demo tier)

  - [Google Gemini API](https://ai.google.dev/) — AI chatbot, grounded with live platform data

  - [Razorpay](https://razorpay.com/docs/) — payment gateway (fully tested, India-based test mode)

  - Public RSS feeds (Cointelegraph, Decrypt) — coin-specific news, since dedicated crypto news APIs (CryptoPanic, CryptoCompare) have moved to paid-only tiers

## Features

- Registration, login, JWT auth, email OTP-based 2FA, forgot password

- Live market data sync (scheduled every 3 minutes), pagination, gainers/losers filtering, historical price charts

- Wallet: balance top-up (via Razorpay), peer-to-peer transfer, withdrawal requests

- Buy/sell orders with weighted-average cost basis and automatic profit/loss tracking

- Portfolio, watchlist, trading activity history

- Withdrawal approval workflow with admin controls

- AI chatbot (Gemini) grounded in live coin prices, aggregate market queries (gainers/losers), and platform-aware answers

- Per-coin news + AI-generated "why is this coin moving" insight

- Role-based admin dashboard: user management (block/unblock), withdrawal approvals, transaction monitoring, wallet oversight, audit logs

## Setup

### Prerequisites

- JDK 17+

- MySQL 8

- Maven (or use the Eclipse-bundled Maven)

### Environment Variables

Set these in your IDE run configuration or system environment:

| Variable | Description |

|---|---|

| `DB_PASSWORD` | MySQL root/user password |

| `GMAIL_USERNAME` | Gmail address used for sending OTP emails |

| `GMAIL_APP_PASSWORD` | Gmail App Password (not your regular password) |

| `COINGECKO_API_KEY` | CoinGecko Demo API key |

| `JWT_SECRET` | Long random string for JWT signing |

| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | Razorpay test-mode credentials |

| `GEMINI_API_KEY` | Google AI Studio API key |

### Run

```bash

mvn spring-boot:run

```

The API starts on `http://localhost:8080`. MySQL schema is auto-created via Hibernate (`ddl-auto=update`).

### Run Tests

```bash

mvn test

```

## Known Limitations

- **Stripe integration** is implemented (server-side PaymentIntent creation + verification) but not live-tested — Stripe currently restricts new India-based signups to invite-only business accounts. Razorpay is fully tested end-to-end instead.

- **Crypto news** is sourced from public RSS feeds (Cointelegraph, Decrypt) rather than a dedicated news API, since CryptoPanic and CryptoCompare/CoinDesk both moved their APIs behind paywalls after this project began. Coverage is strong for major coins, sparser for smaller altcoins.

- **Email sending** uses personal Gmail SMTP, which has a ~500 email/day sending limit — fine for demo/personal use; a production deployment would use a dedicated transactional email service (SendGrid, AWS SES, etc.).