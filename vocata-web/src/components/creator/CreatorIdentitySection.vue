<template>
  <section class="creator-section" data-test="creator-identity">
    <h2>角色身份</h2>
    <div class="creator-section__grid">
      <el-upload
        class="avatar-uploader"
        :action="uploadAction"
        :headers="headers"
        :show-file-list="false"
        :on-success="onAvatarSuccess"
        :before-upload="beforeUpload"
      >
        <img v-if="imageUrl" :src="imageUrl" class="avatar" />
        <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
      </el-upload>

      <div class="creator-section__fields">
        <input :value="modelValue.name" placeholder="例如：张三" class="form-input" @input="updateField('name', $event)" />
        <textarea :value="modelValue.description" placeholder="你的角色信息" class="form-textarea" rows="4" @input="updateField('description', $event)"></textarea>
        <input :value="modelValue.greeting" placeholder="你好呀！" class="form-input" @input="updateField('greeting', $event)" />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue'
import type { UploadProps } from 'element-plus'
import type { CreateCharacterRequest } from '@/types/api'

const props = defineProps<{
  modelValue: CreateCharacterRequest
  imageUrl: string
  uploadAction: string
  headers: Record<string, string>
  beforeUpload: UploadProps['beforeUpload']
}>()

const emit = defineEmits<{
  'update:modelValue': [value: CreateCharacterRequest]
  'update:imageUrl': [value: string]
}>()

const updateField = (field: keyof CreateCharacterRequest, event: Event) => {
  const value = (event.target as HTMLInputElement | HTMLTextAreaElement).value
  emit('update:modelValue', {
    ...props.modelValue,
    [field]: value,
  })
}

const onAvatarSuccess: UploadProps['onSuccess'] = (response) => {
  const nextUrl = response.data.fileUrl
  emit('update:imageUrl', nextUrl)
  emit('update:modelValue', {
    ...props.modelValue,
    avatarUrl: nextUrl,
  })
}
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

.creator-section__grid {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 20px;
}

.creator-section__fields {
  display: grid;
  gap: 12px;
}

.form-input,
.form-textarea {
  width: 100%;
  border: 1px solid var(--vt-line);
  border-radius: 16px;
  padding: 14px 16px;
  background: color-mix(in srgb, var(--vt-surface) 82%, var(--vt-brand) 4%);
}

.avatar-uploader :deep(.el-upload) {
  display: grid;
  width: 120px;
  height: 120px;
  place-items: center;
  border-radius: 28px;
  border: 1px dashed var(--vt-line);
  overflow: hidden;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>
