# AI Billing Intelligence — Prompt Catalog

This document lists:

1. **Your Cursor prompts** — what you asked the coding agent to build (the requirement and follow-ups).
2. **Gemini prompts** — what the running app sends to Google Gemini today (`AiFacade` + `GeminiClient`).

Source of Gemini prompts: `src/main/java/com/capstone/billing/ai/AiFacade.java` and `GeminiClient.java`.

---

## Part 1 — Prompts you gave Cursor

### 1.1 Original product requirement (11 Aug 2026)

```
In this project we need to create below idea from scratch:

AI Billing Intelligence Platform

A self-learning billing decision engine for P&C insurers

Instead of:
Manage Bills

Build:
AI decides what should happen next for every policy.

Tech Stack
Backend: Java 21, Spring Boot 3.x, Spring AI (or OpenAI API), Spring Data JPA, H2/MySQL, REST APIs
Frontend: HTML, Bootstrap 5, Vanilla JS, Chart.js, Spring Boot static resources
Deploy: Single Spring Boot Jar

Demo Flow
Imagine the judge opening the application.

Dashboard — insurance command center
- Premium Due Today $4.8M
- Collection Rate 92%
- Policies at Risk 284
- Predicted Revenue Leakage $380K
- AI Recommendations 54
- Beautiful charts, heat map, Recent AI Decisions

Click "Policies at Risk"
Policy | Customer | Risk | AI Recommendation
P1234  | John     | 91%  | Offer Installments
P8934  | Mike     | 86%  | Agent Call
P1982  | Alice    | 82%  | WhatsApp Reminder

Click John → AI Analysis Page
Customer Profile, Payment History, Income Segment, Policy Type, Claims, Payment Behaviour

Huge AI box — AI Reasoning
Customer has missed payments twice during festive months.
Salary credited on 5th.
Preferred payment method: UPI
Recommendation:
✔ Delay reminder until 6th
✔ Offer 3-month installment
✔ Waive late fee
Predicted success 87%

Manager clicks Approve → Spring Boot saves AI Decision → workflow updates.

AI Features
1. Delinquency Prediction
   Input: Age, Premium, Past Due, Claims, Income Segment, Payment History, Occupation, Payment Method
   AI returns Risk Score (e.g. 84%)

2. AI Collection Recommendation
   Prompt:
   You are an Insurance Billing Expert.
   Customer:
   Premium = 2500
   Missed Payments = 2
   Claims = None
   Suggest collection strategy.
   Returns: Friendly Reminder, Installment Plan, Avoid Legal Notice, Notify Agent

3. AI Email Generator — Subject, Body, Tone, Language

4. AI Call Script for collection agents
   Hello John, I noticed your premium...

5. AI Payment Plan Generator
   Customer owes ₹38,000
   AI generates 3 / 6 / 9 months + best recommendation

6. AI Risk Explanation
   Instead of Risk = 89%, explain WHY (missed payments, credit, cancellation, vehicle, inflation area).
   Executives LOVE explainable AI.

7. AI Chat Assistant — floating chatbot
   Why is John's score high?
   How many policies should we save today?
   Summarize today's billing health.

Fake but Realistic Data
Generate 1000 policies. Every policy has Policy, Customer, Premium, Claims, Due Date, Payment History, Occupation, Region, AutoPay, Risk Score.

Killer Feature — Autonomous Decision Simulator
Judge clicks Run AI → animation scanning 1000 policies
AI Found: 312 risky customers, 48 premium leakages, 82 policies likely to lapse
Potential Recovery ₹18,42,000
Then recommendations appear one by one:
✔ WhatsApp Reminder, Agent Call, Flexible Installment, AutoPay Discount, Grace Period

Architecture
Browser → Spring Boot MVC → Billing Controller → AI Decision Engine → OpenAI API → Decision Service → H2 → Dashboard
```

### 1.2 Locked product decisions

| Date | Your prompt |
|------|-------------|
| 11 Aug 10:58 | `hybrid + india` |
| 11 Aug 11:01 | Implement the attached plan. Do not edit the plan file. Complete all to-dos. |
| 11 Aug 11:01 | `lets do initial commit with first todo task first` |
| 11 Aug 11:07 | `not yaml use application.properties instead` |
| 11 Aug 11:10 | `remove com.axis // make it com.capstone` |
| 11 Aug 12:03 | `ok todo task 2.. (lets create 100 entries only for now) create another oracle sql seeder too along with h2 seeder.` |
| 11 Aug 19:59 | `use Gemini` (after choosing Gemini over ChatGPT Plus for low budget) |
| 11 Aug 20:44 | `revrt it.. i will fetch from properties file only` (API key not in DB) |
| 11 Aug 21:18 | `no we are using application.properties` |
| 12 Aug 09:44 | `generate a quick presentation for demo.. that explain what this app does and how ai helps` |
| 12 Aug 13:12 | `AI reasoning message is correct? considering I am admin looking at others cases?` |
| 12 Aug 16:08 | `ok update it` (third-person admin tone for AI Reasoning) |

### 1.3 Implementation steering (short)

These were build/debug instructions, not product copy:

- `ok implement next` / `next` / `ok.. next task` / `ok next`
- `ok commit`
- `we are using oracle as db right?`
- `how do I access the ui running it on local?`
- `at this moment any actual ai prediction is happening?`
- `I have chat gpt plus access.. can I use that key ? or I have gemini api key as well.. low budget`
- `how to fetch gemini api key?`
- Gemini 404 model errors (`gemini-2.0-flash`, `gemini-2.5-flash-lite`) — update to a live model
- Presentation: explain / fix / `revert this change` for the “Judges see magic narratives” slide

An API key was pasted in chat once. **Do not put keys in this file or in git.** Use `billing.ai.gemini.api-key` in local `application.properties` only.

### 1.4 Collection-strategy prompt from your original spec

This is the example you wrote in the requirement. The live Gemini version is in Part 2 (enriched with India context and JSON).

```
You are an Insurance Billing Expert.

Customer:
Premium = 2500
Missed Payments = 2
Claims = None

Suggest collection strategy.
```

---

## Part 2 — Prompts the app sends to Gemini

Gemini is used only when `billing.ai.gemini.api-key` is set. Scores, installment math, and simulator counts stay on the **rule engine**. Gemini rewrites narratives, emails, scripts, chat, and recommendation wording.

How a call is built (`GeminiClient`):

```
{system prompt}

{user prompt}
```

For JSON endpoints, this line is appended to the system prompt:

```
Respond with ONLY valid minified JSON. No markdown fences.
```

Every policy-scoped call also prepends this **context block** (values filled at runtime):

```
Customer: {name}
Policy: {policyNumber} ({policyType})
Premium: {premium}
Age: {age} | Occupation: {occupation} | Income: {incomeSegment} | Region: {region}
Payment method: {preferredPaymentMethod} | Salary credit day: {salaryCreditDay} | AutoPay: {ON|OFF}
Missed payments: {n} | Late: {n} | Claims: {n} | Festive miss pattern: {true|false}
Stored risk score: {score}
```

Allowed `primaryAction` / `supportingActions` values:

`WHATSAPP_REMINDER`, `AGENT_CALL`, `OFFER_INSTALLMENTS`, `AUTOPAY_DISCOUNT`, `GRACE_PERIOD`, `WAIVE_LATE_FEE`, `FRIENDLY_REMINDER`, `DELAY_REMINDER`

---

### 2.1 System prompt A — admin / internal (default)

Used for: delinquency summary, collection recommendation, risk explanation, payment-plan rationale, chat.

```
You are an Insurance Billing Expert briefing P&C billing managers and collection admins in India.
Write in third person about the customer (name, "the customer", "this policy") — never "you" or "your".
Be concise and practical. Prefer UPI, installments, and agent calls over legal notices.
Currency is INR (₹). Never invent policy numbers.
```

### 2.2 System prompt B — customer-facing

Used for: email generator, call script.

```
You are an Insurance Billing Expert drafting customer-facing copy for P&C insurers in India.
Address the policyholder in second person ("you" / "your"). Be empathetic and practical.
Prefer UPI, installments, and agent calls over legal notices. Currency is INR (₹).
Never invent policy numbers.
```

---

### 2.3 Delinquency prediction (narrative only)

**System:** A (admin)

**User:**

```
{context block}

Write an internal admin summary (third person). Return JSON: {"summary": string, "factors": string[] }
Use riskScore={ruleScore} as given. Keep factors short.
```

---

### 2.4 Collection recommendation

**System:** A (admin)

**User:**

```
{context block}

Suggest a collection strategy for the billing admin.
Reasoning must be third person (about the customer, not to them). Return JSON:
{"primaryAction": one of [WHATSAPP_REMINDER, AGENT_CALL, OFFER_INSTALLMENTS, AUTOPAY_DISCOUNT, GRACE_PERIOD, WAIVE_LATE_FEE, FRIENDLY_REMINDER, DELAY_REMINDER],
 "supportingActions": string[], "reasoning": string, "predictedSuccess": number 55-95,
 "avoidActions": string[] }
```

---

### 2.5 Risk explanation (AI Reasoning box)

**System:** A (admin)

**User:**

```
{context block}

Risk score is {ruleScore}%. Write an internal briefing for a billing admin reviewing this case.
Use third person only (e.g. the customer / this policy / their occupation).
Do not address the policyholder. Do not draft a customer message. Return JSON:
{"headline": string, "whyHighRisk": string[], "mitigatingFactors": string[], "narrative": string}
```

---

### 2.6 Email generator

**System:** B (customer-facing)

**User:**

```
{context block}

Generate a collection email for India. Return JSON:
{"subject": string, "body": string, "tone": string, "language": string}
```

---

### 2.7 Call script

**System:** B (customer-facing)

**User:**

```
{context block}

Write a short collection call script for an Indian agent (Hinglish OK). Return JSON:
{"opening": string, "fullScript": string, "closing": string, "tone": string}
```

---

### 2.8 Payment plan rationale

Installment amounts are computed by rules. Gemini only explains why the chosen tenure fits.

**System:** A (admin)

**User:**

```
{context block}

We already computed plans. Best is {N} months.
Return JSON: {"rationale": string} explaining why that tenure fits this customer.
```

---

### 2.9 Floating chat

**System:** A (admin)

**User:**

```
Manager question: {user question}
{optional context block}
{optional} Dashboard KPIs: atRisk={n}, collectionRate={n}%, premiumDueToday={amount}, leakage={amount}, pendingRecs={n}

Answer in 2-5 short sentences for an insurance billing manager.
```

Chat is **text**, not JSON (no “minified JSON” instruction).

---

## Part 3 — What Gemini does *not* do

These stay on `RuleBasedAiEngine` even when the API key is set:

- Numeric **risk score**
- **3 / 6 / 9 month** installment math
- Simulator **scan counts** and bulk recommendations
- Offline demo when Gemini is missing or returns 404

Hybrid rule: Gemini enriches language; business numbers stay grounded in rules.
