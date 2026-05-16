<script setup lang="ts">
import { onMounted, ref } from "vue";
import { createFeatureClient, type FeatureEvaluation } from "../../frontend-sdk/src";

const baseUrl = ref("http://localhost:8080");
const subjectKey = ref("vue-demo-user");
const region = ref("us-east");
const evaluations = ref<FeatureEvaluation[]>([]);
const error = ref("");
const loading = ref(false);

async function loadFeatures() {
  loading.value = true;
  error.value = "";
  evaluations.value = [];

  const client = createFeatureClient({
    baseUrl: baseUrl.value,
    appKey: "checkout-service",
    environment: "local"
  });

  try {
    evaluations.value = await client.evaluateAll({
      subjectKey: subjectKey.value,
      attributes: {
        region: region.value,
        platform: "vue-demo"
      }
    }, "false");
  } catch (err) {
    error.value = String(err);
  } finally {
    loading.value = false;
  }
}

onMounted(loadFeatures);
</script>

<template>
  <main class="page">
    <section class="panel">
      <div class="header">
        <div>
          <h1>Vue SDK Demo</h1>
          <p>Displays all feature flags through the frontend SDK.</p>
        </div>
        <button :disabled="loading" @click="loadFeatures">Reload</button>
      </div>

      <div class="form">
        <label>Backend URL<input v-model="baseUrl" /></label>
        <label>Subject Key<input v-model="subjectKey" /></label>
        <label>Region<input v-model="region" /></label>
      </div>

      <div v-if="evaluations.length === 0 && !loading && !error" class="empty">
        <p>No feature flags found</p>
      </div>

      <div v-for="item in evaluations" :key="item.flagKey" class="flag-card" :class="{ enabled: item.value === 'true' }">
        <div class="flag-header">
          <span class="flag-key">{{ item.flagKey }}</span>
          <span class="flag-value">{{ item.value }}</span>
        </div>
        <dl>
          <div><dt>Enabled</dt><dd>{{ item.enabled ? "Yes" : "No" }}</dd></div>
          <div><dt>Reason</dt><dd>{{ item.reasonCode }}</dd></div>
          <div><dt>Snapshot</dt><dd>v{{ item.snapshotVersion }}</dd></div>
          <div><dt>Release</dt><dd>{{ item.releaseKey || "none" }}</dd></div>
        </dl>
      </div>

      <p v-if="loading" class="muted">Loading feature flags...</p>
      <p v-if="error" class="error">{{ error }}</p>
    </section>
  </main>
</template>

<style scoped>
.flag-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 1rem;
  margin-bottom: 1rem;
  background: white;
}

.flag-card.enabled {
  border-color: #10b981;
  background: linear-gradient(135deg, #ecfdf5 0%, #ffffff 100%);
}

.flag-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.flag-key {
  font-weight: 600;
  color: #1f2937;
}

.flag-value {
  font-size: 1.25rem;
  font-weight: 700;
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  background: #f3f4f6;
  color: #374151;
}

.flag-card.enabled .flag-value {
  background: #10b981;
  color: white;
}

dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.5rem;
  margin: 0;
}

dt {
  font-size: 0.75rem;
  font-weight: 500;
  color: #6b7280;
}

dd {
  margin: 0;
  font-size: 0.875rem;
  color: #1f2937;
}

.empty {
  text-align: center;
  padding: 2rem;
  color: #9ca3af;
}
</style>