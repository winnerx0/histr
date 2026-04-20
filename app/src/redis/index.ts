import { RedisClient } from "bun";
import { config } from "../config";

export class RedisService {
    client: RedisClient | null = null

    isConnected = false;

    constructor() {
        if (!this.isConnected) {
            this.client = new RedisClient(config.REDIS_URL);

        }
    }

    async connect(){
        if (this.client) {
            await this.client.connect();
            this.isConnected = true;
        }
    }

    async disconnect(){
        if (this.client) {
            await this.client.close();
            this.isConnected = false;
        }
    }
}