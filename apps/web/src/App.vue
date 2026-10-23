<script setup lang="ts">
import { onMounted, ref } from 'vue'
type Dataset = { id:string; name:string; sql:string; dataSourceId:string }
const datasets = ref<Dataset[]>([])
const loading = ref(true)
const error = ref('')
onMounted(async () => {
  try {
    const response = await fetch('/api/v1/datasets')
    if (!response.ok) throw new Error(`HTTP ${response.status}`)
    datasets.value = await response.json()
  } catch (e) { error.value = e instanceof Error ? e.message : '加载失败' }
  finally { loading.value = false }
})
</script>

<template>
  <main>
    <header><div><span class="eyebrow">SMARTBI PLATFORM</span><h1>数据资产工作台</h1><p>连接数据源、定义 SQL 数据集，并安全预览分析结果。</p></div><button>新建数据集</button></header>
    <section class="stats"><article><strong>{{ datasets.length }}</strong><span>数据集</span></article><article><strong>500</strong><span>预览行上限</span></article><article><strong class="live">● 正常</strong><span>平台状态</span></article></section>
    <section class="panel"><div class="panel-title"><h2>SQL 数据集</h2><span>{{ loading ? '正在同步…' : `${datasets.length} 项` }}</span></div><p v-if="error" class="error">API 暂不可用：{{ error }}</p><div v-else-if="!loading && !datasets.length" class="empty"><div class="icon">⌁</div><h3>还没有数据集</h3><p>启动后端后，通过 API 创建第一个只读 SQL 数据集。</p></div><table v-else-if="datasets.length"><thead><tr><th>名称</th><th>SQL</th><th>数据源</th></tr></thead><tbody><tr v-for="item in datasets" :key="item.id"><td>{{ item.name }}</td><td><code>{{ item.sql }}</code></td><td>{{ item.dataSourceId }}</td></tr></tbody></table></section>
  </main>
</template>

