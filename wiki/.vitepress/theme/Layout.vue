<script setup lang="ts">
import DefaultTheme from 'vitepress/theme'
import { useData } from 'vitepress'

const { lang } = useData()
</script>

<template>
  <DefaultTheme.Layout>
    <template #home-hero-info-before>
      <span class="wiki-preview">{{ lang === 'zh-CN' ? 'WIKI PREVIEW · 文档预览' : 'WIKI PREVIEW · BUILD SOMETHING NEW' }}</span>
    </template>
  </DefaultTheme.Layout>
</template>
