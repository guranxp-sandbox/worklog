const indicator = document.getElementById('status-indicator');
const label = document.getElementById('status-label');
const today = document.getElementById('today');

today.textContent = new Date().toLocaleDateString('en-SE', {
  weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'
});

async function checkHealth() {
  try {
    const res = await fetch('/actuator/health');
    const data = await res.json();
    if (data.status === 'UP') {
      indicator.className = 'status-up';
      label.textContent = 'Backend: UP';
    } else {
      indicator.className = 'status-down';
      label.textContent = 'Backend: DOWN';
    }
  } catch {
    indicator.className = 'status-down';
    label.textContent = 'Backend: DOWN';
  }
}

checkHealth();
