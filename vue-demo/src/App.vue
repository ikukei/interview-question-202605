<script setup lang="ts">
import { onMounted, ref } from "vue";
import { createFeatureClient, type FeatureEvaluation } from "../../frontend-sdk/src";

const baseUrl = ref("http://localhost:8080");
const subjectKey = ref("vue-demo-user");
const region = ref("us-east");
const evaluation = ref<FeatureEvaluation | null>(null);
const error = ref("");
const loading = ref(false);

async function loadFeature() {
  loading.value = true;
  error.value = "";
  evaluation.value = null;

  const client = createFeatureClient({
    baseUrl: baseUrl.value,
    appKey: "checkout-service",
    environment: "local"
  });

  try {
    evaluation.value = await client.evaluate("new-checkout", {
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

onMounted(loadFeature);
</script>

<template>
  <main class="page">
    <section class="panel">
      <div class="header">
        <div>
          <h1>Vue SDK Demo</h1>
          <p>Reads <code>new-checkout</code> through the frontend SDK.</p>
        </div>
        <button :disabled="loading" @click="loadFeature">Reload</button>
      </div>

      <div class="form">
        <label>Backend URL<input v-model="baseUrl" /></label>
        <label>Subject Key<input v-model="subjectKey" /></label>
        <label>Region<input v-model="region" /></label>
      </div>

      <div v-if="evaluation" class="result" :class="{ enabled: evaluation.value === 'true' }">
        <span>Feature value</span>
        <strong>{{ evaluation.value }}</strong>
      </div>

      <dl v-if="evaluation">
        <div><dt>Flag</dt><dd>{{ evaluation.flagKey }}</dd></div>
        <div><dt>Reason</dt><dd>{{ evaluation.reasonCode }}</dd></div>
        <div><dt>Snapshot</dt><dd>v{{ evaluation.snapshotVersion }}</dd></div>
        <div><dt>Release</dt><dd>{{ evaluation.releaseKey || "none" }}</dd></div>
      </dl>

      <p v-if="loading" class="muted">Loading feature value...</p>
      <p v-if="error" class="error">{{ error }}</p>
    </section>
  </main>
</template>
