<template>
  <div class="relative mx-auto h-64 w-full max-w-xs">
    <Doughnut v-if="ready" :data="chartData" :options="options" />
  </div>
</template>

<script setup lang="ts">
import { Doughnut } from 'vue-chartjs'
import { ArcElement, Chart as ChartJS, Legend, Tooltip } from 'chart.js'

ChartJS.register(ArcElement, Tooltip, Legend)

const props = defineProps<{
  labels: string[]
  values: number[]
  colors?: string[]
}>()

const ready = ref(false)
onMounted(() => {
  ready.value = true
})

const chartData = computed(() => ({
  labels: props.labels,
  datasets: [
    {
      data: props.values,
      backgroundColor: props.colors || [
        '#10B981',
        '#065F46',
        '#fbbf24',
        '#f43f5e',
        '#94a3b8',
      ],
      borderWidth: 0,
    },
  ],
}))

const options = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: 'bottom' as const },
  },
}
</script>
