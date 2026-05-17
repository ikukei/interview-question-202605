<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";

const props = defineProps<{
  modelValue: string[];
  options: string[];
  placeholder?: string;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: string[]): void;
}>();

const open = ref(false);
const root = ref<HTMLElement | null>(null);

const label = computed(() => {
  if (!props.modelValue.length) return props.placeholder ?? "— none —";
  return props.modelValue.join(", ");
});

function toggle(option: string) {
  const next = props.modelValue.includes(option)
    ? props.modelValue.filter((v) => v !== option)
    : [...props.modelValue, option];
  emit("update:modelValue", next);
}

function handleOutsideClick(e: MouseEvent) {
  if (root.value && !root.value.contains(e.target as Node)) {
    open.value = false;
  }
}

onMounted(() => document.addEventListener("mousedown", handleOutsideClick));
onBeforeUnmount(() => document.removeEventListener("mousedown", handleOutsideClick));
</script>

<template>
  <div class="ms-root" ref="root">
    <!-- trigger: shows selected text, click to open -->
    <button
      type="button"
      class="ms-trigger"
      :class="{ 'ms-open': open }"
      @click="open = !open"
    >
      <span class="ms-label">{{ label }}</span>
      <span class="ms-caret">{{ open ? "▲" : "▼" }}</span>
    </button>

    <!-- dropdown -->
    <div v-if="open" class="ms-dropdown">
      <label
        v-for="opt in options"
        :key="opt"
        class="ms-option"
        :class="{ 'ms-checked': modelValue.includes(opt) }"
      >
        <input
          type="checkbox"
          :checked="modelValue.includes(opt)"
          @change="toggle(opt)"
        />
        {{ opt }}
      </label>
    </div>
  </div>
</template>

<style scoped>
.ms-root {
  position: relative;
  width: 100%;
}

.ms-trigger {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 7px 10px;
  border: 1px solid #cdd5df;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  text-align: left;
  gap: 8px;
}
.ms-trigger:hover { border-color: #206b82; }
.ms-trigger.ms-open { border-color: #206b82; box-shadow: 0 0 0 2px rgba(32,107,130,0.15); }

.ms-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: #1d2433;
}

.ms-caret {
  font-size: 10px;
  color: #9ca3af;
  flex-shrink: 0;
}

.ms-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  min-width: 100%;
  background: #fff;
  border: 1px solid #cdd5df;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(20,35,56,0.12);
  z-index: 200;
  padding: 4px 0;
  max-height: 220px;
  overflow-y: auto;
}

.ms-option {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  font-size: 14px;
  color: #1d2433;
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
}
.ms-option:hover { background: #f4f6f8; }
.ms-option.ms-checked { color: #206b82; font-weight: 500; }

.ms-option input[type="checkbox"] {
  width: 15px;
  height: 15px;
  margin: 0;
  flex-shrink: 0;
  accent-color: #206b82;
  cursor: pointer;
}
</style>
