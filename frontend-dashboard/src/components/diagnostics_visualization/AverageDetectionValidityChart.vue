<template>
  <div v-if="this.renderChart" :id="`validity-chart-${this.chartIndex}`">
    <vue-frappe
        :id="`validity-chart-${this.chartIndex}`"
        :dataSets="chartData"
        :height="300"
        :labels="chartLabels"
        :title="chartTitle"
        :tooltipOptions="tooltipOptions"
        type="pie">
    </vue-frappe>
  </div>
</template>

<script>
export default {
  name: "AverageDetectionValidityChart",
  props: ["modelValidity", "chartIndex"],
  data() {
    return {
      renderChart: false,
      chartTitle: null,
      chartLabels: null,
      chartData: [],
      modelNames: [],
      tooltipOptions: {
        formatTooltipX: (d) => `${d}%`,
        formatTooltipY: (d) => `${d}%`,
      }
    };
  },
  async mounted() {
    console.log(this.modelValidity);

    this.setChart();
  },
  methods: {
    setChart() {
      this.chartTitle = `${this.modelValidity.modelName} validity`;

      delete this.modelValidity.modelName;

      this.chartLabels = Object.keys(this.modelValidity);

      this.chartData.push({
        name: this.modelName,
        values: Object.values(this.modelValidity),
      });

      this.renderChart = true;
    }
  }
};
</script>

<style scoped>
</style>
