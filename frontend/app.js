// State tracking
let doctorsList = [];
let selectedDoctorId = null;
let selectedDate = null;
let currentActiveSlotTime = null;

document.addEventListener("DOMContentLoaded", () => {
  initializeDateInput();
  setupTabNavigation();
  setupEventListeners();
  loadDoctors();
});

// Set default date to today in local YYYY-MM-DD
function initializeDateInput() {
  const dateInput = document.getElementById("dateInput");
  const today = new Date().toISOString().split("T")[0];
  dateInput.value = today;
  dateInput.min = today;
}

// Tab switcher
function setupTabNavigation() {
  const tabBook = document.getElementById("tabBook");
  const tabMyAppointments = document.getElementById("tabMyAppointments");
  const bookSection = document.getElementById("bookSection");
  const myAppointmentsSection = document.getElementById("myAppointmentsSection");

  tabBook.addEventListener("click", () => {
    tabBook.classList.add("active");
    tabMyAppointments.classList.remove("active");
    bookSection.style.display = "block";
    myAppointmentsSection.style.display = "none";
  });

  tabMyAppointments.addEventListener("click", () => {
    tabMyAppointments.classList.add("active");
    tabBook.classList.remove("active");
    bookSection.style.display = "none";
    myAppointmentsSection.style.display = "block";
  });
}

function setupEventListeners() {
  document.getElementById("btnLoadSlots").addEventListener("click", fetchSlots);
  document.getElementById("btnLookupAppointments").addEventListener("click", fetchPatientAppointments);
}

// Hide wake-up notice on first successful response
function hideConnectionNotice() {
  const notice = document.getElementById("connectionNotice");
  if (notice) {
    notice.classList.add("hidden");
  }
}

// 1. Load Doctors for dropdown
async function loadDoctors() {
  const select = document.getElementById("doctorSelect");
  try {
    const res = await fetch(`${CONFIG.API_BASE_URL}/doctors`);
    if (!res.ok) throw new Error("Failed to load doctor directory");
    doctorsList = await res.json();
    hideConnectionNotice();

    if (doctorsList.length === 0) {
      select.innerHTML = '<option value="">No doctors found (seed database first)</option>';
      return;
    }

    select.innerHTML = doctorsList.map(doc => 
      `<option value="${doc.id}">${doc.name} (${doc.specialization} - ${doc.consultationMinutes} min)</option>`
    ).join("");

    selectedDoctorId = doctorsList[0].id;
    // Auto-load slots for first doctor
    fetchSlots();
  } catch (err) {
    select.innerHTML = '<option value="">Error connecting to server</option>';
    showBookingAlert("Unable to connect to MediSlot API. Is the backend running?", "error");
  }
}

// 2. Fetch Available Slots for chosen Doctor & Date
async function fetchSlots() {
  const doctorSelect = document.getElementById("doctorSelect");
  const dateInput = document.getElementById("dateInput");
  const slotList = document.getElementById("slotRowsList");
  const countLabel = document.getElementById("slotCountLabel");

  selectedDoctorId = doctorSelect.value;
  selectedDate = dateInput.value;

  if (!selectedDoctorId || !selectedDate) {
    showBookingAlert("Please select a doctor and date.", "error");
    return;
  }

  clearBookingAlert();
  slotList.innerHTML = '<li class="empty-state">Loading day sheet slots...</li>';

  try {
    const res = await fetch(`${CONFIG.API_BASE_URL}/doctors/${selectedDoctorId}/slots?date=${selectedDate}`);
    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.message || "Failed to retrieve slots");
    }

    hideConnectionNotice();
    const slots = await res.json();
    renderDaySheet(slots);
  } catch (err) {
    slotList.innerHTML = `<li class="empty-state">Error: ${err.message}</li>`;
    countLabel.textContent = "";
  }
}

// Render slots as ledger rows
function renderDaySheet(slots) {
  const slotList = document.getElementById("slotRowsList");
  const countLabel = document.getElementById("slotCountLabel");

  if (!slots || slots.length === 0) {
    slotList.innerHTML = '<li class="empty-state">No open slots on this date (doctor may not work today or all slots are taken).</li>';
    countLabel.textContent = "0 open slots";
    return;
  }

  countLabel.textContent = `${slots.length} available`;
  slotList.innerHTML = slots.map(slot => {
    // slot.start is ISO string "2026-11-02T09:00:00"
    const startTimeFormatted = slot.start.split("T")[1].substring(0, 5);
    const endTimeFormatted = slot.end.split("T")[1].substring(0, 5);

    return `
      <li class="slot-row" id="row-${escapeHtml(slot.start)}">
        <span class="slot-time">${startTimeFormatted}</span>
        <span class="slot-status">Available (${startTimeFormatted} - ${endTimeFormatted})</span>
        <div class="slot-action">
          <button type="button" onclick="openBookingForm('${escapeHtml(slot.start)}')">Book slot</button>
        </div>
      </li>
    `;
  }).join("");
}

// Expand inline patient booking input
function openBookingForm(slotStartIso) {
  currentActiveSlotTime = slotStartIso;
  const existingForm = document.getElementById("inlineBookingBox");
  if (existingForm) existingForm.remove();

  const targetRow = document.getElementById(`row-${slotStartIso}`);
  if (!targetRow) return;

  const formBox = document.createElement("div");
  formBox.id = "inlineBookingBox";
  formBox.className = "inline-booking-box";
  formBox.innerHTML = `
    <div class="inline-form">
      <div class="form-group">
        <label for="inputPatientId">Patient ID for booking:</label>
        <input type="number" id="inputPatientId" class="form-control" placeholder="e.g. 1 (Alice Smith)" value="1">
      </div>
      <button type="button" class="action-btn" onclick="submitBooking('${escapeHtml(slotStartIso)}')">Confirm booking</button>
      <button type="button" class="cancel-btn" onclick="cancelInlineForm()">Cancel</button>
    </div>
  `;

  targetRow.after(formBox);
}

function cancelInlineForm() {
  const box = document.getElementById("inlineBookingBox");
  if (box) box.remove();
}

// 3. Submit Booking (Handles 409 Conflict deliberately)
async function submitBooking(slotStartIso) {
  const patientIdInput = document.getElementById("inputPatientId");
  const patientId = patientIdInput.value;

  if (!patientId) {
    showBookingAlert("Please enter a valid patient ID.", "error");
    return;
  }

  const payload = {
    doctorId: parseInt(selectedDoctorId, 10),
    patientId: parseInt(patientId, 10),
    slotStart: slotStartIso
  };

  try {
    const res = await fetch(`${CONFIG.API_BASE_URL}/appointments`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    if (res.status === 409) {
      // Conflict: double-booking caught!
      const errorData = await res.json();
      showBookingAlert(`That slot was just taken. ${errorData.message}`, "error");

      // Visibly mark the row as booked grey
      markRowBooked(slotStartIso);
      cancelInlineForm();
      // Refresh to synchronize open slots
      setTimeout(fetchSlots, 2000);
      return;
    }

    if (!res.ok) {
      const errorData = await res.json();
      throw new Error(errorData.message || "Failed to book appointment");
    }

    const appointment = await res.json();
    cancelInlineForm();
    showBookingAlert(`Appointment confirmed! Booking ID: ${appointment.id} for ${appointment.slotStart.replace("T", " at ")}`, "success");
    fetchSlots();
  } catch (err) {
    showBookingAlert(err.message, "error");
  }
}

// Mark taken slot row as greyed out
function markRowBooked(slotStartIso) {
  const targetRow = document.getElementById(`row-${slotStartIso}`);
  if (targetRow) {
    targetRow.className = "slot-row booked";
    const statusSpan = targetRow.querySelector(".slot-status");
    if (statusSpan) statusSpan.textContent = "Booked (Taken)";
    const actionDiv = targetRow.querySelector(".slot-action");
    if (actionDiv) actionDiv.innerHTML = '<span style="color: #6C7A89; font-size: 0.85rem;">Unavailable</span>';
  }
}

// 4. Patient Appointments History
async function fetchPatientAppointments() {
  const idInput = document.getElementById("patientLookupId");
  const list = document.getElementById("appointmentsList");
  const patientId = idInput.value;

  if (!patientId) {
    showAppointmentsAlert("Please provide a patient ID.", "error");
    return;
  }

  clearAppointmentsAlert();
  list.innerHTML = '<li class="empty-state">Loading appointments...</li>';

  try {
    const res = await fetch(`${CONFIG.API_BASE_URL}/patients/${patientId}/appointments`);
    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.message || "Could not retrieve appointments");
    }

    const appointments = await res.json();
    if (appointments.length === 0) {
      list.innerHTML = '<li class="empty-state">No appointments recorded for this patient.</li>';
      return;
    }

    list.innerHTML = appointments.map(app => {
      const isBooked = app.status === "BOOKED";
      const startClean = app.slotStart.replace("T", " at ");

      return `
        <li class="slot-row ${isBooked ? '' : 'booked'}">
          <span class="slot-time">#${app.id}</span>
          <span class="slot-status">
            ${app.doctorName} &mdash; ${startClean}
            <span style="font-weight: 700; margin-left: 0.5rem; color: ${isBooked ? 'var(--color-success)' : '#8C9BA5'};">
              [${app.status}]
            </span>
          </span>
          <div class="slot-action">
            ${isBooked ? `<button type="button" class="cancel-btn" onclick="cancelAppointment(${app.id})">Cancel appointment</button>` : ''}
          </div>
        </li>
      `;
    }).join("");
  } catch (err) {
    list.innerHTML = `<li class="empty-state">Error: ${err.message}</li>`;
  }
}

// 5. Cancel Appointment
async function cancelAppointment(appointmentId) {
  if (!confirm("Are you sure you want to cancel this appointment?")) return;

  try {
    const res = await fetch(`${CONFIG.API_BASE_URL}/appointments/${appointmentId}/cancel`, {
      method: "PUT"
    });

    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.message || "Cancellation failed");
    }

    showAppointmentsAlert("Appointment cancelled successfully. The slot is now open.", "success");
    fetchPatientAppointments();
  } catch (err) {
    showAppointmentsAlert(err.message, "error");
  }
}

function showBookingAlert(msg, type) {
  const box = document.getElementById("bookingAlertBox");
  box.innerHTML = `<div class="msg-banner ${type}">${escapeHtml(msg)}</div>`;
}

function clearBookingAlert() {
  document.getElementById("bookingAlertBox").innerHTML = "";
}

function showAppointmentsAlert(msg, type) {
  const box = document.getElementById("myAppointmentsAlertBox");
  box.innerHTML = `<div class="msg-banner ${type}">${escapeHtml(msg)}</div>`;
}

function clearAppointmentsAlert() {
  document.getElementById("myAppointmentsAlertBox").innerHTML = "";
}

function escapeHtml(str) {
  if (!str) return "";
  return String(str).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

