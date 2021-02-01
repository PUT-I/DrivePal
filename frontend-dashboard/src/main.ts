import Vue from "vue";
import VueRouter from "vue-router";
import Notifications from "vue-notification";
import Datetime from "vue-datetime";
// @ts-ignore
import Chart from "vue2-frappe";
import {BootstrapVue, IconsPlugin} from "bootstrap-vue";
import App from "@/App";
import DetectionVerificationList from "@/components/DetectionVerificationList";
import DiagnosticsVisualization from "@/components/DiagnosticsVisualization";
import SocDictionary from "@/components/SocDictionary";

Vue.use(VueRouter);
Vue.use(Notifications);
// @ts-ignore
Vue.use(Datetime);
Vue.use(Chart);
Vue.use(BootstrapVue);
Vue.use(IconsPlugin);

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
