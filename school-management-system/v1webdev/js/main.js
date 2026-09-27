/**
 * Crestwood Academy - Pure Vanilla JavaScript (v1webdev)
 * Exact 1:1 text and functional parity with React source components
 * No frameworks, no libraries, plain modern ES6+
 */

document.addEventListener('DOMContentLoaded', () => {
  // ========================================================================
  // 1. Data Store (Exact texts from React source)
  // ========================================================================
  const NEWS_STORIES = [
    {
      id: 'waec-results-2026',
      date: 'OCTOBER 14, 2026',
      title: 'Crestwood Students Excel in 2026 WAEC Results',
      category: 'Academics',
      excerpt: 'Crestwood Academy celebrates outstanding 2026 WAEC results, with 98% of students securing distinctions across Mathematics, English, Sciences, and Humanities.',
      fullContent: `Crestwood Academy has once again recorded exceptional performance in the West African Senior School Certificate Examination (WASSCE / WAEC) for the 2025/2026 academic session.

Highlights of the 2026 WAEC Results:
• 98% of graduating students recorded credit distinctions (A1–B3) in Mathematics and English Language
• 100% pass rate across STEM, Commercial, and Humanities subject clusters
• Over 45 students scored distinctions in all 9 registered subjects
• Outstanding individual scores in Further Mathematics, Physics, Chemistry, Economics, and Literature-in-English

"These results reflect the relentless dedication of our qualified teachers, the diligence of our students, and the continuous support of our parent community," noted the Crestwood Academic Board.`,
      imageUrl: 'https://images.unsplash.com/photo-1523240795612-9a054b0db644?q=80&w=800&auto=format&fit=crop',
      author: 'Crestwood Academic Board',
      readTime: '3 min read'
    },
    {
      id: 'science-competition-2026',
      date: 'SEPTEMBER 28, 2026',
      title: 'Crestwood Academic Team Places 3rd in State Science Competition',
      category: 'Academics',
      excerpt: 'Demonstrating exceptional analytical mastery in physics, chemistry, and biology, Crestwood Academy students earned top podium honors at the state level.',
      fullContent: `Competing against top secondary schools statewide, the Crestwood Academy STEM delegation earned a well-deserved 3rd place overall in the prestigious State Science Olympiad & Practical Quiz Competition.

The four-student Crestwood team excelled across challenging theoretical assessments and practical laboratory experiments in organic chemistry, Newtonian mechanics, and biological taxonomy.

"Our laboratory equipment and dedicated subject teachers gave our students the confidence to excel under rigorous examination," said the Head of the Science Department.`,
      imageUrl: 'https://images.unsplash.com/photo-1577495508048-b635879837f1?q=80&w=800&auto=format&fit=crop',
      author: 'Science Department',
      readTime: '4 min read'
    },
    {
      id: 'computer-lab-2026',
      date: 'SEPTEMBER 05, 2026',
      title: 'New Computer Laboratory Officially Opens at Crestwood Academy',
      category: 'Campus Facilities',
      excerpt: 'Equipped with modern high-speed workstations, fiber internet, and coding resources, the new computer laboratory expands digital learning for all classes.',
      fullContent: `Crestwood Academy has officially commissioned its state-of-the-art Science and Computer Laboratory suite.

The newly opened facility features:
• 60 individual high-speed desktop workstations configured for programming and digital design
• Dedicated fiber-optic high-speed internet connectivity
• Interactive digital smartboards for collaborative STEM instruction
• Full integration with the Crestwood online student learning portal

The computer laboratory enables all junior and senior secondary students to acquire foundational software literacy, data management skills, and computer-based examination readiness.`,
      imageUrl: 'https://images.unsplash.com/photo-1584132967334-10e028bd69f7?q=80&w=800&auto=format&fit=crop',
      author: 'Campus Facilities',
      readTime: '3 min read'
    }
  ];

  const PATHWAYS = [
    {
      id: 'academics',
      category: 'Academic Excellence',
      title: 'WAEC & JAMB Preparation',
      desc: 'A curriculum aligned with the Nigerian secondary school syllabus, preparing students for WAEC, NECO, and JAMB with clear, trackable results.',
      buttonText: 'VIEW CURRICULUM',
      modalType: 'prospectus'
    },
    {
      id: 'activities',
      category: 'Sports & Extracurriculars',
      title: 'Inter-House Sports & Clubs',
      desc: 'Balanced development through inter-house sports, clubs, and co-curricular activities that build character alongside academics.',
      buttonText: 'EXPLORE ACTIVITIES',
      modalType: 'prospectus'
    },
    {
      id: 'portal',
      category: 'Digital School Portal',
      title: 'Student, Parent & Teacher Access',
      desc: 'A secure online portal where students check results, parents track progress and pay school fees, and teachers manage grading — all in one place.',
      buttonText: 'ACCESS PORTAL',
      modalType: 'portal'
    }
  ];

  const TESTIMONIALS = [
    {
      id: 'student',
      badge: 'STUDENT',
      tabLabel: 'STUDENT',
      quote: 'Crestwood Academy gave me a strong academic foundation and the confidence to gain admission into my dream university while graduating at the top of my class.',
      author: 'Chidinma Okafor',
      role: 'Class of 2024 / Admitted to University of Lagos'
    },
    {
      id: 'parent',
      badge: 'PARENT PERSPECTIVE',
      tabLabel: 'PARENT',
      quote: "We chose Crestwood Academy for the quality of teaching, but we stay because of the caring community and how easy it is to track our child's progress online. Our daughter has grown immensely in both confidence and academic performance.",
      author: 'The Okonkwo Family',
      role: 'Parents of SS2 Student'
    }
  ];

  // ========================================================================
  // 2. Navigation & Mobile Drawer
  // ========================================================================
  const mobileToggle = document.getElementById('mobileToggle');
  const mobileDrawer = document.getElementById('mobileDrawer');
  const mobileBackdrop = document.getElementById('mobileBackdrop');

  function toggleMobileMenu(forceClose = false) {
    if (!mobileDrawer || !mobileToggle || !mobileBackdrop) return;
    const shouldOpen = forceClose ? false : !mobileDrawer.classList.contains('open');
    if (shouldOpen) {
      mobileToggle.classList.add('active');
      mobileDrawer.classList.add('open');
      mobileBackdrop.classList.add('open');
      document.body.style.overflow = 'hidden';
    } else {
      mobileToggle.classList.remove('active');
      mobileDrawer.classList.remove('open');
      mobileBackdrop.classList.remove('open');
      document.body.style.overflow = '';
    }
  }

  if (mobileToggle) {
    mobileToggle.addEventListener('click', () => toggleMobileMenu());
  }

  if (mobileBackdrop) {
    mobileBackdrop.addEventListener('click', () => toggleMobileMenu(true));
  }

  // Smooth scroll links and auto-close drawer
  document.querySelectorAll('[data-scroll-to]').forEach((link) => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const targetId = link.getAttribute('data-scroll-to');
      const targetEl = document.getElementById(targetId);
      if (targetEl) {
        toggleMobileMenu(true);
        const headerOffset = 80;
        const elPosition = targetEl.getBoundingClientRect().top;
        const offsetPosition = elPosition + window.pageYOffset - headerOffset;
        window.scrollTo({
          top: offsetPosition,
          behavior: 'smooth'
        });
      }
    });
  });

  // Desktop Portal Dropdown
  const desktopPortalWrapper = document.getElementById('desktopPortalWrapper');
  const desktopPortalBtn = document.getElementById('desktopPortalBtn');

  if (desktopPortalBtn && desktopPortalWrapper) {
    desktopPortalBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      desktopPortalWrapper.classList.toggle('open');
    });

    document.addEventListener('click', (e) => {
      if (!desktopPortalWrapper.contains(e.target)) {
        desktopPortalWrapper.classList.remove('open');
      }
    });
  }

  // Mobile Portal Dropdown Section
  const mobilePortalSection = document.getElementById('mobilePortalSection');
  const mobilePortalBtn = document.getElementById('mobilePortalBtn');

  if (mobilePortalBtn && mobilePortalSection) {
    mobilePortalBtn.addEventListener('click', () => {
      mobilePortalSection.classList.toggle('expanded');
    });
  }

  // ========================================================================
  // 3. Modals Manager
  // ========================================================================
  const modalContainer = document.getElementById('modalContainer');

  function closeModal() {
    if (!modalContainer) return;
    modalContainer.innerHTML = '';
    modalContainer.classList.add('hidden');
    document.body.style.overflow = '';
  }

  window.closeModal = closeModal;

  // Global ESC key listener
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      closeModal();
      toggleMobileMenu(true);
    }
  });

  function openModal(type, data = {}) {
    if (!modalContainer) return;
    toggleMobileMenu(true);
    modalContainer.classList.remove('hidden');
    document.body.style.overflow = 'hidden';

    let contentHtml = '';

    switch (type) {
      case 'tour':
        contentHtml = renderTourModal();
        break;
      case 'apply':
        contentHtml = renderApplyModal();
        break;
      case 'video':
        contentHtml = renderVideoModal();
        break;
      case 'prospectus':
        contentHtml = renderProspectusModal();
        break;
      case 'portal':
        contentHtml = renderPortalModal(data.role || 'portal.user@crestwood.edu.ng');
        break;
      case 'story':
        contentHtml = renderStoryModal(data);
        break;
      default:
        contentHtml = renderProspectusModal();
    }

    modalContainer.innerHTML = `
      <div class="modal-overlay" id="modalOverlay">
        <div class="modal-dialog" id="modalDialog" onclick="event.stopPropagation()">
          <button class="modal-close-btn" id="modalCloseBtn" aria-label="Close modal">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
          <div id="modalBody">
            ${contentHtml}
          </div>
        </div>
      </div>
    `;

    // Bind overlay click & close button
    const overlay = document.getElementById('modalOverlay');
    if (overlay) {
      overlay.addEventListener('click', (e) => {
        if (e.target === overlay) closeModal();
      });
    }
    const closeBtn = document.getElementById('modalCloseBtn');
    if (closeBtn) {
      closeBtn.addEventListener('click', closeModal);
    }

    // Attach dynamic listeners for modal forms
    attachModalEvents(type, data);
  }

  window.openModal = openModal;

  // Bind all data-open-modal trigger buttons on page
  function bindModalTriggers() {
    document.querySelectorAll('[data-open-modal]').forEach((btn) => {
      btn.onclick = (e) => {
        e.preventDefault();
        const modalType = btn.getAttribute('data-open-modal');
        const modalData = btn.getAttribute('data-modal-payload');
        let payload = {};
        if (modalData) {
          try {
            payload = JSON.parse(modalData);
          } catch {
            payload = { role: modalData };
          }
        }
        openModal(modalType, payload);
      };
    });
  }

  bindModalTriggers();

  // --- Modal Templates ---

  // 1. Tour Modal
  function renderTourModal() {
    return `
      <div class="modal-content-pad">
        <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem;">
          <span style="background-color: #EB4E27; color: #FFF; font-size: 10px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.05em; padding: 2px 10px; border-radius: 9999px;">
            ADMISSIONS VISIT
          </span>
        </div>
        <h3 style="font-weight: 900; font-size: 1.75rem; text-transform: uppercase; letter-spacing: -0.025em; color: var(--color-gray-950); line-height: 1.1;">
          SCHEDULE A CAMPUS TOUR
        </h3>
        <p style="color: var(--color-gray-600); font-size: 0.875rem; margin-top: 0.25rem;">
          Experience our modern classrooms, laboratories, library, and sports grounds in person.
        </p>

        <form id="tourForm" style="margin-top: 1.5rem;" class="form-group">
          <div class="form-grid-2">
            <div>
              <label class="form-label">Student-Athlete Name</label>
              <input required type="text" placeholder="e.g. Jordan Miller" class="form-input" />
            </div>
            <div>
              <label class="form-label">Graduation Year / Class</label>
              <select class="form-select">
                <option>Class of 2026 (Senior)</option>
                <option>Class of 2027 (Junior)</option>
                <option>Class of 2028 (Sophomore)</option>
                <option>Class of 2029 (Freshman / Middle)</option>
              </select>
            </div>
          </div>

          <div class="form-grid-2">
            <div>
              <label class="form-label">Preferred Tour Date</label>
              <input required type="date" value="2026-10-15" class="form-input" id="tourDateInput" />
            </div>
            <div>
              <label class="form-label">Time Slot</label>
              <select class="form-select" id="tourTimeInput">
                <option>10:00 AM (Morning Training &amp; Classrooms)</option>
                <option>01:30 PM (Afternoon Matchplay &amp; Biometrics)</option>
                <option>04:00 PM (Sunset Hardcourt &amp; Residence Tour)</option>
              </select>
            </div>
          </div>

          <div>
            <label class="form-label">Parent / Guardian Email</label>
            <input required type="email" placeholder="parent@example.com" class="form-input" />
          </div>

          <div style="padding-top: 0.5rem;">
            <button type="submit" class="btn-primary" style="width: 100%; justify-content: center; background-color: var(--color-dark-indigo); color: #FFF; padding: 0.875rem 1rem;">
              CONFIRM CAMPUS VISIT RESERVATION
            </button>
          </div>
        </form>
      </div>
    `;
  }

  // 2. Application Modal (Multi-step)
  let currentApplyStep = 1;
  function renderApplyModal(step = 1) {
    currentApplyStep = step;
    let stepTitle = 'Step 1: Student Profile';
    if (step === 2) stepTitle = 'Step 2: Academic Interests';
    if (step === 3) stepTitle = 'Step 3: Guardian Authorization';

    return `
      <div class="modal-content-pad">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.5rem; padding-bottom: 1rem; border-bottom: 1px solid var(--color-gray-200);">
          <div style="display: flex; align-items: center; gap: 0.5rem;">
            <span style="width: 1.5rem; height: 1.5rem; border-radius: 9999px; background: var(--color-dark-indigo); color: #FFF; font-size: 0.75rem; font-weight: 700; display: flex; align-items: center; justify-content: center;">
              ${step}
            </span>
            <span style="font-weight: 700; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em; color: var(--color-gray-900);">
              ${stepTitle}
            </span>
          </div>
          <span style="font-size: 0.75rem; font-weight: 600; color: var(--color-gray-500);">
            Step ${step} of 3
          </span>
        </div>

        <h3 style="font-weight: 900; font-size: 1.75rem; text-transform: uppercase; letter-spacing: -0.025em; color: var(--color-gray-950); line-height: 1.1;">
          2025/2026 ADMISSIONS APPLICATION
        </h3>
        <p style="color: var(--color-gray-600); font-size: 0.875rem; margin-top: 0.25rem;">
          Join the ranks of Nigeria's next generation of leaders.
        </p>

        <div id="applyStepContainer">
          ${renderApplyStepContent(step)}
        </div>
      </div>
    `;
  }

  function renderApplyStepContent(step) {
    if (step === 1) {
      return `
        <div style="margin-top: 1.5rem;" class="form-group">
          <div class="form-grid-2">
            <div>
              <label class="form-label">First Name</label>
              <input required value="Elena" class="form-input" id="applyFirstName" />
            </div>
            <div>
              <label class="form-label">Last Name</label>
              <input required value="Reyes" class="form-input" id="applyLastName" />
            </div>
          </div>
          <div>
            <label class="form-label">Target Grade &amp; Term</label>
            <select class="form-select">
              <option>Grade 9 (Class of 2030) - Full Boarding</option>
              <option>Grade 10 (Class of 2029) - Full Boarding</option>
              <option>Grade 11 (Class of 2028) - Full Boarding</option>
              <option>Grade 12 (Class of 2027) - College Transition Track</option>
            </select>
          </div>
          <div>
            <label class="form-label">Current School GPA</label>
            <input required value="3.92 (Unweighted)" class="form-input" />
          </div>
          <button type="button" class="btn-primary" style="width: 100%; justify-content: center; background-color: var(--color-dark-indigo); color: #FFF; padding: 0.875rem 1rem;" id="applyNext1">
            <span>Proceed to Athletics Details</span>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-lime)" stroke-width="2.5"><polyline points="9 18 15 12 9 6"></polyline></svg>
          </button>
        </div>
      `;
    } else if (step === 2) {
      return `
        <div style="margin-top: 1.5rem;" class="form-group">
          <div class="form-grid-2">
            <div>
              <label class="form-label">Current UTR Rating</label>
              <input value="10.85" class="form-input" />
            </div>
            <div>
              <label class="form-label">National / ITF Ranking</label>
              <input value="Top 40 National U16" class="form-input" />
            </div>
          </div>
          <div>
            <label class="form-label">Dominant Playing Hand &amp; Style</label>
            <select class="form-select">
              <option>Right-Handed / Aggressive Baseliner</option>
              <option>Left-Handed / All-Court Player</option>
              <option>Serve &amp; Volley Specialist</option>
              <option>Counter-Puncher</option>
            </select>
          </div>
          <div>
            <label class="form-label">Primary Academic Honors Interests</label>
            <input value="AP Calculus BC, Honors Physics, Sports Biometrics" class="form-input" />
          </div>
          <div style="display: flex; gap: 0.75rem;">
            <button type="button" class="btn-secondary" style="width: 33%; padding: 0.875rem;" id="applyBack1">Back</button>
            <button type="button" class="btn-primary" style="width: 67%; justify-content: center; background-color: var(--color-dark-indigo); color: #FFF; padding: 0.875rem;" id="applyNext2">
              <span>Final Step: Parent Info</span>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-lime)" stroke-width="2.5"><polyline points="9 18 15 12 9 6"></polyline></svg>
            </button>
          </div>
        </div>
      `;
    } else {
      return `
        <div style="margin-top: 1.5rem;" class="form-group">
          <div>
            <label class="form-label">Parent / Guardian Full Name</label>
            <input value="Carlos &amp; Sofia Reyes" class="form-input" />
          </div>
          <div>
            <label class="form-label">Contact Email</label>
            <input type="email" value="reyes.family@example.com" class="form-input" />
          </div>
          <div>
            <label class="form-label">Phone Number</label>
            <input type="tel" value="+1 (555) 349-2810" class="form-input" />
          </div>
          <div style="display: flex; gap: 0.75rem; padding-top: 0.5rem;">
            <button type="button" class="btn-secondary" style="width: 33%; padding: 0.875rem;" id="applyBack2">Back</button>
            <button type="button" class="btn-orange" style="width: 67%; justify-content: center; padding: 0.875rem;" id="applySubmit">
              SUBMIT OFFICIAL APPLICATION
            </button>
          </div>
        </div>
      `;
    }
  }

  // 3. Video Modal
  function renderVideoModal() {
    return `
      <div class="modal-content-pad">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.75rem;">
          <span style="background-color: var(--color-blue); color: #FFF; font-size: 10px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.05em; padding: 2px 10px; border-radius: 9999px;">
            CAMPUS SHOWCASE
          </span>
        </div>
        <h3 style="font-weight: 900; font-size: 1.75rem; text-transform: uppercase; letter-spacing: -0.025em; color: var(--color-gray-950); line-height: 1.1;">
          CRESTWOOD ACADEMY CAMPUS TOUR
        </h3>
        <p style="color: var(--color-gray-600); font-size: 0.875rem; margin-top: 0.25rem; margin-bottom: 1.25rem;">
          Tour modern classrooms, the science and computer laboratories, the school library, and sports grounds.
        </p>

        <div style="position: relative; border-radius: 1rem; border: 2px solid #000; overflow: hidden; aspect-ratio: 16/9; background-color: #0A0A0A;" id="videoScreen">
          <img
            src="https://images.unsplash.com/photo-1560012057-4372e14c5085?q=80&w=1200&auto=format&fit=crop"
            alt="Crestwood Academy Campus"
            id="videoPosterImg"
            style="width: 100%; height: 100%; object-fit: cover; opacity: 0.6; transition: opacity 0.3s;"
          />
          <div style="position: absolute; top: 1rem; left: 1rem; z-index: 10; background: rgba(0,0,0,0.8); padding: 0.375rem 0.75rem; border-radius: 0.5rem; border: 1px solid var(--color-gray-700); color: #FFF; font-size: 0.75rem; font-family: monospace;">
            4K UHD • 60 FPS • Crestwood Academy Campus
          </div>

          <div id="videoControlsOverlay" style="position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #FFF; cursor: pointer;">
            <div style="width: 4rem; height: 4rem; border-radius: 9999px; background: var(--color-lime); border: 2px solid #000; display: flex; align-items: center; justify-content: center; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.3);">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="#000" stroke="#000" style="margin-left: 4px;"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
            </div>
            <span style="font-weight: 900; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 0.05em; margin-top: 0.75rem; background: rgba(0,0,0,0.75); padding: 0.25rem 0.75rem; border-radius: 0.25rem;">
              Start Campus Flythrough
            </span>
          </div>

          <div id="videoLiveStreamBar" class="hidden" style="position: absolute; bottom: 1rem; left: 1rem; right: 1rem; z-index: 20; background: rgba(0,0,0,0.8); backdrop-filter: blur(4px); padding: 0.75rem; border-radius: 0.75rem; border: 1px solid var(--color-gray-700); display: flex; align-items: center; justify-content: space-between; color: #FFF; font-size: 0.75rem;">
            <div style="display: flex; align-items: center; gap: 0.5rem;">
              <span style="width: 10px; height: 10px; border-radius: 9999px; background: #EB4E27;"></span>
              <span style="font-weight: 700;">Streaming: Main Academic Quadrangle &amp; Labs</span>
            </div>
            <button id="videoPauseBtn" style="background: none; border: none; text-decoration: underline; color: var(--color-gray-300); cursor: pointer;">
              Pause
            </button>
          </div>
        </div>

        <div style="margin-top: 1.25rem; display: grid; grid-template-columns: repeat(3, 1fr); gap: 0.75rem; text-align: center;">
          <div style="border: 1px solid var(--color-gray-200); border-radius: 0.5rem; padding: 0.625rem; background: #F9FAFB;">
            <div style="font-weight: 900; font-size: 0.75rem; color: var(--color-gray-900); text-transform: uppercase;">Part 1</div>
            <div style="font-size: 11px; color: var(--color-gray-500);">Modern Classroom Blocks</div>
          </div>
          <div style="border: 1px solid var(--color-gray-200); border-radius: 0.5rem; padding: 0.625rem; background: #F9FAFB;">
            <div style="font-weight: 900; font-size: 0.75rem; color: var(--color-gray-900); text-transform: uppercase;">Part 2</div>
            <div style="font-size: 11px; color: var(--color-gray-500);">Science &amp; ICT Laboratories</div>
          </div>
          <div style="border: 1px solid var(--color-gray-200); border-radius: 0.5rem; padding: 0.625rem; background: #F9FAFB;">
            <div style="font-weight: 900; font-size: 0.75rem; color: var(--color-gray-900); text-transform: uppercase;">Part 3</div>
            <div style="font-size: 11px; color: var(--color-gray-500);">Sports &amp; Library Facilities</div>
          </div>
        </div>
      </div>
    `;
  }

  // 4. Prospectus Modal
  function renderProspectusModal() {
    return `
      <div class="modal-content-pad">
        <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem;">
          <span style="background-color: var(--color-lime); color: #000; font-size: 10px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.05em; padding: 2px 10px; border-radius: 9999px; border: 1px solid #000;">
            OFFICIAL PROSPECTUS
          </span>
        </div>
        <h3 style="font-weight: 900; font-size: 1.75rem; text-transform: uppercase; letter-spacing: -0.025em; color: var(--color-gray-950); line-height: 1.1;">
          2025/2026 ACADEMIC PROSPECTUS
        </h3>
        <p style="color: var(--color-gray-600); font-size: 0.875rem; margin-top: 0.25rem;">
          Comprehensive curriculum guide, WAEC preparation timeline, faculty profiles, and school calendar.
        </p>

        <div style="margin-top: 1.5rem; border: 2px solid #000; border-radius: 1rem; padding: 1.25rem; background-color: #F9FAFB;">
          <div style="display: flex; align-items: center; justify-content: space-between; font-size: 0.75rem; font-weight: 700; color: var(--color-gray-800); padding-bottom: 0.5rem; border-bottom: 1px solid var(--color-gray-200);">
            <span>Guide Contents</span>
            <span style="color: var(--color-blue);">PDF • 14.8 MB</span>
          </div>
          <ul style="font-size: 0.75rem; color: var(--color-gray-600); margin-top: 0.75rem; display: flex; flex-direction: column; gap: 0.5rem; padding-left: 1.25rem; list-style-type: disc;">
            <li>Full academic curriculum outline &amp; SSCE subject directories</li>
            <li>STEM enrichment, science practicals, and coding curriculum</li>
            <li>Leadership, debate, clubs, and sports programs</li>
            <li>Admissions procedure, registration requirements, and fee structure</li>
          </ul>
        </div>

        <div style="margin-top: 1.5rem; display: flex; flex-direction: column; gap: 0.75rem;">
          <button id="downloadProspectusBtn" class="btn-primary" style="width: 100%; justify-content: center; background-color: var(--color-dark-indigo); color: #FFF; padding: 0.875rem 1rem;">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--color-lime)" stroke-width="2.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
            <span id="downloadBtnText">DOWNLOAD OFFICIAL PROSPECTUS</span>
          </button>
          <button onclick="closeModal()" class="btn-secondary" style="width: 100%; justify-content: center; padding: 0.875rem 1rem;">
            Close
          </button>
        </div>
      </div>
    `;
  }

  // 5. Portal Login Modal
  function renderPortalModal(defaultId = 'portal.user@crestwood.edu.ng') {
    return `
      <div class="modal-content-pad">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.5rem;">
          <span style="background-color: var(--color-dark-indigo); color: #FFF; font-size: 10px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.05em; padding: 2px 10px; border-radius: 9999px;">
            SECURE SCHOOL PORTAL
          </span>
        </div>
        <h3 style="font-weight: 900; font-size: 1.75rem; text-transform: uppercase; letter-spacing: -0.025em; color: var(--color-gray-950); line-height: 1.1;">
          CRESTWOOD PORTAL LOGIN
        </h3>
        <p style="color: var(--color-gray-600); font-size: 0.875rem; margin-top: 0.25rem; margin-bottom: 1.5rem;">
          Access real-time term reports, examination grades, fee receipts, and school bulletins.
        </p>

        <form id="portalLoginForm" class="form-group">
          <div>
            <label class="form-label">Student / Parent / Staff ID or Email</label>
            <input required type="text" value="${defaultId.includes('@') ? defaultId : defaultId.toLowerCase().replace(/[^a-z0-9]/g, '.') + '@crestwood.edu.ng'}" class="form-input" id="portalIdInput" />
          </div>

          <div>
            <label class="form-label">Password</label>
            <input required type="password" value="••••••••••••" class="form-input" />
          </div>

          <div style="display: flex; align-items: center; justify-content: space-between; font-size: 0.75rem;">
            <label style="display: flex; align-items: center; gap: 0.5rem; cursor: pointer; color: var(--color-gray-700);">
              <input type="checkbox" checked style="accent-color: var(--color-blue);" />
              <span>Remember device</span>
            </label>
            <a href="#forgot" onclick="event.preventDefault()" style="color: var(--color-blue); font-weight: 700; text-decoration: underline;">
              Forgot password?
            </a>
          </div>

          <div style="padding-top: 0.5rem;">
            <button type="submit" class="btn-primary" style="width: 100%; justify-content: center; background-color: var(--color-dark-indigo); color: #FFF; padding: 0.875rem 1rem;">
              SIGN IN TO MY PORTAL
            </button>
          </div>
        </form>
      </div>
    `;
  }

  // 6. Story Modal
  function renderStoryModal(story) {
    if (!story) return '';
    return `
      <div>
        <div style="position: relative; aspect-ratio: 16/9; background: #171717; border-bottom: 2px solid #000;">
          <img
            src="${story.imageUrl}"
            alt="${story.title}"
            style="width: 100%; height: 100%; object-fit: cover;"
          />
          <div style="position: absolute; inset: 0; background: linear-gradient(to top, rgba(0,0,0,0.8) 0%, rgba(0,0,0,0.2) 60%, transparent 100%);"></div>
          <div style="position: absolute; bottom: 1rem; left: 1.5rem; right: 1.5rem; color: #FFF;">
            <span style="background: #EB4E27; color: #FFF; font-size: 10px; font-weight: 900; text-transform: uppercase; letter-spacing: 0.05em; padding: 2px 10px; border-radius: 9999px; display: inline-block; margin-bottom: 0.5rem;">
              ${story.date}
            </span>
            <h3 style="font-weight: 900; font-size: 1.25rem; text-transform: uppercase; letter-spacing: -0.025em; line-height: 1.25;">
              ${story.title}
            </h3>
          </div>
        </div>

        <div class="modal-content-pad" style="max-height: 50vh; overflow-y: auto;">
          <div style="display: flex; align-items: center; gap: 1rem; font-size: 0.75rem; color: var(--color-gray-500); font-weight: 600; padding-bottom: 1rem; margin-bottom: 1rem; border-bottom: 1px solid var(--color-gray-200);">
            <span>By ${story.author || 'Crestwood Academic Board'}</span>
            <span>•</span>
            <span>${story.readTime || '3 min read'}</span>
            <span>•</span>
            <span style="color: var(--color-blue); font-weight: 700;">${story.category}</span>
          </div>

          <div style="color: var(--color-gray-700); font-size: 0.875rem; line-height: 1.6; white-space: pre-line;">
            ${story.fullContent}
          </div>

          <div style="margin-top: 2rem; padding-top: 1.5rem; border-top: 1px solid var(--color-gray-200); display: flex; justify-content: flex-end;">
            <button onclick="closeModal()" class="btn-secondary" style="padding: 0.625rem 1.5rem; font-weight: 700; font-size: 0.75rem; text-transform: uppercase;">
              Close Story
            </button>
          </div>
        </div>
      </div>
    `;
  }

  // --- Attach Modal Event Listeners ---
  function attachModalEvents(type, data) {
    const modalBody = document.getElementById('modalBody');
    if (!modalBody) return;

    if (type === 'tour') {
      const tourForm = document.getElementById('tourForm');
      if (tourForm) {
        tourForm.addEventListener('submit', (e) => {
          e.preventDefault();
          const date = document.getElementById('tourDateInput').value || '2026-10-15';
          const time = document.getElementById('tourTimeInput').value || '10:00 AM';
          modalBody.innerHTML = `
            <div style="padding: 2.5rem 2rem; text-align: center;">
              <div style="width: 4rem; height: 4rem; border-radius: 9999px; background: var(--color-lime); border: 2px solid #000; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.25rem;">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#000" stroke-width="2.5"><polyline points="20 6 9 17 4 12"></polyline></svg>
              </div>
              <h3 style="font-weight: 900; font-size: 1.75rem; text-transform: uppercase; letter-spacing: -0.025em; color: var(--color-gray-950);">
                CAMPUS TOUR CONFIRMED
              </h3>
              <p style="color: var(--color-gray-600); font-size: 0.875rem; margin: 0.75rem auto 0; max-width: 28rem; line-height: 1.5;">
                We look forward to hosting you at Crestwood Academy on <span style="font-weight: 700; color: var(--color-gray-950);">${date} at ${time}</span>. Our admissions coordinator has sent your customized itinerary to your email.
              </p>
              <button onclick="closeModal()" class="btn-primary" style="margin-top: 2rem; background: var(--color-dark-indigo); color: #FFF; padding: 0.75rem 2rem;">
                Return to Overview
              </button>
            </div>
          `;
        });
      }
    } else if (type === 'apply') {
      function bindApplyStepControls() {
        const next1 = document.getElementById('applyNext1');
        const next2 = document.getElementById('applyNext2');
        const back1 = document.getElementById('applyBack1');
        const back2 = document.getElementById('applyBack2');
        const submit = document.getElementById('applySubmit');

        if (next1) {
          next1.onclick = () => {
            modalBody.innerHTML = renderApplyModal(2);
            bindApplyStepControls();
          };
        }
        if (back1) {
          back1.onclick = () => {
            modalBody.innerHTML = renderApplyModal(1);
            bindApplyStepControls();
          };
        }
        if (next2) {
          next2.onclick = () => {
            modalBody.innerHTML = renderApplyModal(3);
            bindApplyStepControls();
          };
        }
        if (back2) {
          back2.onclick = () => {
            modalBody.innerHTML = renderApplyModal(2);
            bindApplyStepControls();
          };
        }
        if (submit) {
          submit.onclick = () => {
            const refNum = Math.floor(1000 + Math.random() * 9000);
            modalBody.innerHTML = `
              <div style="padding: 2.5rem 2rem; text-align: center;">
                <div style="width: 4rem; height: 4rem; border-radius: 9999px; background: var(--color-lime); border: 2px solid #000; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.25rem;">
                  <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#000" stroke-width="2"><path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"></path><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"></path><path d="M4 22h16"></path><path d="M10 14.66V17c0 .55-.45 1-1 1H7c-.55 0-1-.45-1-1v-2.34"></path><path d="M18 14.66V17c0 .55-.45 1-1 1h-2c-.55 0-1-.45-1-1v-2.34"></path><path d="M8 2h8a2 2 0 0 1 2 2v7a6 6 0 0 1-12 0V4a2 2 0 0 1 2-2z"></path></svg>
                </div>
                <h3 style="font-weight: 900; font-size: 1.75rem; text-transform: uppercase; letter-spacing: -0.025em; color: var(--color-gray-950);">
                  APPLICATION RECEIVED!
                </h3>
                <p style="color: var(--color-gray-600); font-size: 0.875rem; margin: 0.75rem auto 0; max-width: 28rem; line-height: 1.5;">
                  Application Reference: <span style="font-family: monospace; font-weight: 700; color: var(--color-gray-950);">#CWA-2025-${refNum}</span>.
                  Our Admissions Committee reviews candidates on a rolling basis. You will receive an interview invitation within 48 hours.
                </p>
                <button onclick="closeModal()" class="btn-primary" style="margin-top: 2rem; background: var(--color-dark-indigo); color: #FFF; padding: 0.75rem 2rem;">
                  Close &amp; Return Home
                </button>
              </div>
            `;
          };
        }
      }
      bindApplyStepControls();
    } else if (type === 'video') {
      const videoScreen = document.getElementById('videoScreen');
      const videoControls = document.getElementById('videoControlsOverlay');
      const videoBar = document.getElementById('videoLiveStreamBar');
      const poster = document.getElementById('videoPosterImg');
      const pauseBtn = document.getElementById('videoPauseBtn');

      if (videoControls) {
        videoControls.onclick = () => {
          videoControls.style.display = 'none';
          if (videoBar) videoBar.classList.remove('hidden');
          if (poster) poster.style.opacity = '0.85';
        };
      }
      if (pauseBtn) {
        pauseBtn.onclick = () => {
          if (videoControls) videoControls.style.display = 'flex';
          if (videoBar) videoBar.classList.add('hidden');
          if (poster) poster.style.opacity = '0.6';
        };
      }
    } else if (type === 'prospectus') {
      const dlBtn = document.getElementById('downloadProspectusBtn');
      const dlText = document.getElementById('downloadBtnText');
      if (dlBtn && dlText) {
        dlBtn.onclick = () => {
          dlText.textContent = 'PROSPECTUS DOWNLOADED (PDF)';
          dlBtn.style.backgroundColor = '#16a34a';
        };
      }
    } else if (type === 'portal') {
      const form = document.getElementById('portalLoginForm');
      if (form) {
        form.addEventListener('submit', (e) => {
          e.preventDefault();
          modalBody.innerHTML = `
            <div style="padding: 2.5rem 2rem; text-align: center;">
              <div style="width: 4rem; height: 4rem; border-radius: 9999px; background: var(--color-lime); border: 2px solid #000; display: flex; align-items: center; justify-content: center; margin: 0 auto 1.25rem;">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#000" stroke-width="2.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path></svg>
              </div>
              <h3 style="font-weight: 900; font-size: 1.75rem; text-transform: uppercase; letter-spacing: -0.025em; color: var(--color-gray-950);">
                WELCOME TO CRESTWOOD PORTAL
              </h3>
              <p style="color: var(--color-gray-600); font-size: 0.875rem; margin: 0.75rem auto 0; max-width: 28rem; line-height: 1.5;">
                Logged into Crestwood Academy Academic &amp; School Portal. Redirecting to your dashboard...
              </p>
              <button onclick="closeModal()" class="btn-primary" style="margin-top: 2rem; background: var(--color-dark-indigo); color: #FFF; padding: 0.75rem 2rem;">
                Enter Dashboard
              </button>
            </div>
          `;
        });
      }
    }
  }

  // Helper to open story by id directly
  window.openStoryById = function(storyId) {
    const story = NEWS_STORIES.find((s) => s.id === storyId) || NEWS_STORIES[0];
    openModal('story', story);
  };

  // ========================================================================
  // 4. Pathways Interactive Mobile Switcher
  // ========================================================================
  let activePathwayIndex = 0;
  const pathwayCardMobile = document.getElementById('pathwayCardMobile');
  const pathwayTabBtns = document.querySelectorAll('.pathway-tab-btn');
  const pathwayDotBtns = document.querySelectorAll('.pathway-dot-btn');

  function renderMobilePathway(index) {
    activePathwayIndex = index;
    const item = PATHWAYS[index];
    if (!pathwayCardMobile) return;

    const isDark = index === 1;
    pathwayCardMobile.className = `pathway-card ${isDark ? 'dark' : 'light'}`;
    pathwayCardMobile.innerHTML = `
      <div>
        <span class="cat-label" style="${isDark ? 'color: var(--color-lime);' : ''}">${item.category}</span>
        <h3 class="card-title">${item.title}</h3>
        <p class="card-body">${item.desc}</p>
      </div>
      <div class="card-btn" data-open-modal="${item.modalType}">
        <span>${item.buttonText}</span>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <line x1="5" y1="12" x2="19" y2="12"></line>
          <polyline points="12 5 19 12 12 19"></polyline>
        </svg>
      </div>
    `;

    pathwayTabBtns.forEach((btn, i) => {
      btn.classList.toggle('active', i === index);
    });
    pathwayDotBtns.forEach((dot, i) => {
      dot.classList.toggle('active', i === index);
    });

    bindModalTriggers();
  }

  pathwayTabBtns.forEach((btn) => {
    btn.addEventListener('click', () => {
      const idx = parseInt(btn.getAttribute('data-index'), 10);
      renderMobilePathway(idx);
    });
  });

  pathwayDotBtns.forEach((dot) => {
    dot.addEventListener('click', () => {
      const idx = parseInt(dot.getAttribute('data-index'), 10);
      renderMobilePathway(idx);
    });
  });

  renderMobilePathway(0);

  // ========================================================================
  // 5. Testimonials Mobile Switcher
  // ========================================================================
  let activeTestIndex = 0;
  const testimonialCardMobile = document.getElementById('testimonialCardMobile');
  const testTabBtns = document.querySelectorAll('.test-tab-btn');
  const testDotBtns = document.querySelectorAll('.test-dot-btn');

  function renderMobileTestimonial(index) {
    activeTestIndex = index;
    const t = TESTIMONIALS[index];
    if (!testimonialCardMobile) return;

    testimonialCardMobile.innerHTML = `
      <div>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span class="test-badge">${t.badge}</span>
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#EB4E27" stroke-width="2.2" stroke-linecap="round">
            <path d="M4 7H18M4 12H15M4 17H20"></path>
            <circle cx="19" cy="7" r="1.5" fill="#EB4E27"></circle>
            <circle cx="16" cy="12" r="1.5" fill="#EB4E27"></circle>
          </svg>
        </div>
        <blockquote class="test-quote" style="margin-top: 1rem; font-size: 0.9375rem;">
          &ldquo;${t.quote}&rdquo;
        </blockquote>
      </div>
      <div class="test-author-info">
        <div class="test-author-name">${t.author}</div>
        <div class="test-author-role">${t.role}</div>
      </div>
    `;

    testTabBtns.forEach((btn, i) => {
      btn.classList.toggle('active', i === index);
    });
    testDotBtns.forEach((dot, i) => {
      dot.classList.toggle('active', i === index);
    });
  }

  testTabBtns.forEach((btn) => {
    btn.addEventListener('click', () => {
      const idx = parseInt(btn.getAttribute('data-index'), 10);
      renderMobileTestimonial(idx);
    });
  });

  testDotBtns.forEach((dot) => {
    dot.addEventListener('click', () => {
      const idx = parseInt(dot.getAttribute('data-index'), 10);
      renderMobileTestimonial(idx);
    });
  });

  renderMobileTestimonial(0);

  // ========================================================================
  // 6. School News Mobile Carousel
  // ========================================================================
  let activeNewsIndex = 0;
  const newsCardMobile = document.getElementById('newsCardMobile');
  const newsIndexText = document.getElementById('newsIndexText');
  const newsPrevBtn = document.getElementById('newsPrevBtn');
  const newsNextBtn = document.getElementById('newsNextBtn');
  const newsDotBtns = document.querySelectorAll('.news-dot-btn');

  function renderMobileNews(index) {
    activeNewsIndex = index;
    const story = NEWS_STORIES[index];
    if (!newsCardMobile) return;

    if (newsIndexText) {
      newsIndexText.textContent = `Story ${index + 1} of ${NEWS_STORIES.length}`;
    }

    newsCardMobile.innerHTML = `
      <div class="news-card" onclick="openStoryById('${story.id}')" style="cursor: pointer;">
        <div class="news-card-img">
          <img src="${story.imageUrl}" alt="${story.title}" />
          <span class="news-card-tag">${story.category}</span>
        </div>
        <div class="news-card-body">
          <span class="news-date">${story.date}</span>
          <h3 class="news-title">${story.title}</h3>
          <p style="color: var(--color-gray-600); font-size: 0.75rem; margin-top: 0.5rem; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;">
            ${story.excerpt}
          </p>
        </div>
        <div class="news-card-footer">
          <div class="news-read-more">
            <span>Read Full Story</span>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <line x1="5" y1="12" x2="19" y2="12"></line>
              <polyline points="12 5 19 12 12 19"></polyline>
            </svg>
          </div>
        </div>
      </div>
    `;

    newsDotBtns.forEach((dot, i) => {
      dot.classList.toggle('active', i === index);
    });
  }

  if (newsPrevBtn) {
    newsPrevBtn.addEventListener('click', () => {
      const nextIdx = activeNewsIndex === 0 ? NEWS_STORIES.length - 1 : activeNewsIndex - 1;
      renderMobileNews(nextIdx);
    });
  }

  if (newsNextBtn) {
    newsNextBtn.addEventListener('click', () => {
      const nextIdx = activeNewsIndex === NEWS_STORIES.length - 1 ? 0 : activeNewsIndex + 1;
      renderMobileNews(nextIdx);
    });
  }

  newsDotBtns.forEach((dot) => {
    dot.addEventListener('click', () => {
      const idx = parseInt(dot.getAttribute('data-index'), 10);
      renderMobileNews(idx);
    });
  });

  renderMobileNews(0);
});
