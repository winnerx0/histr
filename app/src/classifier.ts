import { pipeline } from "@huggingface/transformers";
import { db } from "./db";
import { categoryTable } from "./db/schema";
import { sql } from "drizzle-orm";

export async function classifyTransaction(
  description: string,
  receipient?: string,
  amount?: number,
): Promise<string> {
  const text = `${description} ${receipient ?? ""}`.replace(/\s+/g, " ").trim();

  const extractor = await pipeline(
    "feature-extraction",
    "sentence-transformers/all-MiniLM-L6-v2",
  );

  const categoryData = await db
    .select({ label: categoryTable.name, embeddings: categoryTable.embeddings })
    .from(categoryTable);

  const categoryEmbeddings = categoryData
    .map((data) => data.embeddings)
    .flatMap((embeddings) => embeddings);

  const transactionResult = await extractor(text, {
    pooling: "mean",
    normalize: true,
  });

  const transactionVector = transactionResult.data as Float32Array;

  let bestScore = -Infinity;
  let bestIdx = 0;

  for (let i = 0; i < categoryEmbeddings.length; i += 384) {
    const score = transactionVector.reduce(
      (acc, val, j) => acc + val * categoryEmbeddings[i + j],
      0,
    );
    if (score > bestScore) {
      bestScore = score;
      bestIdx = i / 384;
    }
  }

  console.log(`Best score: ${bestScore}, Best index: ${categoryData[bestIdx].label}`);

  const queryVector = `[${categoryData[bestIdx].embeddings}]`

  const category = await db
    .select({ id: categoryTable.id })
    .from(categoryTable)
    .orderBy(sql`embeddings <#> ${queryVector}::vector`)
    .limit(1);

  return category[0].id;
}
