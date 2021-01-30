import axios from "axios";

const baseUrl = "http://server.drivepal.pl:5000/api/detection";
const config = {
    dataType: "json",
    headers: {
        "Accept": "application/json",
        "Content-Type": "application/json"
    },
};

export default {
    getAllDetections() {
        return axios.get(baseUrl, config);
    },
    getDetection(id) {
        const url = `${baseUrl}/${id}`;
        return axios.get(url, config);
    },
    getDetectionsValidity() {
        const url = `${baseUrl}/valid`;
        return axios.get(url, config);
    },
    saveVerification(ids) {
        console.log("Saving verification");
        console.log(ids);
        return axios.patch(baseUrl, ids, config);
    },
    deleteDetectionImage(id) {
        const url = `${baseUrl}/${id}/image`;
        return axios.delete(url, config);
    }
};
