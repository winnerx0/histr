import 'dotenv/config';
import { drizzle } from "drizzle-orm/node-postgres";
import { Pool } from "pg";

const pool = new Pool({
  connectionString: process.env.DATABASE_URL!,
  maxLifetimeSeconds: 10,
  maxUses: 5,
  idleTimeoutMillis: 5000,
});
export const db = drizzle({ client: pool });
