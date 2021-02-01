import axios, {AxiosResponse} from "axios";

const baseUrl = "http://server.drivepal.pl:5000/api/soc-dictionary";
const config = {
    dataType: "json",
    headers: {
        "Accept": "application/json",
        "Content-Type": "application/json"
    },
};

export interface SocDictionaryEntry {
    id: number,
    code: string,
    name: string
}

export default {
    getAllEntities(): Promise<AxiosResponse<SocDictionaryEntry[]>> {
        return axios.get(baseUrl, config);
    },
    saveEntity(code: string, name: string): Promise<AxiosResponse<SocDictionaryEntry>> {
        code = code.trim();
        console.log(`Saving dictionary entry : ${code} , ${name}`);
        return axios.post<SocDictionaryEntry>(baseUrl, {code: code, name: name}, config);
    },
    updateEntity(code: string, name: string): Promise<AxiosResponse<SocDictionaryEntry>> {
        code = code.trim();
        console.log(`Updating dictionary entry : ${code} , ${name}`);
        return axios.patch(baseUrl, {code: code, name: name}, config);
    },
    deleteEntity(id: number): Promise<AxiosResponse<SocDictionaryEntry>> {
        console.log(`Deleting dictionary entry : ${id}`);
        return axios.delete(`${baseUrl}/${id}`, config);
    }
};
