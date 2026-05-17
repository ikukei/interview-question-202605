<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

const appOptions = ["vue-demo", "java-demo", "python-demo", "go-demo"];
const regionOptions = ["Asia", "Europe", "North America", "South America", "Africa"];
const subjectOptions = ["internal", "vip", "beta", "public"];
const environmentOptions = ["local", "dev", "sit", "uat", "prod"];

const baseUrl = ref("http://localhost:8080");
const environment = ref("local");
const selectedApps = ref(["vue-demo"]);
const selectedRegions = ref(["Asia"]);
const subject = ref("vip");
const release = ref(todayRelease());
const rolloutPercentage = ref(100);

const flags = ref<any[]>([]);
const flagDefinitions = ref<any[]>([]);
const latestSnapshot = ref<any | null>(null);
const evaluation = ref<any | null>(null);
const explain = ref<any | null>(null);
const message = ref("");
const busy = ref(false);

const newFlag = ref("new-checkout");
const newFlagDescription = ref("Enables the simplified checkout experience.");
const newFlagValue = ref("true");

const selectedFlagKey = ref("new-checkout");
const evalApp = ref("vue-demo");
const evalRegion = ref("Asia");
const evalSubject = ref("vip");
const evalSubjectKey = ref("demo-user-001");
const evalRelease = ref(todayRelease());

const releaseOptions = computed(() => Array.from({ length: 7 }, (_, index) => {
  const date = new Date();
  date.setDate(date.getDate() + index);
  return formatRelease(date);
}));

const visibleFlags = computed(() => flags.value.length ? flags.value : flagDefinitions.value);
const selectedFlag = computed(() => visibleFlags.value.find((flag) => flag.flagKey === selectedFlagKey.value || flag.flag === selectedFlagKey.value));
const conditionPreview = computed(() => JSON.stringify({
  region: selectedRegions.value,
  subject: subject.value,
  release: release.value
}, null, 2));

function todayRelease() {
  return formatRelease(new Date());
}

function formatRelease(date: Date) {
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, "0");
  const dd = String(date.getDate()).padStart(2, "0");
  return `${yyyy}${mm}${dd}`;
}

async function api(path: string, options: RequestInit = {}) {
  const response = await fetch(`${baseUrl.value}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    }
  });
  if (!response.ok) {
    throw new Error(await response.text());
  }
  return response.json();
}

async function load() {
  busy.value = true;
  message.value = "";
  try {
    flagDefinitions.value = await api("/api/v1/flags?appKey=&environment=");
    flags.value = await api(`/api/v1/flags?appKey=${encodeURIComponent(evalApp.value)}&environment=${encodeURIComponent(environment.value)}`);
    if (visibleFlags.value.length && !visibleFlags.value.some((flag) => flag.flagKey === selectedFlagKey.value)) {
      selectedFlagKey.value = visibleFlags.value[0].flagKey;
    }
    await loadLatestSnapshot();
  } catch (error) {
    message.value = String(error);
  } finally {
    busy.value = false;
  }
}

async function loadLatestSnapshot() {
  try {
    latestSnapshot.value = await api(`/api/v1/snapshots/latest?appKey=${encodeURIComponent(evalApp.value)}&environment=${encodeURIComponent(environment.value)}`);
  } catch {
    latestSnapshot.value = null;
  }
}

async function createFlag() {
  if (!newFlag.value.trim()) {
    message.value = "Please enter a flag.";
    return;
  }
  busy.value = true;
  message.value = "";
  try {
    const created = await api("/api/v1/flags", {
      method: "POST",
      body: JSON.stringify({
        flag: newFlag.value.trim(),
        description: newFlagDescription.value.trim(),
        type: "text",
        value: newFlagValue.value
      })
    });
    selectedFlagKey.value = created.flagKey;
    message.value = `Flag "${created.flagKey}" created. Next, configure where it should run.`;
    await load();
  } catch (error) {
    message.value = String(error);
  } finally {
    busy.value = false;
  }
}

async function configureFlag() {
  if (!selectedFlagKey.value) {
    message.value = "Please select a flag first.";
    return;
  }
  busy.value = true;
  message.value = "";
  try {
    await api(`/api/v1/flags/${encodeURIComponent(selectedFlagKey.value)}/configs`, {
      method: "POST",
      body: JSON.stringify({
        appKeys: selectedApps.value,
        environment: environment.value,
        regions: selectedRegions.value,
        subject: subject.value,
        release: release.value,
        value: newFlagValue.value,
        enabled: true,
        rolloutPercentage: rolloutPercentage.value,
        conditionJson: conditionPreview.value
      })
    });
    evalApp.value = selectedApps.value[0] || "vue-demo";
    evalRegion.value = selectedRegions.value[0] || "Asia";
    evalSubject.value = subject.value;
    evalRelease.value = release.value;
    message.value = "Configuration saved locally. Click Publish to push a snapshot used by SDK demos.";
    await load();
  } catch (error) {
    message.value = String(error);
  } finally {
    busy.value = false;
  }
}

async function publish() {
  busy.value = true;
  message.value = "";
  try {
    latestSnapshot.value = await api("/api/v1/publish", {
      method: "POST",
      body: JSON.stringify({ appKey: evalApp.value, environment: environment.value, actor: "web-admin" })
    });
    message.value = `Published ${evalApp.value}/${environment.value} snapshot v${latestSnapshot.value.version}.`;
  } catch (error) {
    message.value = String(error);
  } finally {
    busy.value = false;
  }
}

async function runEvaluation() {
  busy.value = true;
  message.value = "";
  const context = {
    subjectKey: evalSubjectKey.value,
    region: evalRegion.value,
    subject: evalSubject.value,
    release: evalRelease.value,
    attributes: {
      region: evalRegion.value,
      subject: evalSubject.value,
      release: evalRelease.value
    }
  };
  try {
    evaluation.value = await api(`/api/v1/evaluations/flags/${encodeURIComponent(selectedFlagKey.value)}`, {
      method: "POST",
      body: JSON.stringify({ appKey: evalApp.value, environment: environment.value, context, defaultValue: "false" })
    });
    explain.value = await api(`/api/v1/evaluations:explain/${encodeURIComponent(selectedFlagKey.value)}`, {
      method: "POST",
      body: JSON.stringify({ appKey: evalApp.value, environment: environment.value, context, defaultValue: "false" })
    });
  } catch (error) {
    message.value = String(error);
  } finally {
    busy.value = false;
  }
}

function fakeWorkflow(action: string) {
  message.value = `${action} is intentionally disabled in this interview demo. Publish is the real action.`;
}

function promote(direction: "next" | "previous") {
  const index = environmentOptions.indexOf(environment.value);
  const nextIndex = direction === "next" ? Math.min(index + 1, environmentOptions.length - 1) : Math.max(index - 1, 0);
  environment.value = environmentOptions[nextIndex];
  message.value = `Promotion path moved to ${environment.value}. Configure and publish this environment when ready.`;
}

function adjustRollout(mode: "increase" | "decrease" | "full") {
  if (mode === "full") {
    rolloutPercentage.value = 100;
  } else if (mode === "increase") {
    rolloutPercentage.value = Math.min(100, rolloutPercentage.value + 10);
  } else {
    rolloutPercentage.value = Math.max(0, rolloutPercentage.value - 10);
  }
}

onMounted(load);
</script>

<template>
  <main class="shell">
    <section class="topbar">
      <div>
        <h1>Feature Admin</h1>
        <p>Backend server: <strong>{{ baseUrl }}</strong></p>
      </div>
      <div class="top-actions">
        <input v-model="baseUrl" aria-label="Backend server" />
        <button :disabled="busy" @click="load">Refresh</button>
      </div>
    </section>

    <section class="workspace">
      <section class="panel step-card">
        <span class="eyebrow">Step 1</span>
        <h2>Create Flag</h2>
        <label>flag<input v-model="newFlag" placeholder="new-checkout" /></label>
        <label>Description<textarea v-model="newFlagDescription" rows="3" /></label>
        <label>value<input v-model="newFlagValue" placeholder="any text, true, false, beta-A" /></label>
        <select v-model="newFlagValue">
          <option value="true">true</option>
          <option value="false">false</option>
        </select>
        <button class="primary" :disabled="busy" @click="createFlag">Create Flag</button>
      </section>

      <section class="panel step-card">
        <span class="eyebrow">Step 2</span>
        <h2>Configure Flag</h2>
        <label>Flag
          <select v-model="selectedFlagKey">
            <option v-for="flag in visibleFlags" :key="flag.flagKey" :value="flag.flagKey">{{ flag.flagKey }}</option>
          </select>
        </label>
        <label>Apps
          <select v-model="selectedApps" multiple>
            <option v-for="app in appOptions" :key="app" :value="app">{{ app }}</option>
          </select>
        </label>
        <label>Regions
          <select v-model="selectedRegions" multiple>
            <option v-for="region in regionOptions" :key="region" :value="region">{{ region }}</option>
          </select>
        </label>
        <div class="grid">
          <label>Subject
            <select v-model="subject">
              <option v-for="item in subjectOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>Release
            <input v-model="release" />
            <select v-model="release">
              <option v-for="item in releaseOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>Environment
            <select v-model="environment">
              <option v-for="item in environmentOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>Rollout {{ rolloutPercentage }}%
            <input v-model.number="rolloutPercentage" type="range" min="0" max="100" />
          </label>
        </div>
        <div class="path">local -> dev -> sit -> uat -> prod</div>
        <div class="actions">
          <button @click="promote('next')">Promotion</button>
          <button @click="promote('previous')">Rollback</button>
          <button @click="adjustRollout('increase')">increase</button>
          <button @click="adjustRollout('decrease')">decrease</button>
          <button @click="adjustRollout('full')">full</button>
          <button @click="fakeWorkflow('kill switch')">kill switch</button>
        </div>
        <pre class="condition">{{ conditionPreview }}</pre>
        <button class="primary" :disabled="busy" @click="configureFlag">Save Configuration</button>
      </section>

      <section class="panel workflow">
        <button @click="fakeWorkflow('Submit')">Submit</button>
        <button @click="fakeWorkflow('Approve')">Approve</button>
        <button class="primary" :disabled="busy" @click="publish">Publish</button>
        <span v-if="latestSnapshot" class="meta">Snapshot v{{ latestSnapshot.version }} {{ latestSnapshot.checksum }}</span>
      </section>

      <section class="panel list-panel">
        <h2>Current {{ evalApp }}/{{ environment }} Configs</h2>
        <div class="flag-list">
          <button
            v-for="flag in visibleFlags"
            :key="`${flag.flagKey}-${flag.configId || 'definition'}`"
            :class="{ active: flag.flagKey === selectedFlagKey }"
            @click="selectedFlagKey = flag.flagKey"
          >
            {{ flag.flagKey }} <small v-if="flag.configId">{{ flag.value }} / {{ flag.rolloutPercentage }}%</small>
          </button>
        </div>
        <p v-if="selectedFlag" class="meta">{{ selectedFlag.description }} | condition_json: {{ selectedFlag.conditionJson }}</p>
      </section>

      <section class="panel playground">
        <h2>Evaluation</h2>
        <div class="grid">
          <label>App
            <select v-model="evalApp">
              <option v-for="app in appOptions" :key="app" :value="app">{{ app }}</option>
            </select>
          </label>
          <label>Environment
            <select v-model="environment">
              <option v-for="item in environmentOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>Flag
            <select v-model="selectedFlagKey">
              <option v-for="flag in visibleFlags" :key="flag.flagKey" :value="flag.flagKey">{{ flag.flagKey }}</option>
            </select>
          </label>
          <label>Subject Key<input v-model="evalSubjectKey" /></label>
          <label>Region
            <select v-model="evalRegion">
              <option v-for="region in regionOptions" :key="region" :value="region">{{ region }}</option>
            </select>
          </label>
          <label>Subject
            <select v-model="evalSubject">
              <option v-for="item in subjectOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>Release
            <select v-model="evalRelease">
              <option v-for="item in releaseOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
        </div>
        <button class="primary" :disabled="busy" @click="runEvaluation">Evaluate</button>
      </section>

      <section class="panel result">
        <h2>Result</h2>
        <div v-if="evaluation" class="value" :class="{ on: evaluation.value === 'true' }">{{ evaluation.value }}</div>
        <pre v-if="explain">{{ JSON.stringify(explain, null, 2) }}</pre>
        <p v-else class="empty">Publish a snapshot, then run an evaluation.</p>
      </section>
    </section>

    <p v-if="message" class="message">{{ message }}</p>
  </main>
</template>
