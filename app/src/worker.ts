import { Chrono } from "chrono-node";
import { documentTable } from "./db/schema";
import { RedisService } from "./redis";
import { db } from "./db";
import { classifyTransaction } from "./classifier";
import { config } from "./config";

const redis = new RedisService();

const updateWorkerHeartbeat = async () => {
  await redis.client?.set(config.WORKER_HEARTBEAT_KEY, new Date().toISOString());
};

redis
  .connect()
  .then(async () => {
    console.log("Connected to Redis successfully!");

    await updateWorkerHeartbeat();

    setInterval(async () => {
      try {
        await updateWorkerHeartbeat();
      } catch (error) {
        console.error("Failed to update worker heartbeat", error);
      }
    }, 5000);

    while (true) {
      let transaction;

      try {
        transaction = await redis.client?.blpop("transactions", 0);
      } catch (error) {
        console.error("Redis pop failed", error);
        continue;
      }

      if (!transaction) {
        continue;
      }

      const data = JSON.parse(transaction![1]) as string[][];

      const documents: (typeof documentTable.$inferInsert)[] = [];

      console.log("bank", data.length);
      let skip = 0;
      for (let i = 0; i < data.length; i++) {
        skip++;
        if (
          data[i][0] &&
          data[i][0].toString().toLowerCase().includes("date")
        ) {
          for (let j = 0; j < data.length - skip; j++) {
            const document = {
              amount: 0,
              receipient: "",
              description: "",
              createdAt: new Date(),
            } as typeof documentTable.$inferInsert;

            for (let k = 0; k < data[skip + j].length; k++) {
              const key = data[skip - 1][k] as string;

              if (!key) continue;

              const value = data[skip + j][k] as string;

              if (
                key.toLowerCase().includes("date")
              ) {
                document.createdAt = new Chrono().parseDate(value)!;
              }
              if (key.toLowerCase().includes("description")) {
                document.description = value;
              }
              if (
                key.toLowerCase().includes("credit") ||
                key.toLowerCase().includes("money in")
              ) {
                if (value && value !== "--") {
                  document.amount = parseFloat(
                    value.replace("₦", "").replaceAll(",", ""),
                  );
                }
              }
              if (
                key.toLowerCase().includes("debit") ||
                key.toLowerCase().includes("money out")
              ) {
                if (value && value !== "--") {
                  document.amount =
                    parseFloat(value.replace("₦", "").replaceAll(",", "")) * -1;
                }
              }
              if (
                key.toLowerCase().includes("receipient") ||
                key.toLowerCase().includes("payee") ||
                key.toLowerCase().includes("beneficiary") ||
                key.toLowerCase().includes("to / from")
              ) {
                document.receipient = value;
              }

            }
            const categoryId = await classifyTransaction(
              document.description ?? "",
              document.receipient ?? "",
              document.amount,
            );
            document.categoryId = categoryId

            documents.push(document);
          }
        }
      }
      
      if (documents.length > 0) {
        await db
          .insert(documentTable)
          .values(documents)
          .then(async () => {
            await redis.client?.incrby(
              config.WORKER_PROCESSED_COUNT_KEY,
              documents.length,
            );
          })
          .catch((e) => {
            console.log(e);
          });
      }
      
      await updateWorkerHeartbeat();
    }
  })
  .catch((err) => {
    console.error("Failed to connect to Redis:", err);
  });
