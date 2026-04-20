<template>
  <section class="creator-section">
    <h2>陪伴方式</h2>
    <div class="creator-section__row">
      <el-select :model-value="voiceId" placeholder="请选择角色声音" @update:model-value="$emit('update:voiceId', String($event))">
        <el-option v-for="item in options" :key="item.name" :label="item.name" :value="item.name" />
      </el-select>
      <el-checkbox :model-value="isPublic" label="公开这个角色" @update:model-value="$emit('update:isPublic', Boolean($event))" />
    </div>
  </section>
</template>

<script setup lang="ts">
import type { TtsVoiceOption } from '@/types/api'

defineProps<{
  voiceId: string
  isPublic: boolean
  options: TtsVoiceOption[]
}>()

defineEmits<{
  'update:voiceId': [value: string]
  'update:isPublic': [value: boolean]
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

.creator-section__row {
  display: grid;
  gap: 14px;
}
</style>
