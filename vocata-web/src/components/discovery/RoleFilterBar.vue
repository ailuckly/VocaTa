<template>
  <section class="role-filter-bar" data-test="role-filter-bar">
    <div class="role-filter-bar__search">
      <input
        :value="keyword"
        type="text"
        placeholder="搜索角色"
        @input="$emit('update:keyword', ($event.target as HTMLInputElement).value)"
        @keyup.enter="$emit('search')"
      />
    </div>
    <div class="role-filter-bar__tabs">
      <button type="button" :class="{ 'is-active': currentView === 'my' }" @click="$emit('changeView', 'my')">我的</button>
      <button type="button" :class="{ 'is-active': currentView === 'public' && orderDirection === 'desc' }" @click="$emit('changeView', 'hot')">热门</button>
      <button type="button" :class="{ 'is-active': currentView === 'public' && orderDirection === 'asc' }" @click="$emit('changeView', 'latest')">最新</button>
    </div>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  keyword: string
  currentView: string
  orderDirection?: string
}>()

defineEmits<{
  search: []
  changeView: [mode: string]
  'update:keyword': [value: string]
}>()
</script>

<style scoped lang="scss">
.role-filter-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
}

.role-filter-bar__search {
  flex: 1;
}

.role-filter-bar__search input {
  width: 100%;
  border: 1px solid var(--vt-line);
  border-radius: 18px;
  padding: 14px 16px;
  background: var(--vt-surface);
  color: var(--vt-text);
  font-size: 15px;
}

.role-filter-bar__tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.role-filter-bar__tabs button {
  border: 0;
  border-radius: 999px;
  padding: 10px 14px;
  background: color-mix(in srgb, var(--vt-surface) 80%, var(--vt-brand) 8%);
  color: var(--vt-text-soft);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.role-filter-bar__tabs button.is-active {
  background: var(--vt-brand);
  color: white;
}

@media (max-width: 960px) {
  .role-filter-bar {
    grid-template-columns: 1fr;
  }
}
</style>
