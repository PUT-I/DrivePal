export default {
    async sleep(millis) {
        await new Promise((resolve) => setTimeout(resolve, millis));
    },
    formatIsoDate(date) {
        return date.toISOString()
            .replaceAll("T", " ")
            .replaceAll("Z", "")
            .replaceAll(/\.[0-9]+$/gi, "");
    }
};
