<template>
  <div class="p-3" style="min-height: 300px">
    <div>
      <b-form-group
          class="text-left"
          label="Select SOC"
          label-for="soc-select">

        <b-form-select v-model="selectedSoc"
                       :options="availableSoc"
                       class="soc-select"
                       @change="getDataAndSetChart"/>
      </b-form-group>
    </div>

    <vue-frappe
        v-if="renderChart"
        id="chart"
        :colors="['blue']"
        :dataSets="chartData"
        :height="300"
        :labels="chartLabels"
        :title="chartTitle"
        :tooltipOptions="tooltipOptions"
        type="bar">
    </vue-frappe>
  </div>
</template>

<script>
import DiagnosticDataService from "@/js/services/DiagnosticDataService";

export default {
  name: "AverageTimeChart",
  data() {
    return {
      renderChart: false,
      chartTitle: null,
      chartLabels: null,
      chartData: [],
      modelNames: [],
      averageInferenceTimes: null,
      averageProcessingTimes: null,
      availableSoc: ["All"],
      selectedSoc: "All",
      tooltipOptions: {
        formatTooltipX: (d) => `${d}`,
        formatTooltipY: (d) => `${d} ms`,
      }
    };
  },
  async mounted() {
    this.chartTitle = 'Time [ms]';

    const socs = (await DiagnosticDataService.getSocs()).data;
    this.availableSoc = this.availableSoc.concat(socs);

    await this.getDataAndSetChart();
  },
  methods: {
    async getDataAndSetChart() {
      await this.getDiagnosticData();
      this.setChart();
    },
    async getDiagnosticData() {
      const response = (await DiagnosticDataService.getAverage(this.selectedSoc)).data;
      console.log(response);

      try {
        this.modelNames = response.modelNames;
        this.averageInferenceTimes = response.averageInferenceTimes;
        this.averageProcessingTimes = response.averageProcessingTimes;
        console.log("Diagnostics download");
        console.log(response);
      } catch (error) {
        console.log(error);
      }
    },
    setChart() {
      this.renderChart = false;

      this.chartLabels = Object.keys(this.averageInferenceTimes);

      this.chartData = [];
      this.chartData.push({
        name: 'Inference Time',
        values: Object.values(this.averageInferenceTimes),
      });

      this.chartData.push({
        name: 'Processing Time',
        values: Object.values(this.averageProcessingTimes),
      });

      this.renderChart = true;
    }
  }
};
</script>

<style scoped>
</style>
