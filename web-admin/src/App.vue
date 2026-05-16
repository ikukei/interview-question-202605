<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

const baseUrl = ref("http://localhost:8080");
const appKey = ref("checkout-service");
const environment = ref("local");
const flagKey = ref("new-checkout");
const subjectKey = ref("admin-demo-user");
const region = ref("us-east");
const platform = ref("web-admin");
const flags = ref<any[]>([]);
const apps = ref<any[]>([]);
const latestSnapshot = ref<any | null>(null);
const evaluation = ref<any | null>(null);
const explain = ref<any | null>(null);
const message = ref("");
const busy = ref(false);

const newFlagKey = ref("");
const newFlagName = ref("");
const newFlagDescription = ref("");
const newFlagValue = ref("false");
const newFlagEnabled = ref(true);

const selectedFlag = computed(() => flags.value.find((flag) => flag.flagKey === flagKey.value));

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
    apps.value = await api("/api/v1/apps");
    flags.value = await api(`/api/v1/flags?appKey=${encodeURIComponent(appKey.value)}&environment=${encodeURIComponent(environment.value)}`);
    if (flags.value.length && !flags.value.some((flag) => flag.flagKey === flagKey.value)) {
      flagKey.value = flags.value[0].flagKey;
    }
    try {
      latestSnapshot.value = await api(`/api/v1/snapshots/latest?appKey=${encodeURIComponent(appKey.value)}&environment=${encodeURIComponent(environment.value)}`);
    } catch {
      latestSnapshot.value = null;
    }
  } catch (error) {
    message.value = String(error);
  } finally {
    busy.value = false;
  }
}

async function createNewFlag() {
  if (!newFlagKey.value.trim()) {
    message.value = "Please enter a flag key.";
    return;
  }
  
  busy.value = true;
  message.value = "";
  try {
    if (!apps.value.some((app) => app.appKey === appKey.value)) {
      await api("/api/v1/apps", {
        method: "POST",
        body: JSON.stringify({ appKey: appKey.value, name: "Checkout Service", owner: "Platform" })
      });
    }
    await api("/api/v1/flags", {
      method: "POST",
      body: JSON.stringify({
        flagKey: newFlagKey.value.trim(),
        appKey: appKey.value,
        environment: environment.value,
        name: newFlagName.value.trim() || newFlagKey.value.trim(),
        description: newFlagDescription.value.trim() || "No description",
        type: "boolean",
        defaultValue: newFlagValue.value,
        enabled: newFlagEnabled.value,
        releaseKey: null
      })
    });
    message.value = `Flag "${newFlagKey.value.trim()}" created successfully.`;
    newFlagKey.value = "";
    newFlagName.value = "";
    newFlagDescription.value = "";
    newFlagValue.value = "false";
    newFlagEnabled.value = true;
    await load();
  } catch (error) {
    message.value = String(error);
  } finally {
    busy.value = false;
  }
}

async function updateFlag() {
  if (!selectedFlag.value) return;
  
  busy.value = true;
  message.value = "";
  try {
    await api(`/api/v1/flags/${encodeURIComponent(selectedFlag.value.flagKey)}`, {
      method: "PUT",
      body: JSON.stringify({
        appKey: appKey.value,
        environment: environment.value,
        name: selectedFlag.value.name,
        description: selectedFlag.value.description,
        type: selectedFlag.value.type,
        defaultValue: selectedFlag.value.defaultValue,
        enabled: selectedFlag.value.enabled,
        releaseKey: selectedFlag.value.releaseKey
      })
    });
    message.value = "Flag updated successfully.";
    await load();
  } catch (error) {
    message.value = String(error);
  } finally {
    busy.value = false;
  }
}

async function toggleFlagValue() {
  if (!selectedFlag.value) return;
  
  selectedFlag.value.defaultValue = selectedFlag.value.defaultValue === "true" ? "false" : "true";
  await updateFlag();
}

async function toggleFlagEnabled() {
  if (!selectedFlag.value) return;
  
  selectedFlag.value.enabled = !selectedFlag.value.enabled;
  await updateFlag();
}

async function publish() {
  latestSnapshot.value = await api("/api/v1/publish", {
    method: "POST",
    body: JSON.stringify({ appKey: appKey.value, environment: environment.value, actor: "web-admin" })
  });
}

async function runEvaluation() {
  busy.value = true;
  message.value = "";
  const context = {
    subjectKey: subjectKey.value,
    attributes: {
      region: region.value,
      platform: platform.value
    }
  };
  try {
    evaluation.value = await api(`/api/v1/evaluations/flags/${encodeURIComponent(flagKey.value)}`, {
      method: "POST",
      body: JSON.stringify({ appKey: appKey.value, environment: environment.value, context, defaultValue: "false" })
    });
    explain.value = await api(`/api/v1/evaluations:explain/${encodeURIComponent(flagKey.value)}`, {
      method: "POST",
      body: JSON.stringify({ appKey: appKey.value, environment: environment.value, context, defaultValue: "false" })
    });
  } catch (error) {
    message.value = String(error);
  } finally {
    busy.value = false;
  }
}

onMounted(load);
</script>

<template>
  <main class="shell">
    <section class="topbar">
      <div>
        <h1>Feature Admin</h1>
        <p>Configure, publish, and evaluate feature flags against the local Spring Boot backend.</p>
      </div>
      <button :disabled="busy" @click="load">Refresh</button>
    </section>

    <section class="workspace">
      <aside class="panel">
        <h2>Scope</h2>
        <label>Backend URL<input v-model="baseUrl" /></label>
        <label>App Key<input v-model="appKey" /></label>
        <label>Environment<input v-model="environment" /></label>
        <div class="actions">
          <button :disabled="busy" @click="publish">Publish</button>
        </div>
        <p v-if="latestSnapshot" class="meta">Snapshot v{{ latestSnapshot.version }} · {{ latestSnapshot.checksum }}</p>
      </aside>

      <section class="panel">
        <h2>Create New Flag</h2>
        <label>Flag Key<input v-model="newFlagKey" placeholder="e.g., my-feature" /></label>
        <label>Flag Name<input v-model="newFlagName" placeholder="e.g., My Feature" /></label>
        <label>Description<input v-model="newFlagDescription" placeholder="Optional description" /></label>
        <div class="create-controls">
          <div class="control-row">
            <span class="label">Default Value:</span>
            <select v-model="newFlagValue">
              <option value="false">false</option>
              <option value="true">true</option>
            </select>
          </div>
          <div class="control-row">
            <span class="label">Enabled:</span>
            <select v-model="newFlagEnabled">
              <option :value="true">Yes</option>
              <option :value="false">No</option>
            </select>
          </div>
        </div>
        <button class="primary" :disabled="busy || !newFlagKey.trim()" @click="createNewFlag">Create Flag</button>
      </section>

      <section class="panel">
        <h2>Flags</h2>
        <div class="flag-list">
          <button
            v-for="flag in flags"
            :key="flag.flagKey"
            :class="{ active: flag.flagKey === flagKey }"
            @click="flagKey = flag.flagKey"
          >
            {{ flag.flagKey }}
          </button>
        </div>
        <div v-if="selectedFlag" class="details">
          <strong>{{ selectedFlag.name }}</strong>
          <span>{{ selectedFlag.description }}</span>
          <div class="flag-controls">
            <div class="control-group">
              <span class="label">Default Value:</span>
              <button 
                :class="{ active: selectedFlag.defaultValue === 'true' }"
                @click="toggleFlagValue"
              >
                {{ selectedFlag.defaultValue }}
              </button>
            </div>
            <div class="control-group">
              <span class="label">Enabled:</span>
              <button 
                :class="{ active: selectedFlag.enabled }"
                @click="toggleFlagEnabled"
              >
                {{ selectedFlag.enabled ? 'Yes' : 'No' }}
              </button>
            </div>
          </div>
          <span>Rules: {{ selectedFlag.rules.length }}</span>
        </div>
      </section>

      <section class="panel playground">
        <h2>Evaluation Playground</h2>
        <div class="selected-flag">
          <span class="label">Selected Flag:</span>
          <span class="value">{{ flagKey }}</span>
        </div>
        <div class="grid">
          <label>Subject Key<input v-model="subjectKey" /></label>
          <label>Region<input v-model="region" /></label>
          <label>Platform<input v-model="platform" /></label>
        </div>
        <button class="primary" :disabled="busy" @click="runEvaluation">Evaluate</button>
      </section>

      <section class="panel result">
        <h2>Result</h2>
        <div v-if="evaluation" class="value" :class="{ on: evaluation.value === 'true' }">
          {{ evaluation.value }}
        </div>
        <pre v-if="explain">{{ JSON.stringify(explain, null, 2) }}</pre>
        <p v-else class="empty">Run an evaluation to see the feature value and explanation.</p>
      </section>
    </section>

    <p v-if="message" class="message">{{ message }}</p>
  </main>
</template>