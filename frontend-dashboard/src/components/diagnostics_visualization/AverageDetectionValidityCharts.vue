<template>
  <div>
    <div v-if="renderGraphs">
      <template v-for="(detectionValidity, index) in detectionsValidity">
        <div :key="detectionValidity" class="shadow rounded bg-white p-3">
          <AverageDetectionValidityChart :key="detectionValidity"
                                         v-bind:chart-index="index"
                                         v-bind:detection-validity="detectionValidity"/>
        </div>
        <div :key="detectionValidity" class="m-4"></div>
      </template>
    </div>
  </div>
</template>

<script lang="ts">
import DetectionService, {DetectionValidity} from "@/scripts/services/DetectionService";
import AverageDetectionValidityChart from "@/components/diagnostics_visualization/AverageDetectionValidityChart";
import {Component, Vue} from 'vue-property-decorator';

@Component({
  components: {AverageDetectionValidityChart}
})
class AverageDetectionValidityCharts extends Vue {
  renderGraphs: boolean = false;

  detectionsValidity: DetectionValidity[] = [];

  async mounted(): Promise<void> {
    await this.getValidityData();
    this.createCharts();
  }

  async getValidityData(): Promise<void> {
    this.detectionsValidity = (await DetectionService.getDetectionsValidity()).data;
  }

  createCharts(): void {
    console.log("Invoked createCharts()");
    console.log(this.detectionsValidity);
    this.renderGraphs = true;
  }
}

export default AverageDetectionValidityCharts;
</script>

<style scoped>
</style>
