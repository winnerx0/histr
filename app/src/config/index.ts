import { env } from "bun";
import z from "zod";

const envSchema = z.object({
    REDIS_URL: z.string().default("redis://localhost:6379"),
    DATABASE_URL: z.string().min(1, "DATABASE_URL is required"),
    UPLOAD_DIR: z.string().default("uploads"),
    MAX_UPLOAD_SIZE_MB: z
        .string()
        .default("15")
        .transform((value) => Number(value))
        .pipe(z.number().positive()),
    WORKER_HEARTBEAT_KEY: z.string().default("worker:heartbeat"),
    WORKER_PROCESSED_COUNT_KEY: z.string().default("worker:processed_count"),
});

export const config = envSchema.parse(env);