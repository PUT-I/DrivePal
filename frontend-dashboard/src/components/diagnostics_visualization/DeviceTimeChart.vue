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

<script>
import $ from "jquery";
import DiagnosticDataService from "@/js/services/DiagnosticDataService";
import Utils from "@/js/Utils";

export default {
  name: "DeviceTimeChart",
  data() {
    return {
      renderChart: false,
      diagnosticData: null,
      chartLabels: null,
      chartData: [{
        name: '',
        values: [],
      }],
      dateRange: {
        startTimestamp: new Date(Date.now() - 86400000).toUTCString(),
        endTimestamp: new Date().toUTCString(),
        isValid: false,
      },
      modelTypes: null,
      selectedModel: null,
      availableDevices: null,
      selectedDevice: null,
      tooltipOptions: {
        formatTooltipX: (d) => d,
        formatTooltipY: (d) => `${d} ms`,
      }
    };
  },
  async mounted() {
    this.modelTypes = (await DiagnosticDataService.getModels()).data;
    if (this.modelTypes.length > 0) {
      this.selectedModel = this.modelTypes[0];
    }

    this.availableDevices = (await DiagnosticDataService.getDevices()).data;
    if (this.availableDevices.length > 0) {
      this.selectedDevice = this.availableDevices[0];
    }
  },
  methods: {
    async getDiagnosticData() {
      try {
        const response = await DiagnosticDataService.getDiagnosticData();
        this.diagnosticData = response.data;
        console.log("Diagnostics download");
        console.log(this.diagnosticData);
      } catch (error) {
        console.log(error);
      }
    },
    async setChart() {
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
      this.chartLabels = this.createEmptyLabels(this.chartLabels);

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
    },
    validateDateRange() {
      if (Date.parse(this.dateRange.startTimestamp) < Date.parse(this.dateRange.endTimestamp)) {
        this.dateRange.isValid = true;
      } else {
        alert("Invalid dates");
        this.dateRange.isValid = false;
      }

      this.setChart();
    },
    createEmptyLabels(dateStrings) {
      let result = [];
      dateStrings.forEach(function (dateStr) {
        result.push(dateStr.split('.')[0]);
      });
      return result;
    }
  }
};
</script>

<style scoped>
.chart-generator {
  display: flex;
  align-items: center;
}
</style>
