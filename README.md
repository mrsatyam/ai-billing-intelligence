# AI Billing Intelligence Platform

Self-learning billing decision engine for P&C insurers (India demo: INR / UPI).

Hybrid AI: **Gemini** when `billing.ai.gemini.api-key` is set, otherwise a rich **rule-based** engine so the demo always works offline.

---

## Quick start

**Requirements:** Java 17+ and Maven 3.9+

```powershell
cd ai-billing-intelligence
mvn spring-boot:run
```

Open **http://localhost:8080**

### Enable Gemini (optional)

In [`src/main/resources/application.properties`](src/main/resources/application.properties):

```properties
billing.ai.gemini.api-key=YOUR_GEMINI_API_KEY
billing.ai.gemini.model=gemini-3.1-flash-lite
```

Or via environment:

```powershell
$env:GEMINI_API_KEY="YOUR_GEMINI_API_KEY"
mvn spring-boot:run
```

Check provider: **http://localhost:8080/api/ai/status**

> Do not commit real API keys. Prefer env vars or a local untracked override.

### Build runnable JAR

```powershell
mvn -DskipTests package
java -jar target/ai-billing-intelligence-0.0.1-SNAPSHOT.jar
```

---

## Judge / demo flow

0. **Pitch deck** `/presentation` — 8 slides (problem → how AI helps → click path)  
1. **Dashboard** `/` — KPIs, risk doughnut, collection trend, region heat map, recent AI decisions  
2. Click **Policies at Risk** — table with policy, customer, risk %, recommendation  
3. Open a customer (try **P1234 / John**) — AI analysis, explainability, **Approve**  
4. Generate **Email / Call Script / Payment Plans** on the analysis page  
5. **Simulator** `/simulator` — click **Run AI** for scan animation + recovery + streamed recommendations  
6. Floating **AI chat** (bottom-right) — ask billing health / John’s risk / save today  

H2 console: **http://localhost:8080/h2-console**  
JDBC URL `jdbc:h2:mem:billingdb`, user `sa`, blank password.

---

## Features

| Area | What you get |
|------|----------------|
| Dashboard | Premium due, collection rate, at-risk count, leakage, AI recs + charts |
| Delinquency | Risk score + factors (rules + optional Gemini narrative) |
| Collection AI | WhatsApp / agent call / installments / AutoPay / grace |
| Explainable AI | Why the score is high + mitigating signals |
| Comms | AI email + call script (on demand) |
| Plans | 3 / 6 / 9 month installment options |
| Chat | Floating assistant with KPI / policy context |
| Simulator | Bulk scan with animated progress + recommendation parade |

Seed data: **100** India-localized policies (`billing.seed.policy-count`).

---

## Architecture

```
Browser (Thymeleaf + Bootstrap + Chart.js)
        │
        ▼
Spring Boot MVC + REST
        │
        ├── Dashboard / Policy / Decision / Simulator / Chat
        │
        ▼
AiFacade (Gemini when keyed → else RuleBasedAiEngine)
        │
        ▼
H2 (default)  |  Oracle profile + SQL scripts under db/oracle/
```

---

## Tech stack

- Java 17+ / Spring Boot 3.3 / Spring Web / Thymeleaf / Spring Data JPA  
- H2 (default) · Oracle scripts + `application-oracle.properties`  
- Gemini API (hybrid) · Bootstrap 5 · Chart.js · Vanilla JS  
- Single Spring Boot JAR

---

## Oracle (optional)

```powershell
# Apply SQL*Plus scripts, then:
mvn spring-boot:run "-Dspring-boot.run.profiles=oracle" "-Dbilling.seed.enabled=false"
```

Scripts: [`src/main/resources/db/oracle/schema.sql`](src/main/resources/db/oracle/schema.sql), [`seed-data.sql`](src/main/resources/db/oracle/seed-data.sql)

Set `ORACLE_JDBC_URL`, `ORACLE_USER`, `ORACLE_PASSWORD`.

---

## Useful APIs

| Method | Path |
|--------|------|
| GET | `/api/dashboard/metrics` |
| GET | `/api/ai/status` |
| GET | `/api/ai/policies/{id}/predict` |
| GET | `/api/ai/policies/{id}/email` |
| GET | `/api/ai/policies/{id}/call-script` |
| GET | `/api/ai/policies/{id}/payment-plans` |
| POST | `/api/chat` body `{"message":"..."}` |
| POST | `/api/simulator/run` |
| POST | `/api/decisions/{id}/approve` |

---

## Project goal

Move from traditional “manage bills” workflows to an **autonomous billing intelligence** loop: predict → recommend → communicate → approve → recover.
