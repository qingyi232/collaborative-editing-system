<template>
  <div class="monitor-page">
    <div class="monitor-header">
      <h2 class="page-title">资源监控</h2>
      <div class="refresh-info">
        <el-icon class="spin-icon" :class="{ spinning: refreshing }"><Refresh /></el-icon>
        <span class="refresh-text">每 {{ refreshInterval }} 秒自动刷新</span>
      </div>
    </div>

    <div v-loading="loading" class="monitor-grid">
      <div class="monitor-card card-memory">
        <div class="card-header">
          <div class="card-icon memory-icon">
            <el-icon :size="24"><Coin /></el-icon>
          </div>
          <div class="card-title">
            <span class="label">JVM 内存使用</span>
            <span class="value">{{ memoryUsed }} / {{ memoryMax }} MB</span>
          </div>
        </div>
        <div class="progress-wrapper">
          <el-progress
            :percentage="memoryPercent"
            :color="getProgressColor(memoryPercent)"
            :stroke-width="12"
            :show-text="false"
          />
          <span class="progress-label">{{ memoryPercent }}%</span>
        </div>
      </div>

      <div class="monitor-card card-cpu">
        <div class="card-header">
          <div class="card-icon cpu-icon">
            <el-icon :size="24"><Cpu /></el-icon>
          </div>
          <div class="card-title">
            <span class="label">CPU 核心数</span>
            <span class="value value-large">{{ monitorData.cpuCores || 0 }}</span>
          </div>
        </div>
        <div class="card-footer-text">处理器核心</div>
      </div>

      <div class="monitor-card card-threads">
        <div class="card-header">
          <div class="card-icon thread-icon">
            <el-icon :size="24"><Connection /></el-icon>
          </div>
          <div class="card-title">
            <span class="label">活跃线程数</span>
            <span class="value value-large">{{ monitorData.activeThreads || 0 }}</span>
          </div>
        </div>
        <div class="card-footer-text">当前活跃</div>
      </div>

      <div class="monitor-card card-connections">
        <div class="card-header">
          <div class="card-icon conn-icon">
            <el-icon :size="24"><User /></el-icon>
          </div>
          <div class="card-title">
            <span class="label">在线连接数</span>
            <span class="value value-large">{{ monitorData.onlineConnections || 0 }}</span>
          </div>
        </div>
        <div class="card-footer-text">WebSocket 连接</div>
      </div>
    </div>

    <div class="chart-section">
      <div class="chart-card">
        <h3 class="chart-title">JVM 内存使用趋势</h3>
        <v-chart class="chart" :option="memoryChartOption" autoresize />
      </div>
      <div class="chart-card">
        <h3 class="chart-title">线程 & 连接数趋势</h3>
        <v-chart class="chart" :option="threadChartOption" autoresize />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, shallowRef } from 'vue'
import { getMonitor, getConfigs } from '@/api/admin'
import { Coin, Cpu, Connection, User, Refresh } from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent
} from 'echarts/components'
import VChart from 'vue-echarts'

use([CanvasRenderer, LineChart, GridComponent, TooltipComponent, LegendComponent])

const MAX_HISTORY = 30

const loading = ref(false)
const refreshing = ref(false)
const monitorData = ref({})
const refreshInterval = ref(10)
let timer = null

const historyTime = ref([])
const historyMemory = ref([])
const historyThreads = ref([])
const historyConnections = ref([])

const memoryUsed = computed(() =>
  Math.round((monitorData.value.jvmMemoryUsed || 0) / 1024 / 1024)
)

const memoryMax = computed(() =>
  Math.round((monitorData.value.jvmMemoryMax || 1) / 1024 / 1024)
)

const memoryPercent = computed(() => {
  if (!monitorData.value.jvmMemoryMax || monitorData.value.jvmMemoryMax <= 0) return 0
  return Math.round((monitorData.value.jvmMemoryUsed / monitorData.value.jvmMemoryMax) * 100)
})

const getProgressColor = (percent) => {
  if (percent < 60) return '#2DB87F'
  if (percent < 80) return '#F0B429'
  return '#E5524A'
}

const chartBase = {
  grid: { top: 40, right: 24, bottom: 32, left: 52 },
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(255,255,255,0.96)',
    borderColor: '#e8e8e8',
    textStyle: { color: '#333', fontSize: 12 }
  }
}

const memoryChartOption = computed(() => ({
  ...chartBase,
  tooltip: {
    ...chartBase.tooltip,
    formatter: (params) => {
      const p = params[0]
      return `<b>${p.axisValue}</b><br/>${p.marker} ${p.seriesName}: <b>${p.value} MB</b>`
    }
  },
  legend: { data: ['内存使用'], top: 8, textStyle: { fontSize: 12, color: '#666' } },
  xAxis: {
    type: 'category',
    data: historyTime.value,
    axisLabel: { fontSize: 11, color: '#999' },
    axisLine: { lineStyle: { color: '#e8e8e8' } },
    boundaryGap: false
  },
  yAxis: {
    type: 'value',
    name: 'MB',
    nameTextStyle: { fontSize: 11, color: '#999' },
    axisLabel: { fontSize: 11, color: '#999' },
    splitLine: { lineStyle: { color: '#f0f0f0' } },
    max: memoryMax.value > 0 ? memoryMax.value : undefined
  },
  series: [{
    name: '内存使用',
    type: 'line',
    data: historyMemory.value,
    smooth: true,
    symbol: 'circle',
    symbolSize: 4,
    lineStyle: { width: 2.5, color: '#2DB87F' },
    itemStyle: { color: '#2DB87F' },
    areaStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(45,184,127,0.25)' },
          { offset: 1, color: 'rgba(45,184,127,0.02)' }
        ]
      }
    }
  }]
}))

const threadChartOption = computed(() => ({
  ...chartBase,
  legend: {
    data: ['活跃线程', 'WebSocket 连接'],
    top: 8,
    textStyle: { fontSize: 12, color: '#666' }
  },
  xAxis: {
    type: 'category',
    data: historyTime.value,
    axisLabel: { fontSize: 11, color: '#999' },
    axisLine: { lineStyle: { color: '#e8e8e8' } },
    boundaryGap: false
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    axisLabel: { fontSize: 11, color: '#999' },
    splitLine: { lineStyle: { color: '#f0f0f0' } }
  },
  series: [
    {
      name: '活跃线程',
      type: 'line',
      data: historyThreads.value,
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      lineStyle: { width: 2.5, color: '#F0B429' },
      itemStyle: { color: '#F0B429' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(240,180,41,0.2)' },
            { offset: 1, color: 'rgba(240,180,41,0.02)' }
          ]
        }
      }
    },
    {
      name: 'WebSocket 连接',
      type: 'line',
      data: historyConnections.value,
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      lineStyle: { width: 2.5, color: '#E5524A' },
      itemStyle: { color: '#E5524A' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(229,82,74,0.15)' },
            { offset: 1, color: 'rgba(229,82,74,0.02)' }
          ]
        }
      }
    }
  ]
}))

const pushHistory = (data) => {
  const now = new Date()
  const label = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
  const usedMB = Math.round((data.jvmMemoryUsed || 0) / 1024 / 1024)

  historyTime.value.push(label)
  historyMemory.value.push(usedMB)
  historyThreads.value.push(data.activeThreads || 0)
  historyConnections.value.push(data.onlineConnections || 0)

  if (historyTime.value.length > MAX_HISTORY) {
    historyTime.value.shift()
    historyMemory.value.shift()
    historyThreads.value.shift()
    historyConnections.value.shift()
  }
}

const fetchMonitor = async () => {
  refreshing.value = true
  try {
    const res = await getMonitor()
    const data = res.data || {}
    monitorData.value = data
    pushHistory(data)
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const configRes = await getConfigs()
    const configs = configRes.data || []
    const intervalConfig = configs.find(c => c.configKey === 'websocket_heartbeat_interval')
    if (intervalConfig && Number(intervalConfig.configValue) > 0) {
      refreshInterval.value = Number(intervalConfig.configValue)
    }
  } catch (e) { /* fallback to default 10s */ }
  fetchMonitor()
  timer = setInterval(fetchMonitor, refreshInterval.value * 1000)
})

onBeforeUnmount(() => {
  clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.monitor-page {
  max-width: 960px;
  margin: 0 auto;
}

.monitor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
}

.refresh-info {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.spin-icon {
  transition: transform 0.3s;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.monitor-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.monitor-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid var(--border-color);
  transition: box-shadow 0.25s;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  }
}

.card-header {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 16px;
}

.card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.memory-icon {
  background: rgba(45, 184, 127, 0.1);
  color: #2DB87F;
}

.cpu-icon {
  background: rgba(54, 181, 160, 0.1);
  color: #36B5A0;
}

.thread-icon {
  background: rgba(240, 180, 41, 0.1);
  color: #F0B429;
}

.conn-icon {
  background: rgba(229, 82, 74, 0.1);
  color: #E5524A;
}

.card-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.label {
  font-size: 13px;
  color: var(--text-secondary);
}

.value {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.value-large {
  font-size: 28px;
  line-height: 1.2;
}

.progress-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;

  .el-progress {
    flex: 1;
  }
}

.progress-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  min-width: 40px;
  text-align: right;
}

.card-footer-text {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: -4px;
}

.chart-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid var(--border-color);
  transition: box-shadow 0.25s;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  }
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.chart {
  width: 100%;
  height: 260px;
}

@media (max-width: 768px) {
  .monitor-grid,
  .chart-section {
    grid-template-columns: 1fr;
  }
}
</style>
