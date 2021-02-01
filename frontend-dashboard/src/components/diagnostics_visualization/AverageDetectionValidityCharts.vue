<template>
  <div>
    <div v-if="renderGraphs">
      <template v-for="(modelValidity, index) in modelsValidity">
        <div :key="modelValidity" class="shadow rounded bg-white p-3">
          <AverageDetectionValidityChart :key="modelValidity"
                                         v-bind:chart-index="index"
                                         v-bind:detection-validity="modelValidity.detectionValidity"
                                         v-bind:model-name="modelValidity.modelName"/>
        </div>
        <div :key="modelValidity" class="m-4"></div>
      </template>
    </div>
  </div>
</template>

<script lang="ts">
import DetectionService, {DetectionValidity} from "@/scripts/services/DetectionService";
import AverageDetectionValidityChart from "@/components/diagnostics_visualization/AverageDetectionValidityChart";
import {Component, Vue} from 'vue-property-decorator';

class ModelValidity {
  modelName: string
  detectionValidity: DetectionValidity
}

@Component({
  components: {AverageDetectionValidityChart}
})
class AverageDetectionValidityCharts extends Vue {
  renderGraphs: boolean = false;

  modelsValidity: ModelValidity[] = [];

  async mounted(): Promise<void> {
    await this.getValidityData();
    this.createCharts();
  }

  async getValidityData(): Promise<void> {
    let detectionsValidityObject = (await DetectionService.getDetectionsValidity()).data;
    // TODO: Change server response to be able remove Map type
    const detectionsValidity = new Map<string, DetectionValidity>(Object.entries(detectionsValidityObject));

    const modelsValidity: ModelValidity[] = [];
    detectionsValidity.forEach((detectionValidity: DetectionValidity, modelName: string) => {
      let modelValidity: ModelValidity = new ModelValidity();
      modelValidity.detectionValidity = detectionValidity;
      modelValidity.modelName = modelName;

      modelsValidity.push(modelValidity);
    });

    this.modelsValidity = modelsValidity;
  }

  createCharts(): void {
    console.log("Invoked createCharts()");
    console.log(this.modelsValidity);
    this.renderGraphs = true;
  }
}

export default AverageDetectionValidityCharts;
</script>

<style scoped>
</style>
