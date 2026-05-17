<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
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
const messageIsError = ref(false);

// create / edit panel
const showCreatePanel = ref(false);
const editingFlagKey = ref<string | null>(null); // null = create mode, set = edit mode
const newFlag = ref("");
const newFlagDescription = ref("");
const newFlagRelease = ref(todayRelease());
const newFlagEnabled = ref(true);
const panelMessage = ref("");        // inline message next to Create/Update button
const panelMessageIsError = ref(false);

// configure panel
const envPipeline = ["local", "dev", "sit", "uat", "prod"];
const configuringFlagKey = ref<string | null>(null);
const cfgApps = ref(["vue-demo"]);
const cfgRegions = ref(["Asia"]);
const cfgSubject = ref("vip");
const cfgEnvLevel = ref(-1);          // -1=none, 0=local, 1=local+dev, …, 4=all
const cfgRollout = ref(100);
const approved = ref(false);
const latestSnapshot = ref<any | null>(null);

// evaluation
const evalApp = ref("vue-demo");
const evalEnvironment = ref("local");
const evalFlagKey = ref("");
const evalSubjectKey = ref("demo-user-001");
const evalRegion = ref("Asia");
const evalSubject = ref("vip");
const evaluation = ref<any | null>(null);
const explain = ref<any | null>(null);

// --- computed ---
const cfgEnvs = computed(() =>
  cfgEnvLevel.value < 0 ? [] : envPipeline.slice(0, cfgEnvLevel.value + 1)
);
const isProd = computed(() => cfgEnvLevel.value === envPipeline.length - 1);
const canApprove = computed(() => isProd.value && !approved.value && cfgEnvLevel.value >= 0);
const canPublish = computed(() => cfgEnvLevel.value >= 0 && (!isProd.value || approved.value));

const conditionPreview = computed(() =>
  JSON.stringify({ region: cfgRegions.value, subject: cfgSubject.value }, null, 2)
);

const configuringFlag = computed(() =>
  configuringFlagKey.value ? flagDefinitions.value.find(f => f.flagKey === configuringFlagKey.value) : null
);

// auto-adjust rollout and approval when pipeline level changes
watch(cfgEnvLevel, (level) => {
  approved.value = false;
  if (level === envPipeline.length - 1) cfgRollout.value = 10;   // prod → 10%
  else if (level >= 0) cfgRollout.value = 100;                    // non-prod → 100%
});

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
  if (!response.ok) {
    const text = await response.text();
    let msg = text;
    try { msg = JSON.parse(text).error ?? JSON.parse(text).message ?? text; } catch {}
    throw new Error(msg);
  }
  return response.json();
}

function showError(e: unknown) {
  message.value = String(e instanceof Error ? e.message : e);
  messageIsError.value = true;
}

function showInfo(msg: string) {
  message.value = msg;
  messageIsError.value = false;
}

function showPanelError(e: unknown) {
  panelMessage.value = String(e instanceof Error ? e.message : e);
  panelMessageIsError.value = true;
}

function showPanelInfo(msg: string) {
  panelMessage.value = msg;
  panelMessageIsError.value = false;
}

function openCreatePanel() {
  editingFlagKey.value = null;
  newFlag.value = "";
  newFlagDescription.value = "";
  newFlagRelease.value = todayRelease();
  newFlagEnabled.value = true;
  panelMessage.value = "";
  showCreatePanel.value = true;
}

function openEditPanel(flag: any) {
  editingFlagKey.value = flag.flagKey;
  newFlag.value = flag.flagKey;
  newFlagDescription.value = flag.description ?? "";
  newFlagRelease.value = flag.releaseKey ?? todayRelease();
  newFlagEnabled.value = flag.enabled ?? true;
  panelMessage.value = "";
  showCreatePanel.value = true;
}

function closePanel() {
  showCreatePanel.value = false;
  editingFlagKey.value = null;
  panelMessage.value = "";
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
    showError(e);
  } finally {
    busy.value = false;
  }
}

async function submitFlagPanel() {
  panelMessage.value = "";
  if (editingFlagKey.value) {
    // edit mode — PUT
    busy.value = true;
    try {
      await api(`/api/v1/flags/${encodeURIComponent(editingFlagKey.value)}`, {
        method: "PUT",
        body: JSON.stringify({ description: newFlagDescription.value.trim(), release: newFlagRelease.value, enabled: newFlagEnabled.value })
      });
      showPanelInfo(`Flag "${editingFlagKey.value}" updated.`);
      await load();
    } catch (e) {
      showPanelError(e);
    } finally {
      busy.value = false;
    }
  } else {
    // create mode — POST
    if (!newFlag.value.trim()) { showPanelError("Please enter a flag key."); return; }
    busy.value = true;
    try {
      const created = await api("/api/v1/flags", {
        method: "POST",
        body: JSON.stringify({ flag: newFlag.value.trim(), description: newFlagDescription.value.trim(), type: "boolean", release: newFlagRelease.value, enabled: newFlagEnabled.value })
      });
      closePanel();
      await load();
      startConfigure(created.flagKey);
    } catch (e) {
      showPanelError(e);
    } finally {
      busy.value = false;
    }
  }
}

function startConfigure(flagKey: string) {
  configuringFlagKey.value = flagKey;
  evalFlagKey.value = flagKey;
  cfgEnvLevel.value = -1;
  approved.value = false;
  evaluation.value = null;
  explain.value = null;
}

function cancelConfigure() {
  configuringFlagKey.value = null;
}

function upgrade() {
  if (cfgEnvLevel.value < envPipeline.length - 1) cfgEnvLevel.value++;
}

function downgrade() {
  if (cfgEnvLevel.value >= 0) cfgEnvLevel.value--;
}

function doApprove() {
  approved.value = true;
}

async function configureFlag() {
  if (!configuringFlagKey.value || cfgEnvLevel.value < 0) return;
  busy.value = true;
  message.value = "";
  try {
    for (const env of cfgEnvs.value) {
      await api(`/api/v1/flags/${encodeURIComponent(configuringFlagKey.value)}/configs`, {
        method: "POST",
        body: JSON.stringify({
          appKeys: cfgApps.value,
          environment: env,
          regions: cfgRegions.value,
          subject: cfgSubject.value,
          enabled: true,
          rolloutPercentage: cfgRollout.value,
          conditionJson: conditionPreview.value
        })
      });
    }
    evalApp.value = cfgApps.value[0] || "vue-demo";
    evalEnvironment.value = cfgEnvs.value[cfgEnvs.value.length - 1] || "local";
    evalRegion.value = cfgRegions.value[0] || "Asia";
    evalSubject.value = cfgSubject.value;
    showInfo(`Config saved for [${cfgEnvs.value.join(", ")}]. Click Publish to push a snapshot.`);
    await loadLatestSnapshot();
    await load();
  } catch (e) {
    showError(e);
  } finally {
    busy.value = false;
  }
}

async function publish() {
  if (!canPublish.value) return;
  busy.value = true;
  message.value = "";
  try {
    let last: any;
    for (const env of cfgEnvs.value) {
      for (const app of cfgApps.value) {
        last = await api("/api/v1/publish", {
          method: "POST",
          body: JSON.stringify({ appKey: app, environment: env, actor: "web-admin" })
        });
      }
    }
    latestSnapshot.value = last;
    approved.value = false;   // reset after publish
    showInfo(`Published [${cfgApps.value.join(", ")}] × [${cfgEnvs.value.join(", ")}] v${last?.version}.`);
  } catch (e) {
    showError(e);
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
    const context = { subjectKey: evalSubjectKey.value, region: evalRegion.value, subject: evalSubject.value };
    evaluation.value = await api(`/api/v1/evaluations/flags/${encodeURIComponent(evalFlagKey.value)}`, {
      method: "POST",
      body: JSON.stringify({ appKey: evalApp.value, environment: evalEnvironment.value, context })
    });
    explain.value = await api(`/api/v1/evaluations:explain/${encodeURIComponent(evalFlagKey.value)}`, {
      method: "POST",
      body: JSON.stringify({ appKey: evalApp.value, environment: evalEnvironment.value, context })
    });
  } catch (e) {
    showError(e);
  } finally {
    busy.value = false;
  }
}

function adjustRollout(delta: number) {
  cfgRollout.value = Math.max(0, Math.min(100, cfgRollout.value + delta));
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
      <button class="primary" @click="showCreatePanel ? closePanel() : openCreatePanel()">
        {{ showCreatePanel ? '✕ Cancel' : '+ New Flag' }}
      </button>
    </div>

    <!-- create / edit flag panel -->
    <section v-if="showCreatePanel" class="panel create-panel">
      <h2>{{ editingFlagKey ? 'Edit Flag' : 'Create Flag' }}</h2>
      <div class="create-grid">
        <label>Flag key
          <input v-model="newFlag" placeholder="e.g. new-checkout" :readonly="!!editingFlagKey" :class="{ 'input-readonly': !!editingFlagKey }" />
        </label>
        <label>Description<input v-model="newFlagDescription" placeholder="What this flag controls" /></label>
        <label>Release<input v-model="newFlagRelease" placeholder="YYYYMMDD" /></label>
        <label>Enabled
          <button
            type="button"
            class="toggle-btn"
            :class="newFlagEnabled ? 'toggle-on' : 'toggle-off'"
            @click="newFlagEnabled = !newFlagEnabled"
          >{{ newFlagEnabled ? 'Enabled' : 'Disabled' }}</button>
        </label>
        <label class="create-btn-cell">
          <span class="create-btn-label">&nbsp;</span>
          <div class="create-btn-row">
            <button class="primary" :disabled="busy || (!editingFlagKey && !newFlag.trim())" @click="submitFlagPanel">
              {{ editingFlagKey ? 'Update' : 'Create' }}
            </button>
            <span v-if="panelMessage" class="panel-msg" :class="{ 'panel-msg-error': panelMessageIsError }">{{ panelMessage }}</span>
          </div>
        </label>
      </div>
    </section>

    <!-- flag table -->
    <section class="panel table-panel">
      <table>
        <thead>
          <tr>
            <th>Flag</th>
            <th>Description</th>
            <th>Release</th>
            <th>Enabled</th>
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
            <td class="flag-release">{{ flag.releaseKey || '—' }}</td>
            <td>
              <span class="badge" :class="flag.enabled ? 'enabled' : 'disabled'">
                {{ flag.enabled ? 'enabled' : 'disabled' }}
              </span>
            </td>
            <td class="actions-cell">
              <button
                class="btn-action"
                @click="openEditPanel(flag)"
              >Edit</button>
              <button
                class="btn-configure btn-action"
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
        <h2>Configure: <span class="flag-name">{{ configuringFlag.flagKey }}</span></h2>
        <button @click="cancelConfigure">✕</button>
      </div>

      <!-- Row 1: Apps / Regions / Environment display / Subject -->
      <div class="cfg-row1">
        <label>Apps
          <MultiSelect v-model="cfgApps" :options="appOptions" placeholder="— select apps —" />
        </label>
        <label>Regions
          <MultiSelect v-model="cfgRegions" :options="regionOptions" placeholder="— select regions —" />
        </label>
        <label>Environment
          <div class="env-display-text">{{ cfgEnvs.length ? cfgEnvs.join(', ') : '— none —' }}</div>
        </label>
        <label>Subject
          <select v-model="cfgSubject">
            <option v-for="s in subjectOptions" :key="s" :value="s">{{ s }}</option>
          </select>
        </label>
      </div>

      <!-- Row 2: Pipeline | Rollout | Condition -->
      <div class="cfg-row2">

        <!-- Pipeline + upgrade/downgrade -->
        <div class="cfg-pipeline-col">
          <div class="pipeline-track">
            <template v-for="(env, i) in envPipeline" :key="env">
              <span class="pipeline-node" :class="{ 'node-active': i <= cfgEnvLevel }">{{ env }}</span>
              <span v-if="i < envPipeline.length - 1" class="pipeline-arrow" :class="{ 'arrow-active': i < cfgEnvLevel }">→</span>
            </template>
          </div>
          <div class="pipeline-btns">
            <button @click="downgrade" :disabled="cfgEnvLevel < 0">↙ Downgrade</button>
            <button class="primary" @click="upgrade" :disabled="cfgEnvLevel >= envPipeline.length - 1">Upgrade ↗</button>
          </div>
        </div>

        <!-- Rollout -->
        <div class="cfg-rollout-col">
          <label class="rollout-label">Rollout&nbsp;<strong>{{ cfgRollout }}%</strong>
            <input v-model.number="cfgRollout" type="range" min="0" max="100" class="rollout-slider" />
          </label>
          <div class="rollout-btns">
            <button class="btn-kill" @click="cfgRollout = 0">Kill Switch</button>
            <button @click="adjustRollout(-10)">−10%</button>
            <button @click="adjustRollout(+10)">+10%</button>
            <button @click="cfgRollout = 100">Full</button>
          </div>
        </div>

        <!-- Condition preview -->
        <div class="cfg-condition-col">
          <div class="condition-label">Condition preview</div>
          <pre class="condition-pre">{{ conditionPreview }}</pre>
        </div>
      </div>

      <!-- Row 3: actions -->
      <div class="configure-actions">
        <button class="primary" :disabled="busy || cfgEnvLevel < 0" @click="configureFlag">Save Config</button>
        <button :disabled="!canApprove" @click="doApprove">Approve</button>
        <button class="primary" :disabled="busy || !canPublish" @click="publish">Publish</button>
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
      </div>
      <button class="primary" :disabled="busy || !evalFlagKey" @click="runEvaluation">Evaluate</button>

      <div v-if="evaluation" class="eval-result">
        <div class="value-badge" :class="{ on: evaluation.enabled }">{{ evaluation.enabled ? 'enabled' : 'disabled' }}</div>
        <pre>{{ JSON.stringify(explain, null, 2) }}</pre>
      </div>
      <p v-else class="empty">Publish a snapshot, then run an evaluation.</p>
    </section>

    <p v-if="message" class="message" :class="{ 'message-error': messageIsError }">{{ message }}</p>
  </main>
</template>
