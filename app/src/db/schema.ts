import {
  decimal,
  pgTable,
  text,
  timestamp,
  uuid,
  varchar,
  vector,
} from "drizzle-orm/pg-core";
import { v7 as uuidv7 } from "uuid";

export const categoryTable = pgTable("categories", {
  id: uuid("id")
    .primaryKey()
    .$defaultFn(() => uuidv7()),
  name: varchar("name", { length: 100 }).notNull(),
  keywords: text("keywords").notNull(),
  embeddings: vector("embeddings", { dimensions: 384 }).notNull(),
  createdAt: timestamp("created_at", { withTimezone: true })
    .notNull()
    .$defaultFn(() => new Date()),
});

export const documentTable = pgTable("documents", {
  id: uuid("id")
    .primaryKey()
    .$defaultFn(() => uuidv7()),
  receipient: varchar("recepient", { length: 100 }),
  amount: decimal("amount", { precision: 10, scale: 2 })
    .$type<number>()
    .notNull(),
  description: varchar("description").notNull(),
  categoryId: uuid("category_id").references(() => categoryTable.id),
  createdAt: timestamp("created_at", { withTimezone: true })
    .notNull()
    .$defaultFn(() => new Date()),
});
