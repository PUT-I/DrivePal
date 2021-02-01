export default {
    async sleep(millis: number) {
        await new Promise((resolve) => setTimeout(resolve, millis));
    },
    formatIsoDate(date: Date): string {
        return date.toISOString()
            .replace("T", " ")
            .replace("Z", "")
            .replace(/\.[0-9]+$/gi, "");
    }
};
