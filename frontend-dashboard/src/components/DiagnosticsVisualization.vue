<template>
  <div>
    <b-form class="shadow-sm text-left text-bold p-2"><b>Diagnostics visualization</b></b-form>

    <div id="main-container" class="p-3">
      <div class="shadow rounded bg-white p-3">
        <b-form-group
            class="text-left"
            label="Chart type"
            label-for="char-type-select">
          <b-form-select id="chart-type-select"
                         v-model="selectedChartType"
                         :options="chartTypes"
                         @change="changeChart()"/>
        </b-form-group>
      </div>

      <div class="m-4"></div>

      <div id="chart-container" style="opacity: 0">
        <div v-if="selectedChartTypeSwitch !== 'detectionValidity'"
             class="shadow rounded bg-white p-3">
          <AverageTimeChart v-if="selectedChartTypeSwitch==='avgTime'"/>
          <DeviceTimeChart v-if="selectedChartTypeSwitch==='deviceTime'"/>
        </div>
        <AverageDetectionValidityCharts v-if="selectedChartTypeSwitch==='detectionValidity'"/>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import AverageTimeChart from "@/components/diagnostics_visualization/AverageTimeChart";
import DeviceTimeChart from "@/components/diagnostics_visualization/DeviceTimeChart";
import AverageDetectionValidityCharts from "@/components/diagnostics_visualization/AverageDetectionValidityCharts";
import Utils from "@/scripts/Utils.ts";
import * as $ from 'jquery';
import Component from "vue-class-component";
import Vue from "vue";


@Component({
  components: {AverageDetectionValidityCharts, DeviceTimeChart, AverageTimeChart}
})
class DiagnosticsVisualization extends Vue {
  chartTypes: any = [
    {value: 'detectionValidity', text: 'Detection validity'},
    {value: 'avgTime', text: 'Total average times'},
    {value: 'deviceTime', text: 'Time for specific device'}
  ];

  selectedChartType: string = 'detectionValidity';

  selectedChartTypeSwitch: string = 'detectionValidity';

  mounted(): void {
    this.changeChart();
  }

  async changeChart(): Promise<void> {
    console.log(`Changing chart to : ${this.selectedChartType}`);

    const container = $("#chart-container");
    await container.animate({"duration": 200, "opacity": "0"}).promise();

    this.selectedChartTypeSwitch = this.selectedChartType;

    if (this.selectedChartTypeSwitch != null) {
      await Utils.sleep(100);
      await container.animate({"duration": 400, "opacity": "100%"});
    }
  }
}

export default DiagnosticsVisualization;
</script>
