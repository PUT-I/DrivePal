import Vue from "vue";
import VueRouter from "vue-router";
import Notifications from "vue-notification";
import {BootstrapVue, IconsPlugin} from "bootstrap-vue";
import DetectionVerificationList from "@/components/DetectionVerificationList";
import App from "@/App";
import DiagnosticsVisualization from "@/components/DiagnosticsVisualization";
import SocDictionary from "@/components/SocDictionary";
import Chart from "vue2-frappe";
import Datetime from "vue-datetime";

Vue.use(VueRouter);
Vue.use(Notifications);
Vue.use(BootstrapVue);
Vue.use(IconsPlugin);
Vue.use(Chart);
Vue.use(Datetime);

const routes = [
    {path: "/", redirect: "/detections/"},
    {path: '/detections/', component: DetectionVerificationList},
    {path: '/diagnostics/', component: DiagnosticsVisualization},
    {path: '/soc-dictionary/', component: SocDictionary}
];

const router = new VueRouter({
    mode: 'history',
    routes: routes
});

Vue.config.productionTip = false;

new Vue({
    router: router,
    render: h => h(App)
}).$mount("#app");
