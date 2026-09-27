# Crestwood Academy SMS — Build Prompt v3 (Real Data, No Simulation)

> Paste this whole document into your AI coding tool, alongside: (1) your existing HTML/CSS/JS frontend folder (design source of truth), and (2) the freshly-initialized Spring Boot project folder.

---

## 0. NON-NEGOTIABLE: NOTHING IS SIMULATED

Every previous version of this project's other builds (resume builder, study quiz engine, etc.) used simulated/hardcoded data with one real feature. **This build is different — do not simulate anything.** Specifically:

- The database connection is real (credentials provided in Section 1 below) — every entity must actually persist to and read from this real Postgres database via JPA/Hibernate. Do not hardcode in-memory lists, do not mock repository responses, do not fake data that looks like it came from a database.
- Fee payments must call the **real Paystack API** (test mode keys, provided by the user separately — see Section 6) — do not build a fake "payment successful" button that just flips a status flag without an actual Paystack transaction.
- Email notifications must actually send via a real SMTP connection (see Section 7) — do not just log "email sent" to the console without a real send attempt.
- The only "not real" data allowed in this entire build is the demo/seed dataset in Section 5, and that's seed data *inside the real database* — it still persists for real, gets read by real queries, and behaves identically to data a real user would create. It is not mocked; it's just pre-populated.

---

## 1. DATABASE CONNECTION — REAL, USE THIS EXACTLY

This is the actual Neon Postgres connection for this project. Paste this directly into `src/main/resources/application.properties` in the fresh Spring Boot backend folder:

```properties
spring.application.name=school-management-system

spring.datasource.url=jdbc:postgresql://ep-lucky-feather-b51oq4t4.c-7.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require
spring.datasource.username=neondb_owner
spring.datasource.password=npg_7CTrleqvYWc4

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

**Security note for whoever is building this:** this is a real, live database password. Do not commit `application.properties` to a public GitHub repository with this password still in it. Before pushing to GitHub, either move these three values into environment variables (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`) referenced from `application.properties`, or ensure the repo is private.

---

## 2. PROJECT OVERVIEW

Build **Crestwood Academy** — a white-label School Management System (SMS) web application, built as a Java Tech NIIT defense project but functioning as a real, working product.

**Stack:**
- Frontend: HTML, CSS, JavaScript — plain vanilla, matching the design already in the provided frontend folder
- Backend: Java, Spring Boot (Maven), Spring Web, Spring Data JPA, Spring Security, Validation
- Database: PostgreSQL on Neon (real connection above)
- Payments: Paystack — real API integration, test mode
- PDF/CSV export: jsPDF
- Email: real SMTP via Spring's JavaMailSender
- Package base: `com.crestwood.school_management_system`

**Five user roles**, each with their own login and dashboard:
1. **Super Admin** — adds/manages teachers, approves enrollment applications, oversees the whole system
2. **Teacher** — scoped only to their own assigned classes/subjects; confirms student rosters after admin approval, enters results, uploads study materials
3. **Student** — views their class, current semester, results per term, teachers, study materials
4. **Parent/Guardian** — views their child's (or children's) results, pays school fees, sees payment history
5. **Accountant** — manages fee invoices per class/term, payment ledger, outstanding fees dashboard, manually marks non-Paystack payments as paid, generates collection reports

---

## 3. CORE USER FLOW (build the backend logic to support exactly this sequence, against the real database)

1. A parent fills out a public **enrollment application** (no login required) — parent info + child info + class applying for. This creates a real row in `enrollment_applications`.
2. The application lands in the **Super Admin's** enrollment requests queue with status `PENDING`, read live from the database.
3. Super Admin reviews and either **approves** or **rejects** it via a real API call. On approval: a `Student` row and a `ParentGuardian` row are created for real, each linked to a new real `User` login record (with a real hashed password — generate a temporary password and include it in the approval email), and the student is assigned to the requested class — marked `rosterConfirmed = false`.
4. The relevant class **Teacher** sees this student in a real "students awaiting roster confirmation" query and confirms (or flags) them via a real API call. Only after confirmation does the student appear in the teacher's active class roster query.
5. Each term, the teacher enters results per subject through a real form submission: **CA1 (out of 20), CA2 (out of 20), Exam (out of 60), Total (auto-summed server-side), Grade (auto-derived from total server-side)**.
6. Students and parents view results via a real authenticated query the moment they're entered.
7. The **Accountant** creates a real fee invoice per class per term. Parents pay via a real Paystack transaction, or the accountant manually logs a real cash/bank transfer payment record against that invoice.
8. Real emails send on: a new result being posted, a fee becoming due, and an enrollment application being approved.
9. A **noticeboard** — real posts, real audience filtering by role/class, postable by Admin or Teacher via real API calls.

---

## 4. DATABASE SCHEMA (build these real entities/tables against the connection in Section 1)

- **users**: id, email (unique), password (hashed with BCrypt), role (enum: SUPER_ADMIN / TEACHER / STUDENT / PARENT / ACCOUNTANT), enabled (bool), created_at
- **teachers**: id, user_id (1:1 → users), full_name, phone
- **students**: id, user_id (1:1 → users), full_name, date_of_birth, gender, school_class_id (→ school_classes), roster_confirmed (bool, default false), admission_date
- **parent_guardians**: id, user_id (1:1 → users), full_name, phone
- **accountants**: id, user_id (1:1 → users), full_name, phone
- **student_parent_links**: id, parent_id (→ parent_guardians), student_id (→ students)
- **school_classes**: id, name (unique), class_teacher_id (→ teachers, nullable). Seed with exactly: **JSS 1, JSS 2, JSS 3, SS 1, SS 2, SS 3**.
- **subjects**: id, name (unique). Seed with exactly: **Mathematics, English Language, Basic Science, Biology, Chemistry, Physics, Civic Education, Social Studies, Agricultural Science, Computer Studies** — same list shared across all six classes.
- **teacher_subject_classes**: id, teacher_id, subject_id, school_class_id
- **academic_sessions**: id, name (unique), current (bool). Seed exactly one: "2025/2026", current = true.
- **terms**: id, academic_session_id, name (enum: FIRST_TERM / SECOND_TERM / THIRD_TERM), current (bool), start_date, end_date. Seed all three under 2025/2026; SECOND_TERM current = true.
- **enrollment_applications**: id, parent_full_name, parent_email, parent_phone, student_full_name, student_date_of_birth, applying_for_class_id (→ school_classes), status (enum: PENDING / APPROVED / REJECTED), submitted_at, reviewed_at, reviewed_by_user_id
- **results**: id, student_id, subject_id, term_id, ca1 (int), ca2 (int), exam (int), total (int), grade (string), entered_by_teacher_id, entered_at
- **study_materials**: id, teacher_id, subject_id, school_class_id, title, file_url, uploaded_at
- **fee_invoices**: id, school_class_id, term_id, amount (decimal), due_date, created_by_accountant_id, created_at
- **fee_payments**: id, student_id, fee_invoice_id, amount_paid (decimal), payment_method (enum: PAYSTACK / CASH / BANK_TRANSFER), paystack_reference (nullable), status (enum: PENDING / PAID), paid_at, recorded_by_accountant_id (nullable)
- **notifications**: id, recipient_user_id, type (enum: FEE_DUE / RESULT_POSTED / NOTICEBOARD / ENROLLMENT_APPROVED), title, message, is_read (bool), created_at
- **noticeboard_posts**: id, title, body, posted_by_user_id, audience (enum: ALL / STUDENTS / PARENTS / TEACHERS / SPECIFIC_CLASS), audience_class_id (nullable), posted_at
- **password_reset_tokens**: id, user_id (→ users), token (unique, random string), expires_at, used (bool, default false). Forgot-password flow: `POST /api/v1/auth/forgot-password` (body: email) generates a token with a 30-minute expiry, saves it here, and emails the user a reset link containing the token — do not reveal in the API response whether the email exists or not (always return a generic "if that email exists, a reset link was sent" message, to avoid leaking which emails are registered). `POST /api/v1/auth/reset-password` (body: token, new password) validates the token is unexpired and unused, updates the user's password, and marks the token used = true so it can't be reused.

Grade scale — exact, do not change: total ≥ 75 → A, 60–74 → B, 50–59 → C, 45–49 → D, 40–44 → E, below 40 → F.

---

## 5. DEMO / SEED DATA — real rows inserted into the real database on first run

- **Teachers:** Mrs. Adebayo Funmi (Mathematics, class teacher JSS 2) · Mr. Chukwu Emeka (English Language, class teacher SS 1) · Ms. Okoro Ifeoma (Biology & Chemistry, class teacher SS 2)
- **Students (JSS 2, roster_confirmed = true):** Tobi Adeyemi (male, DOB 14 Mar 2013) · Halima Suleiman (female, DOB 2 Jul 2013)
- **Parents:** Mrs. Folake Adeyemi (parent of Tobi) · Mr. Suleiman Bello (parent of Halima)
- **Accountant:** Mrs. Grace Okafor
- **Super Admin:** Anthony Adeyemo — `admin@crestwoodacademy.ng`
- **Results** (Mathematics, Second Term): Tobi — CA1 16, CA2 17, Exam 48 (total 81, grade A). Halima — CA1 14, CA2 15, Exam 40 (total 69, grade B)
- **Fee invoice:** JSS 2, Second Term, ₦185,000, due 30 Sept 2026
- **Fee payments:** Tobi — First Term paid in full (Paystack, status PAID). Second Term — unpaid (status PENDING)
- **One pending enrollment application:** parent "Mrs. Chidinma Okoro," student "Emeka Okoro," applying JSS 1, status PENDING

Use a Spring Boot `CommandLineRunner` or `data.sql` to insert this on startup, checking first that it hasn't already been inserted (don't duplicate rows on every restart).

---

## 6. PAYSTACK — REAL INTEGRATION

**The user will provide their own Paystack test keys** (get them free from https://dashboard.paystack.com/#/settings/developers — Test Secret Key and Test Public Key). Do not invent placeholder keys that look real; use obvious placeholder variable names until the real ones are pasted in:

```properties
paystack.secret.key=${PAYSTACK_SECRET_KEY}
paystack.public.key=${PAYSTACK_PUBLIC_KEY}
```

**Integration flow (build this for real, not simulated):**
1. Frontend: use Paystack's Inline JS (`https://js.paystack.co/v1/inline.js`) on the Parent dashboard's "Pay with Paystack" button. Pass the public key, amount (in kobo — multiply naira amount by 100), email, and a reference.
2. On successful frontend payment, Paystack returns a transaction reference.
3. Backend: `POST /api/v1/fee-payments/verify` receives that reference, calls Paystack's real verify endpoint (`GET https://api.paystack.co/transaction/verify/{reference}`) using the secret key in the `Authorization: Bearer` header, and only marks the `fee_payments` row as `PAID` if Paystack's response confirms `status: success`. **Never trust the frontend's claim of success alone** — always verify server-side against Paystack's API before updating payment status.
4. Store the real `paystack_reference` returned from this flow on the `fee_payments` row.

---

## 7. EMAIL — REAL SMTP, NOT LOGGED-ONLY

Use Spring's `spring-boot-starter-mail` (add to `pom.xml` if not already present) with a real SMTP provider. The user will provide real SMTP credentials for one of: Brevo (free tier ~300 emails/day), Resend, or Gmail SMTP (app-password based). Placeholder config:

```properties
spring.mail.host=${SMTP_HOST}
spring.mail.port=${SMTP_PORT}
spring.mail.username=${SMTP_USERNAME}
spring.mail.password=${SMTP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

Trigger real `JavaMailSender.send()` calls on: enrollment approval (send the new login credentials), a result being posted, and a fee invoice becoming due. Do not stub this out with a `System.out.println` placeholder — implement the actual `MailSender` bean and call it.

---

## 8. PAGES/SCREENS TO BUILD

Check the provided frontend folder first — build only what's missing, matching its existing design exactly:

1. Landing page
2. Sign Up (public enrollment application — submits to the real `/api/v1/enrollment-applications` endpoint)
3. Login (Student / Parent / Teacher / Accountant / Admin — one shared login page, real JWT auth against the real `users` table)
4. Super Admin dashboard — real stat counts queried from the database, real Teachers table with real Add Teacher action, a real Accountants table with a real Add Accountant action (accountants are added by Super Admin exactly the same way teachers are — there is no self-signup path for either role), real Enrollment Requests table with real approve/reject actions
5. Teacher dashboard — real roster confirmation actions, real results entry form that writes to the `results` table, real study materials upload
6. Student dashboard — real results query, real teacher list, real study materials list
7. Parent dashboard — real child switcher, real results query, real payment history, real Paystack payment flow per Section 6
8. Accountant dashboard — real invoice creation, real outstanding fees query, real payment ledger, real manual mark-as-paid action, real PDF/CSV export via jsPDF pulling actual data

For every dashboard, reuse the exact shell layout pattern already established in the provided frontend folder — all five should look like one consistent product.

---

## 9. REST API CONVENTIONS

- Base path: `/api/v1/`
- Plural noun resource paths matching table names: `/api/v1/students`, `/api/v1/teachers`, `/api/v1/enrollment-applications`, `/api/v1/fee-invoices`, `/api/v1/fee-payments`, `/api/v1/results`, `/api/v1/study-materials`, `/api/v1/noticeboard-posts`
- Standard verbs only: `GET`/`POST`/`PUT`/`DELETE`, plus `PATCH` for status changes (e.g. `PATCH /api/v1/enrollment-applications/{id}` with a `status` field) — no verb-in-URL endpoints
- `POST /api/v1/auth/login` returns a real JWT; every other endpoint requires `Authorization: Bearer <token>`, restricted by role via `@PreAuthorize`
- Public (no-auth) endpoints: only `POST /api/v1/enrollment-applications` and `POST /api/v1/auth/login`
- JSON error shape: `{ "error": "message" }` with correct HTTP status — never a raw stack trace to the frontend

---

## 10. BUILD ORDER

1. Wire up the real Neon connection (Section 1) and confirm the app starts and connects — check console for successful `HikariPool` startup, not a fallback/local connection.
2. Build all entities per Section 4, confirm real tables appear in Neon (check via Neon's SQL editor or table browser).
3. Insert seed data per Section 5, confirm real rows exist.
4. Build REST endpoints per Section 9, test each with real requests (Postman or similar) against the real database before touching frontend.
5. Wire Paystack per Section 6 using test keys — confirm a real test transaction actually verifies server-side.
6. Wire email per Section 7 — confirm a real test email actually arrives in an inbox.
7. Inspect the provided frontend folder, build any missing pages matching its design exactly, and connect every form/table to the real endpoints built above.

Do not mark anything "done" that hasn't been confirmed against the real database, a real Paystack test transaction, or a real received email — a UI that looks finished but isn't actually calling these real systems is not acceptable for this build.
