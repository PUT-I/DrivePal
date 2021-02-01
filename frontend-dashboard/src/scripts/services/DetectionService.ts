import axios, {AxiosResponse} from "axios";

const baseUrl: string = "http://server.drivepal.pl:5000/api/detection";
const config = {
    dataType: "json",
    headers: {
        "Accept": "application/json",
        "Content-Type": "application/json"
    },
};

export interface DetectionUrl {
    id: number,
    timestamp: string,
    modelName: string,
    entityUrl: string
}

export interface DetectionValidity {
    modelName: string,
    valid: number,
    invalid: number,
    unverified: number
}

export interface Detection {
    id: number,
    className: string,
    confidence: number,
    locationLeft: number,
    locationTop: number,
    locationRight: number,
    locationBottom: number,
    valid: boolean
}

export interface DetectionImage {
    id: number,
    detections: Detection[],
    modelName: string
}

export default {
    getAllDetections(): Promise<AxiosResponse<DetectionUrl[]>> {
        return axios.get(baseUrl, config);
    },
    getDetection(id: number): Promise<AxiosResponse<DetectionImage>> {
        const url = `${baseUrl}/${id}`;
        return axios.get(url, config);
    },
    getDetectionsValidity(): Promise<AxiosResponse<DetectionValidity[]>> {
        const url = `${baseUrl}/valid`;
        return axios.get(url, config);
    },
    saveVerification(detections: Detection[]): Promise<AxiosResponse<void>> {
        console.log("Saving verification");
        console.log(detections);
        return axios.patch(baseUrl, detections, config);
    },
    deleteDetectionImage(id: number): Promise<AxiosResponse<ArrayBuffer>> {
        const url = `${baseUrl}/${id}/image`;
        return axios.delete(url, config);
    }
};
