<template>
  <div>
    <div v-if="this.renderGraphs">
      <template v-for="(modelValidity, index) in this.detectionsValidity">
        <div :key="modelValidity" class="shadow rounded bg-white p-3">
          <AverageDetectionValidityChart :key="modelValidity"
                                         v-bind:chart-index="index"
                                         v-bind:model-validity="modelValidity"/>
        </div>
        <div :key="modelValidity" class="m-4"></div>
      </template>
    </div>
  </div>
</template>

<script>
import DetectionService from "@/js/services/DetectionService";
import AverageDetectionValidityChart from "@/components/diagnostics_visualization/AverageDetectionValidityChart";

export default {
  name: "AverageDetectionValidityCharts",
  // eslint-disable-next-line vue/no-unused-components
  components: {AverageDetectionValidityChart},
  data() {
    return {
      renderGraphs: false,
      diagnosticData: null,
      chartTitle: null,
      chartLabels: null,
      chartData: [],
    };
  },
  async mounted() {
    await this.getDiagnosticData();
    this.createCharts();
  },
  methods: {
    async getDiagnosticData() {
      const response = await DetectionService.getDetectionsValidity();
      console.log(response.data);

      const detectionsValidity = [];
      Object.keys(response.data).forEach((modelName) => {
        const modelValidity = response.data[modelName];
        modelValidity.modelName = modelName;
        detectionsValidity.push(modelValidity);
      });

      console.log(detectionsValidity);
      this.detectionsValidity = detectionsValidity;
    },
    createCharts() {
      this.renderGraphs = true;
    }
  }
};
</script>

<style scoped>
</style>
