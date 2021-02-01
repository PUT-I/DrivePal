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
            ref="table"
            :current-page="currentPage"
            :fields="dictionaryFields"
            :per-page="perPage"
            :items="dictionary"
            bordered
            striped>
          <template #cell(action)="data">
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

<script lang="ts">
import SocDictionaryService, {SocDictionaryEntry} from "@/scripts/services/SocDictionaryService";
import * as $ from 'jquery';
import Vue from 'vue';
import Component from 'vue-class-component';

@Component
class SocDictionary extends Vue {
  code: string = "";

  dictName: string = "";

  currentPage: number = 1;

  perPage: number = 8;

  dictionary: SocDictionaryEntry[] = [];

  dictionaryFields = [
    {key: "code", label: "Code", sortable: true},
    {key: "name", label: "Name", sortable: true},
    {key: "action", label: "Action", sortable: false}
  ];

  async mounted(): Promise<void> {
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
  }


  async getSocDictionary(): Promise<SocDictionaryEntry[]> {
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
      return [];
    }
    return response.data;
  }

  async handleSubmit(): Promise<void> {
    const filtered = this.dictionary.filter((elem: SocDictionaryEntry) => elem.code === this.code);
    if (filtered.length <= 0) {
      const newEntry = (await SocDictionaryService.saveEntity(this.code, this.dictName)).data;
      this.dictionary.push(newEntry);
    } else {
      const newEntry = (await SocDictionaryService.updateEntity(this.code, this.dictName)).data;
      this.dictionary = this.dictionary.filter((elem: SocDictionaryEntry) => elem.code !== this.code);
      this.dictionary.push(newEntry);
    }
  }

  async deleteButtonAction(id: number, event: any): Promise<void> {
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
    await $(event.target)
        .parent()
        .parent()
        .children('td')
        .animate({padding: 0})
        .wrapInner('<div />')
        .children()
        .slideUp()
        .promise();
  }
}

export default SocDictionary;
</script>

<style scoped>

</style>
