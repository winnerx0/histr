import {
  decimal,
  pgEnum,
  pgTable,
  timestamp,
  uuid,
  varchar,
} from "drizzle-orm/pg-core";
import { v7 as uuidv7 } from "uuid";

export const categoryEnum = pgEnum("category", [
  "Income",
  "Food & Dining",
  "Transport",
  "Housing",
  "Utilities",
  "Healthcare",
  "Education",
  "Shopping",
  "Subscriptions",
  "Entertainment",
  "Finance",
  "Family",
  "Travel",
  "Work",
  "Charity",
  "Insurance",
  "Crypto",
  "Government",
  "Legal",
  "Business",
  "Personal",
  "Communication",
  "Events",
  "Misc",
  "Other",
]);

export const documentTable = pgTable("documents", {
  id: uuid("id")
    .primaryKey()
    .$defaultFn(() => uuidv7()),
  receipient: varchar("recepient", { length: 100 }),
  amount: decimal("amount", { precision: 10, scale: 2 }).$type<number>().notNull(),
  description: varchar("description").notNull(),
  category: categoryEnum("category").default("Other").notNull(),
  createdAt: timestamp("created_at", { withTimezone: true })
    .notNull()
    .$defaultFn(() => new Date())
});