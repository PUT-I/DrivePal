<template>
  <div>
    <b-form class="shadow-sm text-left text-bold p-2"><b>Detection verification</b></b-form>

    <b-modal id="verification-modal"
             ok-title="Save"
             size="xl"
             title="Verify detections"
             @ok="saveModal"
             @shown="prepareModal">
      <div style="text-align: center;">

        <div style="position: relative;">
          <div id="modal-img-overlay" style="width: 640px; height: 640px; position: absolute; right: 233px;">
          </div>

          <img id="modal-img"
               alt="No image"
               class="shadow"
               src=""
               style="width: 640px; height: 640px;">
        </div>

        <div style="height: 30px;"/>

        <p id="modal-detection-label"><b>Person</b></p>
        <button class="btn btn-info mr-1 shadow-sm" style="width: 90px;" @click="previousBoundingBox">Previous
        </button>
        <button class="btn btn-info mr-1 shadow-sm" style="width: 90px;" @click="nextBoundingBox">Next</button>

        <div style="height: 10px;"/>
        <p id="modal-detection-number"></p>

        <button class="btn btn-success mr-1 shadow-sm" style="width: 90px;" @click="validateDetection">Valid</button>
        <button class="btn btn-danger mr-1 shadow-sm" style="width: 90px;" @click="invalidateDetection">Invalid
        </button>
      </div>
    </b-modal>

    <div id="container" class="p-3">
      <b-form class="shadow rounded bg-white p-3">
        <b-pagination
            v-model="currentPage"
            :per-page="perPage"
            :total-rows="detections.length"
            aria-controls="detections-table"/>

        <b-table
            id="detections-table"
            class="shadow-sm"
            :current-page="currentPage"
            :fields="detection_fields"
            :items="detections"
            :per-page="perPage"
            bordered
            striped>

          <template #cell(entityUrl)="data">
            <div>
              <img :src="`${data.value}/image`" alt="No image" class="shadow"/>
            </div>
          </template>

          <template #cell(action)="data">

            <b-button v-b-modal.verification-modal
                      class="btn btn-info shadow-sm"
                      style="width: 100px;"
                      @click="sendRowInfo(data.item.id)">Verify
            </b-button>

            <div style="height: 10px;"/>

            <b-button class="btn btn-danger shadow-sm"
                      style="width: 100px;"
                      type="button"
                      @click="deleteButtonAction(data.item.id, $event)">Delete
            </b-button>
          </template>

        </b-table>
      </b-form>
    </div>


  </div>
</template>

<script>
import DetectionService from "../js/services/DetectionService";
import $ from 'jquery';
import Utils from "@/js/Utils";

export default {
  name: "sign-row",
  data() {
    return {
      currentPage: 1,
      perPage: 10,
      detections: [],
      detection_fields: [
        {key: "id", label: "Id", sortable: true},
        {key: "timestamp", label: "Timestamp", sortable: true},
        {key: "modelName", label: "Model Name", sortable: true},
        {key: "entityUrl", label: "Image", sortable: false},
        {key: "action", label: "Action", sortable: false}
      ]
    };
  },
  async mounted() {
    const tableContainer = $("#container");
    tableContainer.css("opacity", "0%");

    // eslint-disable-next-line no-unused-vars
    let response;
    try {
      response = await DetectionService.getAllDetections();
    } catch (error) {
      this.$notify({
        group: "messages",
        type: "error",
        title: "Download failed",
        text: "Failed to download detections"
      });
      console.error(error);
      return;
    }
    let data = response.data;

    console.log(JSON.stringify(data));

    data.forEach((part, index) => {
      const timestamp = part.timestamp;
      data[index].timestamp = Utils.formatIsoDate(new Date(timestamp));
    }, data);

    this.detections = data;

    this.$notify({
      group: "messages",
      type: "success",
      title: "Download success",
      text: "Detections downloaded successfully"
    });
    tableContainer.animate({"duration": 400, "opacity": "100%"});
  },
  methods: {
    async prepareModal() {
      const imgSrc = `http://server.drivepal.pl:5000/api/detection/${this.modalDetectionId}/image`;
      console.log(`Set modal image src to ${imgSrc}`);

      const imgOverlay = $("#modal-img-overlay");
      imgOverlay.hide().empty();

      $("#modal-img").hide()
          .attr("src", imgSrc)
          .show(400);

      const response = await DetectionService.getDetection(this.modalDetectionId);
      let data = response.data;

      this.currentDetections = data.detections;
      for (const index in this.currentDetections) {
        const detection = this.currentDetections[index];
        console.log(detection);

        // noinspection JSUnfilteredForInLoop,JSUnresolvedVariable
        const left = detection.locationLeft * 100.0;
        // noinspection JSUnfilteredForInLoop,JSUnresolvedVariable
        const top = detection.locationTop * 100.0;
        // noinspection JSUnfilteredForInLoop,JSUnresolvedVariable
        const right = 100.0 - detection.locationRight * 100.0;
        // noinspection JSUnfilteredForInLoop,JSUnresolvedVariable
        const bottom = 100.0 - detection.locationBottom * 100.0;

        const location = `left: ${left}%; top: ${top}%; right: ${right}%; bottom: ${bottom}%;`;

        let borderColor = "blue";
        if (detection.valid === true) {
          borderColor = "green";
        } else if (detection.valid === false) {
          borderColor = "red";
        }

        const boundingBox =
            `<div style="float:left; position: absolute; border: 3px solid ${borderColor}; ${location};"/>`;
        imgOverlay.append(boundingBox);
      }

      imgOverlay.css("opacity", "0%")
          .show(400)
          .animate({duration: 400, opacity: "100%"});

      $("#modal-detection-number").text(`Detection 1 of ${this.currentDetections.length}`);

      this.selectedBoundingBox = $(imgOverlay.children()[0]);
      this.selectedDetectionIndex = 0;
      $(this.selectedBoundingBox).css("border-width", "6px");

      const detection = this.currentDetections[this.selectedDetectionIndex];
      $("#modal-detection-label")
          .text(`${detection.className} (${Math.round(detection.confidence * 100)}%)`);
    },
    nextBoundingBox() {
      const bounding_boxes = $("#modal-img-overlay").children();

      if (bounding_boxes.length === 0) {
        return;
      }

      $(bounding_boxes[this.selectedDetectionIndex]).css("border-width", "3px");

      this.selectedDetectionIndex += 1;

      if (this.selectedDetectionIndex === bounding_boxes.length) {
        this.selectedDetectionIndex = 0;
      }

      this.selectedBoundingBox = $(bounding_boxes[this.selectedDetectionIndex]);
      this.selectedBoundingBox.css("border-width", "6px");
      $("#modal-detection-number").text(`Detection ${this.selectedDetectionIndex + 1} of ${bounding_boxes.length}`);

      const detection = this.currentDetections[this.selectedDetectionIndex];
      $("#modal-detection-label").text(`${detection.className} (${Math.round(detection.confidence * 100)}%)`);
    },
    previousBoundingBox() {
      const bounding_boxes = $("#modal-img-overlay").children();

      if (bounding_boxes.length === 1) {
        return;
      }

      $(bounding_boxes[this.selectedDetectionIndex]).css("border-width", "3px");

      this.selectedDetectionIndex -= 1;

      if (this.selectedDetectionIndex < 0) {
        this.selectedDetectionIndex = bounding_boxes.length - 1;
      }

      this.selectedBoundingBox = $(bounding_boxes[this.selectedDetectionIndex]);
      this.selectedBoundingBox.css("border-width", "6px");
      $("#modal-detection-number").text(`Detection ${this.selectedDetectionIndex + 1} of ${bounding_boxes.length}`);

      const detection = this.currentDetections[this.selectedDetectionIndex];
      $("#modal-detection-label").text(`${detection.className} (${Math.round(detection.confidence * 100)}%)`);
    },
    validateDetection() {
      this.selectedBoundingBox.css("border-color", "green");
      this.currentDetections[this.selectedDetectionIndex].valid = true;
    },
    invalidateDetection() {
      this.selectedBoundingBox.css("border-color", "red");
      this.currentDetections[this.selectedDetectionIndex].valid = false;
    },
    async saveModal() {
      console.log("Saving verification results");

      try {
        await DetectionService.saveVerification(this.currentDetections);
      } catch {
        this.$notify({
          group: "messages",
          type: "error",
          title: "Verification failed",
          text: "Failed to save verification"
        });
        return;
      }

      this.$notify({
        group: "messages",
        type: "success",
        title: "Verification saved",
        text: "Verification saved successfully"
      });
    },
    sendRowInfo(id) {
      console.log(id);
      this.modalDetectionId = id;
      console.log(`Changed modal image id to : ${id}`);
    },
    deleteButtonAction(id, event) {
      DetectionService.deleteDetectionImage(id);
      this.$notify({
        group: "messages",
        type: "success",
        title: "Delete success",
        text: `Detection (id ${id}) deleted successfully`
      });
      $(event.target).parent().parent().children('td')
          .animate({padding: 0})
          .wrapInner('<div />')
          .children()
          .slideUp(() => {
            $(this).closest('tr').remove();
          });
    }
  }
}
;
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h3 {
  margin: 40px 0 0;
}

ul {
  list-style-type: none;
  padding: 0;
}

li {
  display: inline-block;
  margin: 0 10px;
}

a {
  color: #42b983;
}

img {
  width: 100px;
  height: 100px;
}
</style>
