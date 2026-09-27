# Crestwood Academy SMS — Frontend Build Prompt (Google AI Studio, Frontend Only)

> Paste this into Google AI Studio's Build mode, and upload your existing HTML/CSS/JS files alongside it. This is a frontend-only prompt — do not attempt to write any backend, database, or server-side logic. This app will later be connected to a separately-built Java Spring Boot backend; for now, every screen displays realistic sample content so the UI can be reviewed and approved before real data is wired in.

---

## 0. WHAT NOT TO TOUCH

**Leave the landing page exactly as it already is.** Do not redesign, restructure, or "improve" it. It is finished. Every instruction below applies only to what comes after login.

---

## 1. WHAT TO DO: EXTRACT THE EXISTING DESIGN SYSTEM FIRST

Before building anything new, inspect the provided HTML/CSS/JS files and extract the actual design system already in use: exact color values, font families/weights, border-radius values, button styles, card styles, shadow styles, spacing scale, and any breakpoints already defined for mobile. Every new screen must use these same values — do not introduce new colors, fonts, or component shapes that aren't already present in the provided files. This should look like one person built the whole site, not like the dashboards were designed separately from the landing page.

---

## 2. WHAT TO BUILD

Two auth screens, then five role dashboards, all reusing the same design system and the same shared dashboard shell (sidebar + topbar + content area) described in Section 3.

### Sign Up (public enrollment application)
Centered card, cream background, matching the landing page's card/button style. Fields: Parent/Guardian full name; Email and Phone (side by side); Child's full name; Date of birth and Applying for class — a dropdown populated with exactly these six options in this order: JSS 1, JSS 2, JSS 3, SS 1, SS 2, SS 3. A note box above the fields explaining the application is reviewed by admin before an account is created. Submit button, and a footer link to the Login screen.

### Login
Centered card, same style as Sign Up. A 5-tab role selector in exactly this order: **Student / Parent / Teacher / Accountant / Admin**. Email/ID and Password fields. Submit button. Footer links for forgot password and "apply now."

### Forgot Password / Reset Password (two small screens, same card style as Login)
- **Forgot Password:** centered card, headline "Reset your password," one field (Email), submit button labeled "Send reset link," and a note below it: "If that email exists in our system, you'll receive a reset link shortly." A link back to Login.
- **Reset Password:** centered card, headline "Choose a new password," two fields (New Password, Confirm Password), submit button labeled "Update password." This screen is what the emailed reset link points to (it reads a token from the URL — the frontend doesn't need to display or validate the token itself, just capture it from the URL and send it along with the new password when the form submits).

---

## 3. SHARED DASHBOARD SHELL — USED IDENTICALLY BY ALL 5 ROLES

**Sidebar (left, fixed):**
- Contains: brand/logo at top, a small role-tag pill showing which role is logged in (e.g. "TEACHER," "ACCOUNTANT"), then a vertical list of nav items for that role.
- **Must be animated**, specifically:
  - On desktop: the sidebar supports a collapsed/expanded toggle. Expanded state shows icon + full text label for each nav item. Collapsed state shrinks to icon-only, narrower width. Toggling between states animates smoothly (width transition, ~0.25–0.3s ease, with nav item labels fading out/in rather than abruptly disappearing).
  - Each nav item animates on hover: background fades in smoothly (~0.2s), and the active/current page's nav item has a persistent highlighted state (solid background in the brand's accent color) that doesn't require hover to show.
  - On mobile: the sidebar becomes a slide-in drawer triggered by a hamburger icon in the topbar. The drawer slides in from the left (or right — match whichever direction the landing page's existing mobile nav already uses, for consistency) with a dark semi-transparent overlay behind it, smooth easing (~0.3–0.4s), and closes when the overlay is tapped or a nav item is selected.
- Every nav item icon should use a consistent icon style (flat line icons, not emoji, not mismatched styles between items).

**Topbar (top, full width of content area):**
- Page title on the left, subtitle beneath it (e.g. current term/session context)
- On mobile: hamburger icon appears here to open the sidebar drawer
- Avatar/initials circle on the right

**Content area:**
- Below the topbar: on Overview/Dashboard pages only, a row of stat cards (see Section 4 for exact grid rules); on every other page, panel sections start immediately below the topbar with no stat cards unless Section 4 explicitly says that page has them.
- Stat card grid arrangement: **exactly 4 stat cards in a single horizontal row on desktop (≥1024px width)**, equal width, equal height, 16–20px gap between them. On tablet (768–1023px): 2 columns × 2 rows. On mobile (<768px): 1 column, stacked, full width. Never wrap unevenly (e.g. 3-then-1) — always a clean grid at every breakpoint.
- Panel sections (tables/lists) stack vertically below the stat card row, one panel per horizontal band, full width of the content area, 20–24px vertical gap between panels. Each panel: header row (title left-aligned, primary action button right-aligned, both vertically centered) at the top, content (table or list) directly below with no extra empty space, consistent internal padding on all four sides.
- Every table must scroll horizontally within its own card on narrow/mobile widths (`overflow-x: auto` on the table wrapper) — the page itself must never scroll sideways.
- Cards and panels should have a subtle hover-lift effect where they're interactive (e.g. a clickable table row highlights on hover; a stat card does not need a hover effect since it's not clickable).

**STRICT RULE — one sidebar nav item = one page, no content overlap between pages:**
Every sidebar nav item listed in Section 4 is its own separate page/view. A page shows **only** the content explicitly assigned to it in Section 4 below — never the full content of a different nav item. The only exception: an Overview/Dashboard/My Classes page (each role's first/home nav item) may show small **preview widgets** — a short 2–3 row snippet of another page's data with a "View All →" link that navigates to that page — but it must never embed that other page's full interactive table, its filters, or its action buttons. If a user wants to actually manage fees, they must click into the Fees page — the Overview page never lets them do fee actions directly. This applies identically across all 5 roles: Overview stays Overview, Fees stays Fees, Results stays Results, Teachers stays Teachers — nothing bleeds across pages.

Build this shell as one shared, reusable component/template — all five dashboards use the exact same shell structure, only the sidebar nav items, stat cards, and panel content differ per role.

---

## 4. PAGE-BY-PAGE CONTENT — ONE PAGE PER NAV ITEM, EXACT ARRANGEMENT

### SUPER ADMIN (sidebar: Overview / Teachers / Accountants / Students / Enrollment Requests / Fees & Payments / Settings)

**Overview page** — the home page after login. Contains ONLY:
- Stat card row (4, per the grid rule above): Total Students (842), Total Teachers (37), Pending Enrollments (14), Fees Collected This Term (₦18.4M)
- One preview panel: "Recent Enrollment Requests" — shows only the 3 most recent, columns (Applicant, Class, Submitted), each row clickable, header row has a "View All →" link to the Enrollment Requests page. No approve/reject buttons here — those only exist on the full Enrollment Requests page.
- One preview panel: "Latest Announcements" — 3 most recent noticeboard posts (title + date only), "View All →" link.
- Nothing else on this page. No teacher list, no accountant list, no fee table.

**Teachers page** — contains ONLY teacher management:
- Page header: "Teachers" (left) + "Add Teacher" button (right)
- One full-width panel: complete teachers table — columns: Name, Subject(s), Class(es), Status, Edit action. Sample rows: Mrs. Adebayo Funmi (Mathematics, JSS 2), Mr. Chukwu Emeka (English Language, SS 1), Ms. Okoro Ifeoma (Biology & Chemistry, SS 2)
- No stat cards on this page. No accountant, student, or fee content.

**Accountants page** — contains ONLY accountant management:
- Page header: "Accountants" + "Add Accountant" button
- One full-width panel: accountants table — columns: Name, Status, Edit action. Sample row: Mrs. Grace Okafor
- This is the only place accountant accounts are created — no self-signup exists for this role, same as Teacher.

**Students page** — contains ONLY the school-wide student directory (distinct from Teacher's class-scoped roster):
- Page header: "Students" + a Class filter dropdown (All Classes / JSS 1 / JSS 2 / JSS 3 / SS 1 / SS 2 / SS 3) top-right
- One full-width panel: students table — columns: Name, Class, Roster Confirmed (yes/no badge), Admission Date, View action
- No edit-results or fee actions here — this page is read/search only for admin oversight.

**Enrollment Requests page** — contains ONLY the enrollment application queue:
- Page header: "Enrollment Requests" + filter tabs directly beneath it: Pending / Approved / Rejected / All (Pending selected by default)
- One full-width panel: applications table — columns: Applicant, Applying For (class), Submitted Date, Status, Review action. Review action opens a detail panel/modal showing full parent+child info with Approve/Reject buttons.

**Fees & Payments page** — admin's oversight-only view (the actual working tools — creating invoices, marking payments — live only on the Accountant's dashboard, never here):
- Page header: "Fees & Payments"
- Stat card row (4): Total Collected This Term, Total Outstanding, Invoices Created This Term, Overdue Count
- One full-width panel: a read-only summary table — columns: Class, Term, Amount Invoiced, Amount Collected, Outstanding. No edit or "mark as paid" actions on this table — it's oversight only.

**Settings page** — contains ONLY school configuration:
- Page header: "Settings"
- One panel: School Profile — fields for School Name, Logo upload, primary brand color (matching the config.js white-label pattern)
- One panel: Academic Sessions & Terms — list of sessions/terms with create/edit actions, and a way to mark which term is "current"
- Nothing else — no user management here (that's Teachers/Accountants/Students pages).

---

### TEACHER (sidebar: My Classes / Confirm Roster / Results Entry / Study Materials)

**My Classes page** — the home page. Contains ONLY:
- Stat card row (4): My Students (96), Awaiting Roster Confirmation (3), Materials Uploaded (21), Results Pending Entry (12)
- One panel: a card grid (not a table) — one card per assigned class/subject combination, e.g. "JSS 2 — Mathematics" showing student count. Each card is clickable and navigates to Confirm Roster or Results Entry pre-filtered to that class — but this page itself never shows the roster or results table directly.

**Confirm Roster page** — contains ONLY roster confirmation:
- Page header: "Confirm Roster" + Class filter dropdown (if teacher has more than one class)
- One full-width panel: table — columns: Student Name, Class, Confirm action, Flag action

**Results Entry page** — contains ONLY results entry:
- Page header: "Results Entry" + three filter dropdowns in a row beneath it: Class, Subject, Term
- One full-width panel: editable table — columns: Student, CA1 (editable number input), CA2 (editable number input), Exam (editable number input), Total (read-only, auto-calculated live as CA1/CA2/Exam are typed), Grade (read-only, auto-derived live from Total). "Save All" button in the panel header, right-aligned. Sample rows: Tobi Adeyemi (16/17/48 → 81/A), Halima Suleiman (14/15/40 → 69/B)

**Study Materials page** — contains ONLY materials management:
- Page header: "Study Materials" + Class and Subject filter dropdowns + "Upload Material" button, all in the header row
- One full-width panel: list of uploaded materials — each row: title, subject, class, upload date, delete action

---

### STUDENT (sidebar: Dashboard / Results / Study Materials)

**Dashboard page** — the home page. Contains ONLY:
- One panel: profile card — name, class, age, birthday, current term (Tobi Adeyemi, JSS 2, age 13, birthday 14 Mar, Second Term)
- One panel: "My Teachers" — small list, class teacher + subject teachers, name and subject only, no other content
- One preview panel: "Latest Result" — shows only the single most recent subject's grade as a small highlight (e.g. "Mathematics — Grade A"), with a "View Full Results →" link to the Results page. The full results table never appears here.

**Results page** — contains ONLY results:
- Page header: "Results" + Term filter dropdown (First / Second / Third Term)
- One full-width panel: full results table — columns: Subject, CA1, CA2, Exam, Total, Grade — every subject for the selected term

**Study Materials page** — contains ONLY materials:
- Page header: "Study Materials" + Subject filter dropdown
- One full-width panel: list of materials, each row: title, subject, upload date, download action

---

### PARENT (sidebar: Dashboard / Fees / Results)

**Dashboard page** — the home page. Contains ONLY:
- Child switcher at the top if the parent has more than one child linked (tabs or a dropdown)
- One compact panel: fee-due summary — amount due, due date only, with a "Go to Fees →" button that navigates to the Fees page. **No "Pay" button here and no payment form** — payment only happens on the Fees page itself.
- One compact preview panel: "Latest Result" — same single-subject highlight pattern as Student's dashboard, with "View Full Results →" link. No full table here.
- One preview panel: "Latest Announcements" — 2–3 recent noticeboard posts, "View All →" link.

**Fees page** — contains ONLY fee content, and this is where payment actually happens:
- Page header: "Fees"
- One panel: full fee-due card — amount, due date, and the actual "Pay with Paystack" button here (UI placeholder for now, per Section 2's note on backend wiring happening later)
- One full-width panel below it: full payment history table — columns: Term, Amount, Date, Status. Sample rows: First Term ₦185,000 Paid, Second Term ₦185,000 Due

**Results page** — contains ONLY results:
- Page header: "Results" + Term filter dropdown
- One full-width panel: full results table, read-only, same column structure as Student's Results page

---

### ACCOUNTANT (sidebar: Overview / Fee Invoices / Outstanding Fees / Payment Ledger / Reports)

**Overview page** — the home page. Contains ONLY:
- Stat card row (4): Total Outstanding, Collected This Term, Invoices Created, Overdue Count
- One preview panel: "Recent Payments" — 3 most recent rows only (Student, Amount, Date), "View All →" link to Payment Ledger. No mark-as-paid action here.

**Fee Invoices page** — contains ONLY invoice management:
- Page header: "Fee Invoices" + "Create Invoice" button
- One full-width panel: invoices table — columns: Class, Term, Amount, Due Date, Created Date, Edit action

**Outstanding Fees page** — contains ONLY the defaulters view:
- Page header: "Outstanding Fees"
- One full-width panel: table — columns: Student, Class, Amount Owed, Days Overdue, "Send Reminder" action (this triggers the fee-due email notification — it's the only action on this page, no mark-as-paid here)

**Payment Ledger page** — contains ONLY the payment record log, and this is the only page with a mark-as-paid action:
- Page header: "Payment Ledger"
- One full-width panel: table — columns: Student, Term/Invoice, Amount Paid, Method (Paystack/Cash/Bank Transfer badge), Date, Status, "Mark as Paid" action (only enabled/visible for rows where method is Cash or Bank Transfer and status is still Pending — Paystack rows are marked automatically and this action doesn't apply to them)

**Reports page** — contains ONLY export tools:
- Page header: "Reports"
- One panel: report filters — Class dropdown, Term dropdown, date range — followed by two buttons: "Export Results Report (PDF)" and "Export Collection Report (PDF/CSV)" (both UI placeholders for now, real export logic comes later)
- No tables on this page — it's purely the export controls.

---

## 5. MOBILE BEHAVIOR (apply to every dashboard, not just one)

- Sidebar → slide-in drawer per Section 3
- Stat card rows → stack to a single column, full width
- Every table → horizontally scrollable within its own card, never the page itself
- Topbar stays fixed/sticky with the hamburger icon always accessible

---

## 6. WHAT "DONE" LOOKS LIKE FOR THIS PASS

By the end of this pass: Sign Up and Login screens exist and match the landing page's visual style; all five roles have their full set of separate pages built exactly per Section 4 — every sidebar nav item is its own distinct page, no page shows another page's full table or actions, Overview/Dashboard pages only show small preview widgets with "View All" links; every screen works correctly at both desktop and mobile widths using the exact grid/stacking rules in Sections 3 and 5; the landing page is untouched. Before calling this done, check each role's Overview/home page specifically — it should be short and scannable, not a dumping ground for every other page's content. No backend calls, no real authentication, no real Paystack integration — this is the frontend shell that the real Spring Boot backend will be wired into afterward.
