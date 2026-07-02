<template>
  <nav class="category-chips" aria-label="场景分类">
    <button
      v-for="category in categories"
      :key="category"
      type="button"
      :class="{ 'is-active': category === activeCategory, 'is-more': category === '更多' }"
      :data-test="`category-chip-${category}`"
      @click="$emit('select', category)"
    >
      {{ category }}
      <span v-if="category === '更多'" class="category-more-icon">&gt;</span>
    </button>
  </nav>
</template>

<script setup lang="ts">
defineProps<{
  categories: string[]
  activeCategory: string
}>()

defineEmits<{
  select: [category: string]
}>()


</script>

<style scoped lang="scss">
.category-chips {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  scrollbar-width: none;
  align-items: center;

  &::-webkit-scrollbar { display: none; }

  button {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 auto;
    height: 44px;
    padding: 0 20px;
    border-radius: 999px;
    border: 1px solid rgba(129, 140, 248, 0.22);
    background: rgba(255, 255, 255, 0.78);
    color: #68708f;
    font-weight: 600;
    white-space: nowrap;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.2s;

    &:hover:not(.is-active) {
      background: #ffffff;
      color: #333;
    }

    &.is-active {
      background: linear-gradient(135deg, #7c5cff, #6b47f5);
      color: #fff;
      border-color: transparent;
    }

    &.is-more {
      margin-left: auto;
      border: 0;
      background: transparent;
      padding-right: 0;
      color: #a0a5b5;
      
      &:hover {
        color: #7c5cff;
      }
    }
  }
  
  .category-more-icon {
    font-size: 12px;
    margin-left: 2px;
  }
}
</style>
