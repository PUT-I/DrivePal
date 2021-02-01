<template>
  <div id="app">

    <b-navbar class="shadow-sm bg-primary ">
      <b-navbar-brand class="text-light">DrivePal</b-navbar-brand>

      <b-collapse id="nav-collapse" is-nav>
        <b-navbar-nav>
          <b-nav-item href="/detections/" link-classes="text-light">Detection verification</b-nav-item>

          <b-nav-item href="/diagnostics/" link-classes="text-light">Diagnostics visualization
          </b-nav-item>

          <b-nav-item href="/soc-dictionary/" link-classes="text-light">SOC Dictionary</b-nav-item>
        </b-navbar-nav>
      </b-collapse>
    </b-navbar>

    <router-view/>

    <notifications group="messages" position="top right"/>

  </div>
</template>

<script lang="ts">
import "bootstrap/dist/css/bootstrap.css";
import "bootstrap/dist/js/bootstrap";
import "bootstrap-vue/dist/bootstrap-vue.css";
import Vue from "vue";

export default class App extends Vue {
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
}
</style>
