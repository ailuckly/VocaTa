<template>
  <section class="creator-section" data-test="creator-persona">
    <div class="creator-section__header">
      <div>
        <h2>提示词与陪伴口吻</h2>
        <p>先写基础设定，再让 AI 帮你补完 persona。</p>
      </div>
      <button type="button" :disabled="isGenerating" @click="$emit('generate')">
        {{ isGenerating ? '生成中...' : 'AI 生成' }}
      </button>
    </div>
    <textarea :value="persona" rows="6" class="form-textarea" placeholder="你的角色提示词" @input="$emit('update:persona', ($event.target as HTMLTextAreaElement).value)"></textarea>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  persona: string
  isGenerating: boolean
}>()

defineEmits<{
  generate: []
  'update:persona': [value: string]
}>()
</script>

<style scoped lang="scss">
.creator-section {
  display: grid;
  gap: 16px;
  padding: 24px;
  border-radius: var(--vt-radius-xl);
  background: var(--vt-surface);
  box-shadow: var(--vt-shadow);
}

.creator-section__header {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}

.creator-section__header h2,
.creator-section__header p {
  margin: 0;
}

.creator-section__header p {
  color: var(--vt-text-soft);
}

.creator-section__header button {
  border: 0;
  border-radius: 999px;
  padding: 10px 14px;
  background: var(--vt-brand);
  color: white;
  cursor: pointer;
}

.form-textarea {
  width: 100%;
  border: 1px solid var(--vt-line);
  border-radius: 16px;
  padding: 14px 16px;
  background: color-mix(in srgb, var(--vt-surface) 82%, var(--vt-brand) 4%);
}
</style>
