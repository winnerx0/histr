import { Elysia } from "elysia";
import { documentController } from "./document";
import { RedisService } from "./redis";
import { cors } from "@elysiajs/cors";
import { categorySeeder } from "./utils/seeder";

export const redis = new RedisService();

await redis.connect();

await categorySeeder();

const app = new Elysia({ prefix: "/api/v1" })
  .use(cors())
  .use(documentController)
  .get("/", () => "Hello Elysia")
  .listen(5000);

console.log(
  `🦊 Elysia is running at ${app.server?.hostname}:${app.server?.port}`,
);
