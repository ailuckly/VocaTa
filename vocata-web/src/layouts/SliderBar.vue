<template>
  <AppSidebar :collapsed="Boolean(sidebarCollapsed)" @toggle="$emit('toggleSidebar')" />
</template>

<script setup lang="ts">
import AppSidebar from '@/components/shell/AppSidebar.vue'

defineProps<{
  sidebarCollapsed: boolean
}>()

defineEmits<{
  toggleSidebar: []
}>()
</script>
