import axios, {AxiosResponse} from "axios";

const baseUrl = "http://server.drivepal.pl:5000/api/diagnostic-data";
const config = {
    dataType: "json",
    headers: {
        "Accept": "application/json",
        "Content-Type": "application/json"
    },
};

export interface AverageTimes {
    modelName: string,
    averageInferenceTime: number,
    averageProcessingTime: number
}

export interface DiagnosticData {
    id: 1,
    timeStamp: string,
    soc: string,
    deviceId: string,
    processingTime: number,
    inferenceTime: number,
    modelName: string,
}

export default {
    getModels(): Promise<AxiosResponse<string[]>> {
        const url = `${baseUrl}/models`;
        return axios.get(url, config);
    },
    getDevices(): Promise<AxiosResponse<string[]>> {
        const url = `${baseUrl}/devices`;
        return axios.get(url, config);
    },
    getSocs(useSocDictionary = true): Promise<AxiosResponse<string[]>> {
        const url = `${baseUrl}/soc?useSocDictionary=${useSocDictionary}`;
        return axios.get(url, config);
    },
    getAverage(soc = "ALL", useSocDictionary = true): Promise<AxiosResponse<AverageTimes[]>> {
        const url = `${baseUrl}/average?soc=${soc}&useSocDictionary=${useSocDictionary}`;
        return axios.get(url, config);
    },
    getDiagnosticData(from = "",
                      to = "",
                      modelName = "",
                      deviceId = ""): Promise<AxiosResponse<DiagnosticData[]>> {
        const queryString = `?from=${from}&to=${to}&modelName=${modelName}&deviceId=${deviceId}`;
        const url = baseUrl + queryString;

        console.log(`Sending request : ${url}`);
        return axios.get(url, config);
    }
};
