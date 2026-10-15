<template>
  <div class="relative h-64 w-full">
    <Bar v-if="ready" :data="chartData" :options="options" />
  </div>
</template>

<script setup lang="ts">
import { Bar } from 'vue-chartjs'
import {
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Title,
  Tooltip,
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend)

const props = defineProps<{
  labels: string[]
  values: number[]
  label?: string
  color?: string
}>()

const ready = ref(false)
onMounted(() => {
  ready.value = true
})

const chartData = computed(() => ({
  labels: props.labels,
  datasets: [
    {
      label: props.label || 'Giá trị',
      data: props.values,
      backgroundColor: props.color || 'rgba(16, 185, 129, 0.75)',
      borderRadius: 8,
    },
  ],
}))

const options = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
  },
  scales: {
    y: { beginAtZero: true, ticks: { precision: 0 } },
  },
}
</script>
