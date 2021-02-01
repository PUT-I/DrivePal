<template>
  <div v-if="renderChart" :id="`validity-chart-${chartIndex}`">
    <vue-frappe
        :id="`validity-chart-${chartIndex}`"
        :dataSets="chartData"
        :height="300"
        :labels="chartLabels"
        :title="chartTitle"
        :tooltipOptions="tooltipOptions"
        type="pie">
    </vue-frappe>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import {DetectionValidity} from "@/scripts/services/DetectionService";


@Component
class AverageDetectionValidityChart extends Vue {
  @Prop()
  detectionValidity: DetectionValidity;

  @Prop()
  chartIndex: number;

  renderChart: boolean = false;

  chartTitle: string = "";

  chartLabels: string[] = [];

  chartData: any[] = [];

  // noinspection JSUnusedGlobalSymbols
  tooltipOptions: any = {
    formatTooltipX: (d: string) => `${(parseFloat(d) * 100).toFixed(1)}%`,
    formatTooltipY: (d: string) => `${(parseFloat(d) * 100).toFixed(1)}%`
  }

  async mounted(): Promise<void> {
    console.log("Creating AverageDetectionValidityChart");
    console.log(this.chartIndex);
    console.log(this.detectionValidity);

    this.setChart();
  }

  setChart(): void {
    this.chartTitle = `${this.detectionValidity.modelName} validity`;


    this.chartLabels = Object.keys(this.detectionValidity);

    this.chartData.push({
      name: this.detectionValidity.modelName,
      values: [
        this.detectionValidity.valid,
        this.detectionValidity.invalid,
        this.detectionValidity.unverified
      ],
    });

    this.renderChart = true;
  }
}

export default AverageDetectionValidityChart;
</script>

<style scoped>
</style>
