import axios from "axios";

const baseUrl = "http://server.drivepal.pl:5000/api/diagnostic-data";
const config = {
    dataType: "json",
    headers: {
        "Accept": "application/json",
        "Content-Type": "application/json"
    },
};

export default {
    getModels() {
        const url = `${baseUrl}/models`;
        return axios.get(url, config);
    },
    getDevices() {
        const url = `${baseUrl}/devices`;
        return axios.get(url, config);
    },
    getSocs(useSocDictionary = true) {
        const url = `${baseUrl}/soc?useSocDictionary=${useSocDictionary}`;
        return axios.get(url, config);
    },
    getAverage(soc = "ALL", useSocDictionary = true) {
        const url = `${baseUrl}/average?soc=${soc}&useSocDictionary=${useSocDictionary}`;
        return axios.get(url, config);
    },
    getDiagnosticData(from = "", to = "", modelName = "", deviceId = "") {
        const queryString = `?from=${from}&to=${to}&modelName=${modelName}&deviceId=${deviceId}`;
        const url = baseUrl + queryString;

        console.log(`Sending request : ${url}`);
        return axios.get(url, config);
    }
};
