<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import MultiSelect from "./MultiSelect.vue";

const appOptions = ["vue-demo", "java-demo", "python-demo", "go-demo"];
const regionOptions = ["Asia", "Europe", "North America", "South America", "Africa"];
const subjectOptions = ["internal", "vip", "beta", "public"];
const environmentOptions = ["local", "dev", "sit", "uat", "prod"];

const baseUrl = ref("http://localhost:8080");

// --- state ---
const flagDefinitions = ref<any[]>([]);
const busy = ref(false);
const message = ref("");

// create panel
const showCreatePanel = ref(false);
const newFlag = ref("");
const newFlagDescription = ref("");

// configure panel
const configuringFlagKey = ref<string | null>(null);
const cfgApps = ref(["vue-demo"]);
const cfgEnvironment = ref("local");
const cfgRegions = ref(["Asia"]);
const cfgSubject = ref("vip");
const cfgRelease = ref(todayRelease());
const cfgEnabled = ref(true);
const cfgRollout = ref(100);
const latestSnapshot = ref<any | null>(null);

// evaluation
const evalApp = ref("vue-demo");
const evalEnvironment = ref("local");
const evalFlagKey = ref("");
const evalSubjectKey = ref("demo-user-001");
const evalRegion = ref("Asia");
const evalSubject = ref("vip");
const evalRelease = ref(todayRelease());
const evaluation = ref<any | null>(null);
const explain = ref<any | null>(null);

// --- computed ---
const releaseOptions = computed(() =>
  Array.from({ length: 7 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() + i);
    return formatRelease(d);
  })
);

const conditionPreview = computed(() =>
  JSON.stringify({ region: cfgRegions.value, subject: cfgSubject.value, release: cfgRelease.value }, null, 2)
);

const configuringFlag = computed(() =>
  configuringFlagKey.value ? flagDefinitions.value.find(f => f.flagKey === configuringFlagKey.value) : null
);

// --- helpers ---
function todayRelease() {
  return formatRelease(new Date());
}

function formatRelease(d: Date) {
  return `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, "0")}${String(d.getDate()).padStart(2, "0")}`;
}

async function api(path: string, options: RequestInit = {}) {
  const response = await fetch(`${baseUrl.value}${path}`, {
    ...options,
    headers: { "Content-Type": "application/json", ...(options.headers || {}) }
  });
  if (!response.ok) throw new Error(await response.text());
  return response.json();
}

// --- actions ---
async function load() {
  busy.value = true;
  message.value = "";
  try {
    flagDefinitions.value = await api("/api/v1/flags?appKey=&environment=");
    if (!evalFlagKey.value && flagDefinitions.value.length) {
      evalFlagKey.value = flagDefinitions.value[0].flagKey;
    }
  } catch (e) {
    message.value = String(e);
  } finally {
    busy.value = false;
  }
}

async function createFlag() {
  if (!newFlag.value.trim()) { message.value = "Please enter a flag key."; return; }
  busy.value = true;
  message.value = "";
  try {
    const created = await api("/api/v1/flags", {
      method: "POST",
      body: JSON.stringify({ flag: newFlag.value.trim(), description: newFlagDescription.value.trim(), type: "boolean" })
    });
    message.value = `Flag "${created.flagKey}" created.`;
    newFlag.value = "";
    newFlagDescription.value = "";
    showCreatePanel.value = false;
    await load();
    startConfigure(created.flagKey);
  } catch (e) {
    message.value = String(e);
  } finally {
    busy.value = false;
  }
}

function startConfigure(flagKey: string) {
  configuringFlagKey.value = flagKey;
  evalFlagKey.value = flagKey;
  evaluation.value = null;
  explain.value = null;
  loadLatestSnapshot();
}

function cancelConfigure() {
  configuringFlagKey.value = null;
}

async function configureFlag() {
  if (!configuringFlagKey.value) return;
  busy.value = true;
  message.value = "";
  try {
    await api(`/api/v1/flags/${encodeURIComponent(configuringFlagKey.value)}/configs`, {
      method: "POST",
      body: JSON.stringify({
        appKeys: cfgApps.value,
        environment: cfgEnvironment.value,
        regions: cfgRegions.value,
        subject: cfgSubject.value,
        release: cfgRelease.value,
        enabled: cfgEnabled.value,
        rolloutPercentage: cfgRollout.value,
        conditionJson: conditionPreview.value
      })
    });
    evalApp.value = cfgApps.value[0] || "vue-demo";
    evalEnvironment.value = cfgEnvironment.value;
    evalRegion.value = cfgRegions.value[0] || "Asia";
    evalSubject.value = cfgSubject.value;
    evalRelease.value = cfgRelease.value;
    message.value = "Configuration saved. Click Publish to push a snapshot.";
    await loadLatestSnapshot();
    await load();
  } catch (e) {
    message.value = String(e);
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
      body: JSON.stringify({ appKey: evalApp.value, environment: evalEnvironment.value, actor: "web-admin" })
    });
    message.value = `Published ${evalApp.value}/${evalEnvironment.value} snapshot v${latestSnapshot.value.version}.`;
  } catch (e) {
    message.value = String(e);
  } finally {
    busy.value = false;
  }
}

async function loadLatestSnapshot() {
  try {
    latestSnapshot.value = await api(`/api/v1/snapshots/latest?appKey=${encodeURIComponent(evalApp.value)}&environment=${encodeURIComponent(evalEnvironment.value)}`);
  } catch {
    latestSnapshot.value = null;
  }
}

async function runEvaluation() {
  busy.value = true;
  message.value = "";
  try {
    const context = { subjectKey: evalSubjectKey.value, region: evalRegion.value, subject: evalSubject.value, release: evalRelease.value };
    evaluation.value = await api(`/api/v1/evaluations/flags/${encodeURIComponent(evalFlagKey.value)}`, {
      method: "POST",
      body: JSON.stringify({ appKey: evalApp.value, environment: evalEnvironment.value, context })
    });
    explain.value = await api(`/api/v1/evaluations:explain/${encodeURIComponent(evalFlagKey.value)}`, {
      method: "POST",
      body: JSON.stringify({ appKey: evalApp.value, environment: evalEnvironment.value, context })
    });
  } catch (e) {
    message.value = String(e);
  } finally {
    busy.value = false;
  }
}

function fakeWorkflow(action: string) {
  message.value = `${action} is intentionally disabled in this demo. Publish is the real action.`;
}

function adjustRollout(mode: "increase" | "decrease" | "full" | "kill") {
  if (mode === "full") cfgRollout.value = 100;
  else if (mode === "kill") cfgRollout.value = 0;
  else if (mode === "increase") cfgRollout.value = Math.min(100, cfgRollout.value + 10);
  else cfgRollout.value = Math.max(0, cfgRollout.value - 10);
}

onMounted(load);
</script>

<template>
  <main class="shell">

    <!-- topbar -->
    <section class="topbar">
      <div>
        <h1>Feature Admin</h1>
        <p>Backend: <strong>{{ baseUrl }}</strong></p>
      </div>
      <div class="top-actions">
        <input v-model="baseUrl" aria-label="Backend URL" />
        <button :disabled="busy" @click="load">Refresh</button>
      </div>
    </section>

    <!-- create flag trigger -->
    <div class="table-toolbar">
      <button class="primary" @click="showCreatePanel = !showCreatePanel">
        {{ showCreatePanel ? '✕ Cancel' : '+ New Flag' }}
      </button>
    </div>

    <!-- create flag panel — only when showCreatePanel -->
    <section v-if="showCreatePanel" class="panel create-panel">
      <h2>Create Flag</h2>
      <div class="create-grid">
        <label>Flag key<input v-model="newFlag" placeholder="e.g. new-checkout" /></label>
        <label>Description<input v-model="newFlagDescription" placeholder="What this flag controls" /></label>
      </div>
      <button class="primary" :disabled="busy || !newFlag.trim()" @click="createFlag">Create</button>
    </section>

    <!-- flag table -->
    <section class="panel table-panel">
      <table>
        <thead>
          <tr>
            <th>Flag</th>
            <th>Description</th>
            <th>Type</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="flag in flagDefinitions"
            :key="flag.flagKey"
            :class="{ 'row-active': configuringFlagKey === flag.flagKey }"
          >
            <td class="flag-key">{{ flag.flagKey }}</td>
            <td class="flag-desc">{{ flag.description }}</td>
            <td>{{ flag.type }}</td>
            <td><span class="badge" :class="flag.status">{{ flag.status }}</span></td>
            <td>
              <button
                class="btn-configure"
                :class="{ active: configuringFlagKey === flag.flagKey }"
                @click="configuringFlagKey === flag.flagKey ? cancelConfigure() : startConfigure(flag.flagKey)"
              >
                {{ configuringFlagKey === flag.flagKey ? 'Close' : 'Configure' }}
              </button>
            </td>
          </tr>
          <tr v-if="!flagDefinitions.length">
            <td colspan="5" class="empty-row">No flags yet. Click + New Flag to create one.</td>
          </tr>
        </tbody>
      </table>
    </section>

    <!-- configure flag panel — only when a flag is selected -->
    <section v-if="configuringFlag" class="panel configure-panel">
      <div class="configure-header">
        <h2>Configure Flag: <span class="flag-name">{{ configuringFlag.flagKey }}</span></h2>
        <button @click="cancelConfigure">✕</button>
      </div>

      <div class="cfg-grid">
        <label>Apps
          <MultiSelect v-model="cfgApps" :options="appOptions" placeholder="— select apps —" />
        </label>
        <label>Regions
          <MultiSelect v-model="cfgRegions" :options="regionOptions" placeholder="— select regions —" />
        </label>
        <div class="cfg-col">
          <label>Environment
            <select v-model="cfgEnvironment">
              <option v-for="e in environmentOptions" :key="e" :value="e">{{ e }}</option>
            </select>
          </label>
          <label>Subject
            <select v-model="cfgSubject">
              <option v-for="s in subjectOptions" :key="s" :value="s">{{ s }}</option>
            </select>
          </label>
          <label>Release
            <div class="inline-row">
              <input v-model="cfgRelease" />
              <select v-model="cfgRelease">
                <option v-for="r in releaseOptions" :key="r" :value="r">{{ r }}</option>
              </select>
            </div>
          </label>
          <label>Enabled
            <div class="inline-row">
              <input type="checkbox" v-model="cfgEnabled" style="width:auto;" />
              <span>{{ cfgEnabled ? 'On' : 'Off (kill switch)' }}</span>
            </div>
          </label>
          <label>Rollout {{ cfgRollout }}%
            <input v-model.number="cfgRollout" type="range" min="0" max="100" />
            <div class="rollout-btns">
              <button @click="adjustRollout('increase')">+10%</button>
              <button @click="adjustRollout('decrease')">-10%</button>
              <button @click="adjustRollout('full')">Full</button>
              <button class="btn-kill" @click="adjustRollout('kill')">Kill switch</button>
            </div>
          </label>
        </div>
        <div class="cfg-col">
          <div class="condition-label">Condition preview</div>
          <pre class="condition-pre">{{ conditionPreview }}</pre>
        </div>
      </div>

      <div class="env-path">
        <span>local</span><span class="arrow">→</span>
        <span>dev</span><span class="arrow">→</span>
        <span>sit</span><span class="arrow">→</span>
        <span>uat</span><span class="arrow">→</span>
        <span class="env-current">prod</span>
      </div>

      <div class="configure-actions">
        <button @click="fakeWorkflow('Submit')">Submit</button>
        <button @click="fakeWorkflow('Approve')">Approve</button>
        <button class="primary" :disabled="busy" @click="configureFlag">Save Config</button>
        <button class="primary" :disabled="busy" @click="publish">Publish</button>
        <span v-if="latestSnapshot" class="snapshot-meta">
          Snapshot v{{ latestSnapshot.version }} · {{ latestSnapshot.checksum?.slice(0, 16) }}…
        </span>
      </div>
    </section>

    <!-- evaluation -->
    <section class="panel eval-panel">
      <h2>Evaluation Playground</h2>
      <div class="eval-grid">
        <label>App
          <select v-model="evalApp">
            <option v-for="app in appOptions" :key="app" :value="app">{{ app }}</option>
          </select>
        </label>
        <label>Environment
          <select v-model="evalEnvironment">
            <option v-for="e in environmentOptions" :key="e" :value="e">{{ e }}</option>
          </select>
        </label>
        <label>Flag
          <select v-model="evalFlagKey">
            <option v-for="flag in flagDefinitions" :key="flag.flagKey" :value="flag.flagKey">{{ flag.flagKey }}</option>
          </select>
        </label>
        <label>Subject key<input v-model="evalSubjectKey" /></label>
        <label>Region
          <select v-model="evalRegion">
            <option v-for="r in regionOptions" :key="r" :value="r">{{ r }}</option>
          </select>
        </label>
        <label>Subject
          <select v-model="evalSubject">
            <option v-for="s in subjectOptions" :key="s" :value="s">{{ s }}</option>
          </select>
        </label>
        <label>Release
          <select v-model="evalRelease">
            <option v-for="r in releaseOptions" :key="r" :value="r">{{ r }}</option>
          </select>
        </label>
      </div>
      <button class="primary" :disabled="busy || !evalFlagKey" @click="runEvaluation">Evaluate</button>

      <div v-if="evaluation" class="eval-result">
        <div class="value-badge" :class="{ on: evaluation.enabled }">{{ evaluation.enabled ? 'enabled' : 'disabled' }}</div>
        <pre>{{ JSON.stringify(explain, null, 2) }}</pre>
      </div>
      <p v-else class="empty">Publish a snapshot, then run an evaluation.</p>
    </section>

    <p v-if="message" class="message">{{ message }}</p>
  </main>
</template>
