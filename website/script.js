/**
 * AAYA AI Assistant — Interactive Website Engine
 */

document.addEventListener('DOMContentLoaded', () => {
  initKineticTypography();
  initDownloadToasts();
});

/**
 * Kinetic Typography Scroll Observer
 * Scales and illuminates words as they enter the screen
 */
function initKineticTypography() {
  const growWords = document.querySelectorAll('.grow-word');
  if (!growWords.length) return;

  const observerOptions = {
    threshold: 0.6,
    rootMargin: '0px 0px -50px 0px'
  };

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('in-view');
      } else {
        entry.target.classList.remove('in-view');
      }
    });
  }, observerOptions);

  growWords.forEach(word => observer.observe(word));
}

/**
 * Interactive Simulator Controller
 * Simulates AAYA listening, processing and replying on the phone screen
 */
function simulateCommand(queryText, responseText) {
  const queryElem = document.getElementById('simulatorQuery');
  const responseElem = document.getElementById('simulatorResponse');
  const statusElem = document.getElementById('siriStatusText');
  const bars = document.querySelectorAll('.siri-waveform .bar');

  // Update active chip state
  const chips = document.querySelectorAll('.sim-chip');
  chips.forEach(c => c.classList.remove('active'));
  if (event && event.currentTarget) {
    event.currentTarget.classList.add('active');
  }

  // Phase 1: Listening animation
  statusElem.textContent = 'AAYA LISTENING...';
  statusElem.style.color = '#00f2fe';
  bars.forEach(b => {
    b.style.animationDuration = '0.4s';
    b.style.filter = 'drop-shadow(0 0 8px #00f2fe)';
  });

  queryElem.style.opacity = '0.5';
  queryElem.textContent = `"${queryText}"`;
  responseElem.textContent = 'Thinking...';
  responseElem.style.color = '#8e9bb5';

  // Phase 2: Execution & Response
  setTimeout(() => {
    statusElem.textContent = 'AAYA EXECUTED (LOCAL 80ms)';
    statusElem.style.color = '#00ff87';
    queryElem.style.opacity = '1';

    bars.forEach(b => {
      b.style.animationDuration = '1.4s';
      b.style.filter = 'none';
    });

    responseElem.textContent = responseText;
    responseElem.style.color = '#00ff87';
  }, 600);
}

/**
 * Floating Download Feedback Toast
 */
function initDownloadToasts() {
  const downloadBtns = document.querySelectorAll('a[download]');

  downloadBtns.forEach(btn => {
    btn.addEventListener('click', (e) => {
      showCyberToast('⚡ Initiating AAYA v3.3.0 Download! Open file after download to install.');
    });
  });
}

function showCyberToast(msg) {
  let existing = document.getElementById('cyberToast');
  if (existing) existing.remove();

  const toast = document.createElement('div');
  toast.id = 'cyberToast';
  toast.innerHTML = `
    <div class="toast-content">
      <span class="toast-dot"></span>
      <span class="toast-msg">${msg}</span>
    </div>
  `;

  // Inline styling for the self-contained toast
  Object.assign(toast.style, {
    position: 'fixed',
    bottom: '24px',
    right: '24px',
    zIndex: '9999',
    background: 'rgba(9, 13, 32, 0.95)',
    border: '1px solid #00f2fe',
    boxShadow: '0 0 25px rgba(0, 242, 254, 0.4)',
    borderRadius: '12px',
    padding: '14px 22px',
    color: '#fff',
    fontFamily: "'Space Grotesk', sans-serif",
    fontSize: '0.9rem',
    backdropFilter: 'blur(10px)',
    transform: 'translateY(100px)',
    opacity: '0',
    transition: 'all 0.35s cubic-bezier(0.16, 1, 0.3, 1)'
  });

  const content = toast.querySelector('.toast-content');
  Object.assign(content.style, {
    display: 'flex',
    alignItems: 'center',
    gap: '12px'
  });

  const dot = toast.querySelector('.toast-dot');
  Object.assign(dot.style, {
    width: '10px',
    height: '10px',
    borderRadius: '50%',
    background: '#00ff87',
    boxShadow: '0 0 8px #00ff87'
  });

  document.body.appendChild(toast);

  // Trigger entry animation
  requestAnimationFrame(() => {
    toast.style.transform = 'translateY(0)';
    toast.style.opacity = '1';
  });

  // Auto dismiss after 4 seconds
  setTimeout(() => {
    toast.style.transform = 'translateY(50px)';
    toast.style.opacity = '0';
    setTimeout(() => toast.remove(), 400);
  }, 4000);
}
