-- AI Billing Intelligence — Oracle seed data (100 India-localized policies)
-- Prerequisites: schema.sql applied. Safe to re-run after truncate/drop.

DELETE FROM ai_decisions;
DELETE FROM payment_history;
DELETE FROM claims;
DELETE FROM policies;
DELETE FROM customers;
COMMIT;

DECLARE
  TYPE str_tab IS TABLE OF VARCHAR2(80) INDEX BY PLS_INTEGER;

  first_names str_tab;
  last_names  str_tab;
  occupations str_tab;
  regions     str_tab;

  v_cust_id   customers.id%TYPE;
  v_pol_id    policies.id%TYPE;
  v_name      VARCHAR2(120);
  v_age       NUMBER;
  v_occ       VARCHAR2(80);
  v_income    VARCHAR2(20);
  v_region    VARCHAR2(80);
  v_pay       VARCHAR2(20);
  v_salary    NUMBER;
  v_ptype     VARCHAR2(20);
  v_premium   NUMBER(12, 2);
  v_autopay   NUMBER(1);
  v_due       DATE;
  v_risk      NUMBER;
  v_status    VARCHAR2(20);
  v_missed    NUMBER;
  v_claims    NUMBER;
  v_rec       VARCHAR2(40);
  v_pm        NUMBER;
  v_due_day   DATE;
  v_festive   BOOLEAN;
  v_is_missed NUMBER(1);
  v_is_late   NUMBER(1);
  v_paid      DATE;
  i           PLS_INTEGER;
  m           PLS_INTEGER;
  c           PLS_INTEGER;
BEGIN
  first_names(1) := 'Aarav'; first_names(2) := 'Vivaan'; first_names(3) := 'Aditya';
  first_names(4) := 'Vihaan'; first_names(5) := 'Arjun'; first_names(6) := 'Ananya';
  first_names(7) := 'Diya'; first_names(8) := 'Priya'; first_names(9) := 'Rahul';
  first_names(10) := 'Neha'; first_names(11) := 'Rohan'; first_names(12) := 'Sneha';
  first_names(13) := 'Karan'; first_names(14) := 'Meera'; first_names(15) := 'Amit';
  first_names(16) := 'Pooja';

  last_names(1) := 'Sharma'; last_names(2) := 'Verma'; last_names(3) := 'Patel';
  last_names(4) := 'Reddy'; last_names(5) := 'Iyer'; last_names(6) := 'Nair';
  last_names(7) := 'Khan'; last_names(8) := 'Singh'; last_names(9) := 'Gupta';
  last_names(10) := 'Mehta'; last_names(11) := 'Joshi'; last_names(12) := 'Desai';

  occupations(1) := 'Software Engineer'; occupations(2) := 'Bank Officer';
  occupations(3) := 'Teacher'; occupations(4) := 'Shop Owner';
  occupations(5) := 'CA'; occupations(6) := 'Sales Executive';
  occupations(7) := 'Doctor'; occupations(8) := 'Government Clerk';

  regions(1) := 'Mumbai'; regions(2) := 'Pune'; regions(3) := 'Bengaluru';
  regions(4) := 'Hyderabad'; regions(5) := 'Chennai'; regions(6) := 'Delhi NCR';
  regions(7) := 'Kolkata'; regions(8) := 'Ahmedabad'; regions(9) := 'Jaipur';
  regions(10) := 'Lucknow'; regions(11) := 'Indore'; regions(12) := 'Nagpur';

  FOR i IN 1 .. 100 LOOP
    IF i = 1 THEN
      v_name := 'John D''Souza';
      v_age := 34;
      v_occ := 'Sales Executive';
      v_income := 'MID';
      v_region := 'Mumbai';
      v_pay := 'UPI';
      v_salary := 5;
    ELSE
      v_name := first_names(1 + MOD(i * 7, 16)) || ' ' || last_names(1 + MOD(i * 11, 12));
      v_age := 22 + MOD(i * 3, 40);
      v_occ := occupations(1 + MOD(i, 8));
      v_income := CASE MOD(i, 3) WHEN 0 THEN 'LOW' WHEN 1 THEN 'MID' ELSE 'HIGH' END;
      v_region := regions(1 + MOD(i * 5, 12));
      v_pm := MOD(i * 13, 100);
      v_pay := CASE WHEN v_pm < 65 THEN 'UPI' WHEN v_pm < 85 THEN 'NEFT' ELSE 'CARD' END;
      v_salary := CASE MOD(i, 7)
        WHEN 0 THEN 1 WHEN 1 THEN 5 WHEN 2 THEN 7 WHEN 3 THEN 10
        WHEN 4 THEN 15 WHEN 5 THEN 25 ELSE 28 END;
    END IF;

    INSERT INTO customers (name, age, occupation, income_segment, region, preferred_payment_method, salary_credit_day)
    VALUES (v_name, v_age, v_occ, v_income, v_region, v_pay, v_salary)
    RETURNING id INTO v_cust_id;

    IF i = 1 THEN
      v_ptype := 'MOTOR';
      v_premium := 2500;
      v_autopay := 0;
      v_due := TRUNC(SYSDATE);
    ELSE
      v_ptype := CASE MOD(i, 3) WHEN 0 THEN 'MOTOR' WHEN 1 THEN 'HEALTH' ELSE 'HOME' END;
      v_premium := 1500 + MOD(i * 917, 45000);
      v_autopay := CASE WHEN MOD(i, 3) = 0 THEN 1 ELSE 0 END;
      v_due := TRUNC(SYSDATE) + (MOD(i * 9, 40) - 15);
    END IF;

    v_missed := 0;
    v_claims := CASE WHEN MOD(i, 4) = 0 THEN 1 ELSE 0 END;
    v_risk := 25 + (v_missed * 18) + (v_claims * 8);
    IF v_autopay = 0 THEN v_risk := v_risk + 10; END IF;
    IF v_income = 'LOW' THEN v_risk := v_risk + 12; ELSIF v_income = 'HIGH' THEN v_risk := v_risk - 8; END IF;
    IF v_region IN ('Lucknow', 'Nagpur', 'Indore') THEN v_risk := v_risk + 6; END IF;
    v_risk := v_risk + MOD(i, 17);
    IF i = 1 THEN v_risk := 91; END IF;
    IF v_risk < 5 THEN v_risk := 5; ELSIF v_risk > 98 THEN v_risk := 98; END IF;

    v_status := CASE
      WHEN v_risk >= 70 THEN 'AT_RISK'
      WHEN v_due < TRUNC(SYSDATE) AND v_risk >= 60 THEN 'PAST_DUE'
      ELSE 'ACTIVE' END;

    INSERT INTO policies (policy_number, policy_type, premium, due_date, auto_pay, status, risk_score, customer_id)
    VALUES (
      CASE WHEN i = 1 THEN 'P1234' ELSE 'P' || LPAD(TO_CHAR(1000 + i), 4, '0') END,
      v_ptype, v_premium, v_due, v_autopay, v_status, v_risk, v_cust_id
    )
    RETURNING id INTO v_pol_id;

    -- Payment history (6 months), festive bias for Oct/Nov/Mar
    v_missed := 0;
    FOR m IN 1 .. 6 LOOP
      v_due_day := ADD_MONTHS(TRUNC(SYSDATE), -m);
      v_due_day := LEAST(v_due_day, LAST_DAY(v_due_day));
      BEGIN
        v_due_day := TO_DATE(
          TO_CHAR(v_due_day, 'YYYY-MM') || '-' || LPAD(TO_CHAR(LEAST(v_salary, 28)), 2, '0'),
          'YYYY-MM-DD');
      EXCEPTION WHEN OTHERS THEN
        v_due_day := ADD_MONTHS(TRUNC(SYSDATE), -m);
      END;

      v_festive := EXTRACT(MONTH FROM v_due_day) IN (3, 10, 11);
      IF i = 1 AND v_festive AND v_missed < 2 THEN
        v_is_missed := 1; v_is_late := 0; v_paid := NULL; v_missed := v_missed + 1;
      ELSIF v_festive AND MOD(i + m, 3) = 0 THEN
        v_is_missed := 1; v_is_late := 0; v_paid := NULL; v_missed := v_missed + 1;
      ELSIF MOD(i + m, 7) = 0 THEN
        v_is_missed := 0; v_is_late := 1; v_paid := v_due_day + 5;
      ELSE
        v_is_missed := 0; v_is_late := 0; v_paid := v_due_day - 1;
      END IF;

      INSERT INTO payment_history (policy_id, due_date, paid_date, amount, missed, late)
      VALUES (v_pol_id, v_due_day, v_paid, v_premium, v_is_missed, v_is_late);
    END LOOP;

    -- Recalculate risk with actual missed count
    v_risk := 25 + (v_missed * 18) + (v_claims * 8);
    IF v_autopay = 0 THEN v_risk := v_risk + 10; END IF;
    IF v_income = 'LOW' THEN v_risk := v_risk + 12; ELSIF v_income = 'HIGH' THEN v_risk := v_risk - 8; END IF;
    IF v_region IN ('Lucknow', 'Nagpur', 'Indore') THEN v_risk := v_risk + 6; END IF;
    v_risk := v_risk + MOD(i, 11);
    IF i = 1 THEN v_risk := 91; END IF;
    IF v_risk < 5 THEN v_risk := 5; ELSIF v_risk > 98 THEN v_risk := 98; END IF;
    v_status := CASE WHEN v_risk >= 70 THEN 'AT_RISK'
                     WHEN v_due < TRUNC(SYSDATE) AND v_risk >= 60 THEN 'PAST_DUE'
                     ELSE 'ACTIVE' END;
    UPDATE policies SET risk_score = v_risk, status = v_status WHERE id = v_pol_id;

    IF v_claims > 0 AND i <> 1 THEN
      FOR c IN 1 .. v_claims LOOP
        INSERT INTO claims (policy_id, amount, status, claim_date, description)
        VALUES (v_pol_id, 5000 + MOD(i * 401, 95000),
                CASE MOD(i + c, 3) WHEN 0 THEN 'OPEN' WHEN 1 THEN 'SETTLED' ELSE 'REJECTED' END,
                TRUNC(SYSDATE) - (30 + MOD(i * 17, 300)),
                v_ptype || ' claim #' || c);
      END LOOP;
    END IF;

    IF v_risk >= 70 THEN
      IF v_missed >= 2 AND v_risk >= 85 THEN
        v_rec := 'OFFER_INSTALLMENTS';
      ELSIF v_risk >= 80 THEN
        v_rec := 'AGENT_CALL';
      ELSIF v_autopay = 0 THEN
        v_rec := 'AUTOPAY_DISCOUNT';
      ELSE
        v_rec := 'WHATSAPP_REMINDER';
      END IF;
      IF i = 1 THEN v_rec := 'OFFER_INSTALLMENTS'; END IF;

      INSERT INTO ai_decisions (policy_id, recommendation, reasoning, predicted_success, status, created_at)
      VALUES (
        v_pol_id,
        v_rec,
        'Customer shows elevated delinquency risk (' || v_risk || '%). Missed payments=' || v_missed
          || ', AutoPay=' || CASE v_autopay WHEN 1 THEN 'ON' ELSE 'OFF' END
          || ', preferred method=' || v_pay || '. Recommended action: ' || v_rec || '.',
        GREATEST(55, 100 - v_risk + MOD(i, 10)),
        'PENDING',
        SYSTIMESTAMP - NUMTODSINTERVAL(MOD(i, 48), 'HOUR')
      );
    END IF;
  END LOOP;

  COMMIT;
  DBMS_OUTPUT.PUT_LINE('Oracle seeder created 100 India-localized policies');
END;
/
