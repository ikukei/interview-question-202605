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

async function createSampleFlag() {
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
        flagKey: flagKey.value,
        appKey: appKey.value,
        environment: environment.value,
        name: "New Checkout",
        description: "Enable the simplified checkout flow.",
        type: "boolean",
        defaultValue: "false",
        enabled: true,
        releaseKey: "release-2026-05-checkout"
      })
    });
    await api(`/api/v1/flags/${encodeURIComponent(flagKey.value)}/rules`, {
      method: "POST",
      body: JSON.stringify({
        appKey: appKey.value,
        environment: environment.value,
        priority: 1,
        conditions: [{ attribute: "region", operator: "equals", value: region.value }],
        rolloutPercentage: 100,
        variationValue: "true",
        enabled: true
      })
    });
    await publish();
    message.value = "Sample flag created and published.";
  } catch (error) {
    message.value = String(error);
  } finally {
    busy.value = false;
    await load();
  }
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
          <button :disabled="busy" @click="createSampleFlag">Create Sample</button>
          <button :disabled="busy" @click="publish">Publish</button>
        </div>
        <p v-if="latestSnapshot" class="meta">Snapshot v{{ latestSnapshot.version }} · {{ latestSnapshot.checksum }}</p>
      </aside>

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
          <span>Default: {{ selectedFlag.defaultValue }} · Enabled: {{ selectedFlag.enabled }}</span>
          <span>Rules: {{ selectedFlag.rules.length }}</span>
        </div>
      </section>

      <section class="panel playground">
        <h2>Evaluation Playground</h2>
        <div class="grid">
          <label>Flag Key<input v-model="flagKey" /></label>
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
