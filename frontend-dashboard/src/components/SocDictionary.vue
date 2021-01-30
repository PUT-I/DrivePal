<template>
  <div>
    <b-form class="shadow-sm text-left text-bold p-2"><b>SOC Dictionary</b></b-form>

    <b-modal
        id="add-modal"
        hide-footer
        title="Add Dictionary Entry">

      <b-form @submit.prevent="handleSubmit">
        <b-form-group
            label="Code:"
            label-for="code-input">

          <b-form-input
              id="code-input"
              v-model="code"
              placeholder="Enter SOC code"
              required/>
        </b-form-group>

        <b-form-group
            label="Name:"
            label-for="name-input">

          <b-form-input
              id="name-input"
              v-model="dictName"
              placeholder="Enter name"
              required/>
        </b-form-group>

        <b-button type="submit" variant="primary">Submit</b-button>
      </b-form>
    </b-modal>

    <div id="container" class="p-3">
      <b-form class="shadow rounded bg-white p-3">
        <b-pagination
            v-model="currentPage"
            :per-page="perPage"
            :total-rows="dictionary.length"
            aria-controls="dictionary-table"/>

        <b-table
            id="dictionary-table"
            class="shadow-sm"
            :current-page="currentPage"
            :fields="dictionary_fields"
            :per-page="perPage"
            :items="dictionary"
            bordered
            striped>
          <template #cell(action)="data">
            <div style="height: 10px;"/>
            <b-button class="btn btn-danger shadow-sm" style="width: 100px;" type="button"
                      @click="deleteButtonAction(data.item.id, $event)">Delete
            </b-button>
          </template>
        </b-table>

        <b-button v-b-modal.add-modal class="btn btn-success shadow-sm">
          Add/Update
        </b-button>
      </b-form>
    </div>
  </div>
</template>

<script>
import $ from "jquery";
import SocDictionaryService from "@/js/services/SocDictionaryService";

export default {
  name: "SocDictionary",
  data() {
    return {
      code: "",
      dictName: "",
      currentPage: 1,
      perPage: 8,
      dictionary: [],
      dictionary_fields: [
        {key: "code", label: "Code", sortable: true},
        {key: "name", label: "Name", sortable: true},
        {key: "action", label: "Action", sortable: false}
      ]
    };
  },
  async mounted() {
    const tableContainer = $("#container");
    tableContainer.css("opacity", "0%");

    const data = await this.getSocDictionary();
    console.log(JSON.stringify(data));

    this.dictionary = data;

    this.$notify({
      group: "messages",
      type: "success",
      title: "Download success",
      text: "Dictionary downloaded successfully"
    });
    tableContainer.animate({"duration": 400, "opacity": "100%"});
  },
  methods: {
    async getSocDictionary() {
      let response;
      try {
        response = await SocDictionaryService.getAllEntities();
      } catch (error) {
        this.$notify({
          group: "messages",
          type: "error",
          title: "Download failed",
          text: "Failed to download dictionary"
        });
        console.error(error);
        return;
      }
      return response.data;
    },
    handleSubmit() {
      const filtered = this.dictionary.filter(elem => elem.code === this.code);
      if (filtered.length <= 0) {
        SocDictionaryService.saveEntity(this.code, this.dictName);
        this.dictionary.push({code: this.code, name: this.dictName});
      } else {
        SocDictionaryService.updateEntity(this.code, this.dictName);
        this.dictionary = this.dictionary.filter(elem => elem.code !== this.code);
        this.dictionary.push({code: this.code, name: this.dictName});
      }
    },
    async deleteButtonAction(id, event) {
      try {
        await SocDictionaryService.deleteEntity(id);
      } catch (error) {
        this.$notify({
          group: "messages",
          type: "error",
          title: "Delete failed",
          text: "Failed to delete dictionary entry"
        });
        console.error(error);
        return;
      }

      this.$notify({
        group: "messages",
        type: "success",
        title: "Delete success",
        text: `Dictionary entry (id ${id}) deleted successfully`
      });
      $(event.target)
          .parent()
          .parent()
          .children('td')
          .animate({padding: 0})
          .wrapInner('<div />')
          .children()
          .slideUp(() => {
            $(this).closest('tr').remove();
          });
    }
  }
};
</script>

<style scoped>

</style>
