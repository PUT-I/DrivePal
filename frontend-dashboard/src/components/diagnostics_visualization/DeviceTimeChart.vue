<template>
  <div>

    <div class="chart-generator">
      <b-form-group
          class="text-left"
          label="Start date"
          label-for="start-datetime">
        <datetime id="start-datetime"
                  v-model="dateRange.startTimestamp"
                  placeholder="Select start date"
                  type="datetime"/>
      </b-form-group>

      <div class="m-4"/>

      <b-form-group
          class="text-left"
          label="End date"
          label-for="end-datetime">
        <datetime id="end-datetime"
                  v-model="dateRange.endTimestamp"
                  placeholder="Select end date"
                  type="datetime"/>
      </b-form-group>

      <div class="m-4"/>

      <b-form-group
          class="text-left"
          label="Device id"
          label-for="device-select">
        <b-form-select id="device-select"
                       v-model="selectedDevice"
                       :options="availableDevices"
                       placeholder="Select device model"/>
      </b-form-group>

      <div class="m-4"/>

      <b-form-group
          class="text-left"
          label="Model"
          label-for="model-select">
        <b-form-select id="model-select"
                       v-model="selectedModel"
                       :options="modelTypes"
                       placeholder="Select device model"/>
      </b-form-group>

      <div class="m-4"/>

      <b-button class="btn shadow-sm" @click="validateDateRange">Generate</b-button>
    </div>

    <div id="time-chart-container" style="height: 0; opacity: 0">
      <vue-frappe
          v-if="renderChart"
          id="chart"
          :height="600"
          type="line"
          :colors="['blue']"
          :dataSets="chartData"
          :labels="chartLabels"
          :tooltipOptions="tooltipOptions"
          title="Time [ms]">
      </vue-frappe>
    </div>
  </div>
</template>

<script lang="ts">
import DiagnosticDataService, {DiagnosticData} from "@/scripts/services/DiagnosticDataService";
import Utils from "@/scripts/Utils";
import * as $ from 'jquery';
import Vue from "vue";
import Component from "vue-class-component";

@Component
class DeviceTimeChart extends Vue {
  renderChart: boolean = false;

  diagnosticData: DiagnosticData[] = [];

  chartLabels: string[] = [];

  chartData: any[] = [{
    name: '',
    values: [],
  }];

  dateRange: any = {
    startTimestamp: new Date(Date.now() - 86400000).toUTCString(),
    endTimestamp: new Date().toUTCString(),
    isValid: false,
  };

  modelTypes: string[] = [];

  selectedModel: string = "";

  availableDevices: string[] = [];

  selectedDevice: string = "";

  // noinspection JSUnusedGlobalSymbols
  tooltipOptions: any = {
    formatTooltipX: (d: string) => d,
    formatTooltipY: (d: string) => `${d} ms`
  };

  static createEmptyLabels(dateStrings: string[]): string[] {
    let result: string[] = [];
    dateStrings.forEach(function (dateStr) {
      result.push(dateStr.split('.')[0]);
    });

    return result;
  }

  async mounted() {
    this.modelTypes = (await DiagnosticDataService.getModels()).data;
    if (this.modelTypes.length > 0) {
      this.selectedModel = this.modelTypes[0];
    }

    this.availableDevices = (await DiagnosticDataService.getDevices()).data;
    if (this.availableDevices.length > 0) {
      this.selectedDevice = this.availableDevices[0];
    }
  }

  async setChart(): Promise<void> {
    const chart = $("#time-chart-container");

    if (this.renderChart) {
      await chart.animate({"duration": 200, "opacity": "0"}).promise();
      await chart.animate({"duration": 200, "height": "0"}).promise();
    }

    this.renderChart = false;

    const startDate = new Date(Date.parse(this.dateRange.startTimestamp)).toISOString();
    const endDate = new Date(Date.parse(this.dateRange.endTimestamp)).toISOString();

    const request =
        DiagnosticDataService.getDiagnosticData(startDate, endDate, this.selectedModel, this.selectedDevice);
    this.diagnosticData = (await request).data;

    console.log("Received diagnostic data");
    console.log(this.diagnosticData);

    if (this.diagnosticData.length === 0) {
      this.$notify({
        group: "messages",
        type: "warn",
        title: "Chart generation",
        text: "No data for chart generation"
      });
      return;
    }

    this.chartLabels = this.diagnosticData.map(data => Utils.formatIsoDate(new Date(data.timeStamp)));
    this.chartLabels = DeviceTimeChart.createEmptyLabels(this.chartLabels);

    this.chartData = [];

    this.chartData.push({
      name: 'Inference',
      values: this.diagnosticData.map(data => data.inferenceTime)
    });

    this.chartData.push({
      name: 'Processing',
      values: this.diagnosticData.map(data => data.processingTime)
    });

    console.log("Set upped chart data");
    console.log(this.chartData);


    this.renderChart = true;

    await chart.animate({"duration": 200, "height": "600px"}).promise();
    await chart.animate({"duration": 200, "opacity": "100%"}).promise();
  }

  validateDateRange(): void {
    if (Date.parse(this.dateRange.startTimestamp) < Date.parse(this.dateRange.endTimestamp)) {
      this.dateRange.isValid = true;
    } else {
      alert("Invalid dates");
      this.dateRange.isValid = false;
    }

    this.setChart();
  }
}

export default DeviceTimeChart;
</script>

<style scoped>
.chart-generator {
  display: flex;
  align-items: center;
}
</style>
