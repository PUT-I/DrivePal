import axios from "axios";

const baseUrl = "http://server.drivepal.pl:5000/api/soc-dictionary";
const config = {
    dataType: "json",
    headers: {
        "Accept": "application/json",
        "Content-Type": "application/json"
    },
};

export default {
    getAllEntities() {
        return axios.get(baseUrl, config);
    },
    saveEntity(code, name) {
        code = code.trim();
        console.log(`Saving dictionary entry : ${code} , ${name}`);
        return axios.post(baseUrl, {code: code, name: name}, config);
    },
    updateEntity(code, name) {
        console.log(`Updating dictionary entry : ${code} , ${name}`);
        return axios.patch(baseUrl, {code: code, name: name}, config);
    },
    deleteEntity(id) {
        console.log(`Deleting dictionary entry : ${id}`);
        return axios.delete(`${baseUrl}/${id}`, config);
    }
};
