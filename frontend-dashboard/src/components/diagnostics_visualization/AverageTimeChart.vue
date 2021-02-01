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

<script lang="ts">
import DiagnosticDataService, {AverageTimes} from "@/scripts/services/DiagnosticDataService";
import Component from "vue-class-component";
import Vue from "vue";

@Component
class AverageTimeChart extends Vue {
  renderChart: boolean = false;

  chartTitle: string = "";

  chartLabels: string[] = [];

  chartData: any[] = [];

  modelNames: string[] = [];

  averageInferenceTimes: number[]

  averageProcessingTimes: number[]

  availableSoc: string[] = ["All"];

  selectedSoc: string = "All";

  // noinspection JSUnusedGlobalSymbols
  tooltipOptions: any = {
    formatTooltipX: (d: string) => `${d}`,
    formatTooltipY: (d: string) => `${d} ms`
  }

  async mounted(): Promise<void> {
    this.chartTitle = 'Time [ms]';

    const socs = (await DiagnosticDataService.getSocs()).data;
    this.availableSoc = this.availableSoc.concat(socs);

    await this.getDataAndSetChart();
  }

  async getDataAndSetChart(): Promise<void> {
    await this.getDiagnosticData();
    this.setChart();
  }

  async getDiagnosticData(): Promise<void> {
    const data = (await DiagnosticDataService.getAverage(this.selectedSoc)).data;
    console.log("Diagnostics downloaded");
    console.log(data);

    try {
      this.modelNames = data.map((value: AverageTimes) => value.modelName);
      this.averageInferenceTimes = data.map((value: AverageTimes) => value.averageInferenceTime);
      this.averageProcessingTimes = data.map((value: AverageTimes) => value.averageProcessingTime);

      console.log("Data transformed");
      console.log(this.modelNames);
      console.log(this.averageInferenceTimes);
      console.log(this.averageProcessingTimes);

    } catch (error) {
      console.log(error);
    }
  }

  setChart(): void {
    this.renderChart = false;

    this.chartLabels = this.modelNames;

    this.chartData = [];
    this.chartData.push({
      name: 'Inference Time',
      values: this.averageInferenceTimes,
    });

    this.chartData.push({
      name: 'Processing Time',
      values: this.averageProcessingTimes,
    });

    this.renderChart = true;
  }
}

export default AverageTimeChart;
</script>

<style scoped>
</style>
