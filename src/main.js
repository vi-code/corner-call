(function () {
  const STORAGE_KEY = "corner-call-settings-v1";

  const presets = [
    { id: "classic", label: "5 x 3:00", rounds: 5, roundSeconds: 180, restSeconds: 60 },
    { id: "champ", label: "12 x 2:00", rounds: 12, roundSeconds: 120, restSeconds: 60 },
    { id: "grind", label: "3 x 5:00", rounds: 3, roundSeconds: 300, restSeconds: 90 },
    { id: "quick", label: "4 x 1:30", rounds: 4, roundSeconds: 90, restSeconds: 45 }
  ];

  const punches = [
    { code: "1", name: "jab", weight: 6 },
    { code: "2", name: "cross", weight: 6 },
    { code: "3", name: "lead hook", weight: 4 },
    { code: "4", name: "rear hook", weight: 3 },
    { code: "5", name: "lead uppercut", weight: 3 },
    { code: "6", name: "rear uppercut", weight: 3 }
  ];

  const defensiveMoves = [
    { code: "duck", name: "duck", weight: 2 },
    { code: "slip", name: "slip", weight: 2 },
    { code: "roll", name: "roll", weight: 1 }
  ];

  const signatureCombos = [
    ["1", "2", "1", "2"],
    ["1", "2", "3"],
    ["1", "duck", "3"],
    ["5", "6", "5", "6"],
    ["1", "1", "2"],
    ["2", "3", "2"],
    ["1", "2", "5", "2"],
    ["3", "2", "3"]
  ];

  const defaultSettings = {
    presetId: "classic",
    rounds: 5,
    roundSeconds: 180,
    restSeconds: 60,
    paceSeconds: 4,
    comboLength: 4,
    includeDefense: true,
    voiceEnabled: true
  };

  const state = {
    settings: loadSettings(),
    status: "idle",
    phase: "work",
    currentRound: 1,
    phaseRemaining: 180,
    phaseDuration: 180,
    nextCallIn: 0,
    comboCount: 0,
    recentCombos: [],
    tickId: null,
    audioContext: null
  };

  const el = {
    phaseLabel: document.getElementById("phaseLabel"),
    roundLabel: document.getElementById("roundLabel"),
    timeRemaining: document.getElementById("timeRemaining"),
    timerHint: document.getElementById("timerHint"),
    timerRing: document.getElementById("timerRing"),
    currentCombo: document.getElementById("currentCombo"),
    comboNames: document.getElementById("comboNames"),
    startPauseButton: document.getElementById("startPauseButton"),
    skipButton: document.getElementById("skipButton"),
    resetButton: document.getElementById("resetButton"),
    presetGrid: document.getElementById("presetGrid"),
    roundsInput: document.getElementById("roundsInput"),
    roundMinutesInput: document.getElementById("roundMinutesInput"),
    restSecondsInput: document.getElementById("restSecondsInput"),
    paceInput: document.getElementById("paceInput"),
    paceValue: document.getElementById("paceValue"),
    comboLengthInput: document.getElementById("comboLengthInput"),
    comboLengthValue: document.getElementById("comboLengthValue"),
    defenseToggle: document.getElementById("defenseToggle"),
    voiceToggle: document.getElementById("voiceToggle"),
    formatSummary: document.getElementById("formatSummary"),
    engineSummary: document.getElementById("engineSummary"),
    comboCount: document.getElementById("comboCount"),
    recentCombos: document.getElementById("recentCombos"),
    punchGrid: document.getElementById("punchGrid")
  };

  init();

  function init() {
    state.phaseRemaining = state.settings.roundSeconds;
    state.phaseDuration = state.settings.roundSeconds;
    renderPresets();
    renderPunchKey();
    bindEvents();
    generateAndShowCombo(false);
    render();
  }

  function bindEvents() {
    el.startPauseButton.addEventListener("click", toggleSession);
    el.skipButton.addEventListener("click", function () {
      generateAndShowCombo(state.status === "running");
      state.nextCallIn = state.settings.paceSeconds;
      render();
    });
    el.resetButton.addEventListener("click", resetSession);

    el.roundsInput.addEventListener("change", function () {
      updateSettings({ rounds: clampNumber(el.roundsInput.value, 1, 15) });
    });
    el.roundMinutesInput.addEventListener("change", function () {
      updateSettings({
        roundSeconds: clampNumber(el.roundMinutesInput.value, 1, 12) * 60
      });
    });
    el.restSecondsInput.addEventListener("change", function () {
      updateSettings({ restSeconds: clampNumber(el.restSecondsInput.value, 15, 180) });
    });
    el.paceInput.addEventListener("input", function () {
      updateSettings({ paceSeconds: clampNumber(el.paceInput.value, 2, 9) }, false);
    });
    el.comboLengthInput.addEventListener("input", function () {
      updateSettings({ comboLength: clampNumber(el.comboLengthInput.value, 2, 6) }, false);
    });
    el.defenseToggle.addEventListener("change", function () {
      updateSettings({ includeDefense: el.defenseToggle.checked });
      generateAndShowCombo(false);
    });
    el.voiceToggle.addEventListener("change", function () {
      updateSettings({ voiceEnabled: el.voiceToggle.checked });
    });
  }

  function renderPresets() {
    el.presetGrid.innerHTML = "";
    presets.forEach(function (preset) {
      const button = document.createElement("button");
      button.type = "button";
      button.textContent = preset.label;
      button.dataset.presetId = preset.id;
      button.addEventListener("click", function () {
        updateSettings({
          presetId: preset.id,
          rounds: preset.rounds,
          roundSeconds: preset.roundSeconds,
          restSeconds: preset.restSeconds
        });
      });
      el.presetGrid.appendChild(button);
    });
  }

  function renderPunchKey() {
    el.punchGrid.innerHTML = "";
    punches.forEach(function (punch) {
      const item = document.createElement("div");
      item.className = "punch";
      item.innerHTML = "<strong>" + punch.code + "</strong><span>" + punch.name + "</span>";
      el.punchGrid.appendChild(item);
    });
  }

  function toggleSession() {
    primeAudio();
    if (state.status === "running") {
      pauseSession();
      return;
    }
    if (state.status === "complete") {
      resetSession();
    }
    const isFreshStart = state.status === "idle";
    state.status = "running";
    el.startPauseButton.textContent = "Pause";
    if (isFreshStart && state.phase === "work") {
      generateAndShowCombo(true);
      state.nextCallIn = state.settings.paceSeconds;
    }
    startTicking();
    render();
  }

  function pauseSession() {
    state.status = "paused";
    stopTicking();
    el.startPauseButton.textContent = "Resume";
    render();
  }

  function resetSession() {
    stopTicking();
    state.status = "idle";
    state.phase = "work";
    state.currentRound = 1;
    state.phaseRemaining = state.settings.roundSeconds;
    state.phaseDuration = state.settings.roundSeconds;
    state.nextCallIn = 0;
    state.comboCount = 0;
    state.recentCombos = [];
    el.startPauseButton.textContent = "Start";
    if ("speechSynthesis" in window) {
      window.speechSynthesis.cancel();
    }
    generateAndShowCombo(false);
    render();
  }

  function startTicking() {
    stopTicking();
    state.tickId = window.setInterval(tick, 1000);
  }

  function stopTicking() {
    if (state.tickId) {
      window.clearInterval(state.tickId);
      state.tickId = null;
    }
  }

  function tick() {
    if (state.status !== "running") {
      return;
    }

    if (state.phase === "work") {
      state.nextCallIn -= 1;
      if (state.nextCallIn <= 0) {
        generateAndShowCombo(true);
        state.nextCallIn = state.settings.paceSeconds;
      }
    }

    state.phaseRemaining -= 1;

    if (state.phaseRemaining <= 0) {
      advancePhase();
    }

    render();
  }

  function advancePhase() {
    playBell();
    if (state.phase === "work" && state.currentRound < state.settings.rounds) {
      state.phase = "rest";
      state.phaseRemaining = state.settings.restSeconds;
      state.phaseDuration = state.settings.restSeconds;
      speak("Rest. Breathe and reset.");
      return;
    }

    if (state.phase === "rest") {
      state.currentRound += 1;
      state.phase = "work";
      state.phaseRemaining = state.settings.roundSeconds;
      state.phaseDuration = state.settings.roundSeconds;
      state.nextCallIn = 0;
      speak("Round " + state.currentRound + ". Work.");
      return;
    }

    state.status = "complete";
    stopTicking();
    el.startPauseButton.textContent = "Start again";
    state.phaseRemaining = 0;
    speak("Workout complete.");
  }

  function generateAndShowCombo(announce) {
    const combo = buildCombo();
    const names = combo.map(getMoveName);
    el.currentCombo.textContent = combo.join(" - ");
    el.comboNames.textContent = names.join(", ");

    if (state.status === "running" || announce) {
      state.comboCount += 1;
      state.recentCombos.unshift(combo.join(" - "));
      state.recentCombos = state.recentCombos.slice(0, 7);
    }

    if (announce) {
      speak(combo.join(", "));
    }
  }

  function buildCombo() {
    const shouldUseSignature = Math.random() < 0.45;
    const availableSignatures = signatureCombos.filter(function (combo) {
      return state.settings.includeDefense || combo.every(isPunchCode);
    });

    if (shouldUseSignature && availableSignatures.length) {
      return sample(availableSignatures).slice(0, state.settings.comboLength);
    }

    const combo = [];
    const pool = state.settings.includeDefense ? punches.concat(defensiveMoves) : punches;
    while (combo.length < state.settings.comboLength) {
      const next = weightedSample(pool).code;
      const previous = combo[combo.length - 1];
      const bothDefense = !isPunchCode(previous) && !isPunchCode(next);
      if (bothDefense) {
        continue;
      }
      combo.push(next);
    }
    return combo;
  }

  function speak(text) {
    if (window.AndroidCoach && typeof window.AndroidCoach.speak === "function") {
      window.AndroidCoach.speak(text);
      return;
    }
    if (!state.settings.voiceEnabled || !("speechSynthesis" in window)) {
      return;
    }
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 1.02;
    utterance.pitch = 0.9;
    utterance.volume = 1;
    window.speechSynthesis.speak(utterance);
  }

  function primeAudio() {
    if (!state.audioContext) {
      const Context = window.AudioContext || window.webkitAudioContext;
      if (Context) {
        state.audioContext = new Context();
      }
    }
    if (state.audioContext && state.audioContext.state === "suspended") {
      state.audioContext.resume();
    }
  }

  function playBell() {
    if (window.AndroidCoach && typeof window.AndroidCoach.bell === "function") {
      window.AndroidCoach.bell();
      return;
    }
    if (!state.audioContext) {
      return;
    }
    const now = state.audioContext.currentTime;
    [0, 0.18].forEach(function (offset) {
      const osc = state.audioContext.createOscillator();
      const gain = state.audioContext.createGain();
      osc.frequency.value = 880;
      gain.gain.setValueAtTime(0.0001, now + offset);
      gain.gain.exponentialRampToValueAtTime(0.28, now + offset + 0.015);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + offset + 0.16);
      osc.connect(gain).connect(state.audioContext.destination);
      osc.start(now + offset);
      osc.stop(now + offset + 0.18);
    });
  }

  function updateSettings(next, resetTimer) {
    state.settings = Object.assign({}, state.settings, next);
    const matchingPreset = presets.find(function (preset) {
      return (
        preset.rounds === state.settings.rounds &&
        preset.roundSeconds === state.settings.roundSeconds &&
        preset.restSeconds === state.settings.restSeconds
      );
    });
    state.settings.presetId = matchingPreset ? matchingPreset.id : "custom";
    saveSettings();

    if (resetTimer !== false && state.status !== "running") {
      state.currentRound = 1;
      state.phase = "work";
      state.phaseRemaining = state.settings.roundSeconds;
      state.phaseDuration = state.settings.roundSeconds;
    }
    render();
  }

  function render() {
    const settings = state.settings;
    const progress = state.phaseDuration
      ? 360 - (state.phaseRemaining / state.phaseDuration) * 360
      : 360;

    el.phaseLabel.textContent = getPhaseLabel();
    el.roundLabel.textContent = "Round " + state.currentRound + " / " + settings.rounds;
    el.timeRemaining.textContent = formatTime(state.phaseRemaining);
    el.timerHint.textContent = getTimerHint();
    el.timerRing.style.setProperty("--progress", progress + "deg");
    el.formatSummary.textContent = settings.rounds + " x " + formatDuration(settings.roundSeconds);
    el.engineSummary.textContent = "Every " + settings.paceSeconds + " sec";
    el.comboCount.textContent = state.comboCount + (state.comboCount === 1 ? " call" : " calls");

    el.roundsInput.value = settings.rounds;
    el.roundMinutesInput.value = Math.round(settings.roundSeconds / 60);
    el.restSecondsInput.value = settings.restSeconds;
    el.paceInput.value = settings.paceSeconds;
    el.paceValue.textContent = settings.paceSeconds + "s";
    el.comboLengthInput.value = settings.comboLength;
    el.comboLengthValue.textContent = settings.comboLength;
    el.defenseToggle.checked = settings.includeDefense;
    el.voiceToggle.checked = settings.voiceEnabled;

    Array.from(el.presetGrid.children).forEach(function (button) {
      button.classList.toggle("is-active", button.dataset.presetId === settings.presetId);
    });

    renderRecentCombos();
  }

  function renderRecentCombos() {
    el.recentCombos.innerHTML = "";
    const combos = state.recentCombos.length ? state.recentCombos : ["Start a round to build the call log"];
    combos.forEach(function (combo) {
      const item = document.createElement("li");
      item.textContent = combo;
      el.recentCombos.appendChild(item);
    });
  }

  function getPhaseLabel() {
    if (state.status === "complete") {
      return "Complete";
    }
    if (state.status === "paused") {
      return "Paused";
    }
    if (state.status === "idle") {
      return "Ready";
    }
    return state.phase === "work" ? "Work" : "Rest";
  }

  function getTimerHint() {
    if (state.status === "idle") {
      return "Pick a format and tap Start";
    }
    if (state.status === "paused") {
      return "Paused";
    }
    if (state.status === "complete") {
      return "Nice work";
    }
    return state.phase === "work" ? "Hands up, chin down" : "Breathe";
  }

  function getMoveName(code) {
    const move = punches.concat(defensiveMoves).find(function (item) {
      return item.code === code;
    });
    return move ? move.name : code;
  }

  function isPunchCode(code) {
    return punches.some(function (punch) {
      return punch.code === code;
    });
  }

  function weightedSample(items) {
    const total = items.reduce(function (sum, item) {
      return sum + item.weight;
    }, 0);
    let target = Math.random() * total;
    for (let i = 0; i < items.length; i += 1) {
      target -= items[i].weight;
      if (target <= 0) {
        return items[i];
      }
    }
    return items[items.length - 1];
  }

  function sample(items) {
    return items[Math.floor(Math.random() * items.length)];
  }

  function formatTime(totalSeconds) {
    const safeSeconds = Math.max(0, totalSeconds);
    const minutes = Math.floor(safeSeconds / 60);
    const seconds = safeSeconds % 60;
    return String(minutes).padStart(2, "0") + ":" + String(seconds).padStart(2, "0");
  }

  function formatDuration(totalSeconds) {
    const safeSeconds = Math.max(0, totalSeconds);
    const minutes = Math.floor(safeSeconds / 60);
    const seconds = safeSeconds % 60;
    return minutes + ":" + String(seconds).padStart(2, "0");
  }

  function clampNumber(value, min, max) {
    const number = Number(value);
    if (Number.isNaN(number)) {
      return min;
    }
    return Math.min(max, Math.max(min, Math.round(number)));
  }

  function loadSettings() {
    try {
      const saved = JSON.parse(window.localStorage.getItem(STORAGE_KEY));
      return Object.assign({}, defaultSettings, saved || {});
    } catch (error) {
      return Object.assign({}, defaultSettings);
    }
  }

  function saveSettings() {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state.settings));
  }
})();
