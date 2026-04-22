import Elysia from "elysia";
import { DocumentService } from "./service";
import { DocumentModal } from "./model";
import { and, count, desc, eq, gte, ilike, lte, sql, sum } from "drizzle-orm";
import { db } from "../db";
import { categoryTable, documentTable } from "../db/schema";
import { redis } from "..";
import { config } from "../config";

const toNumber = (value: unknown): number => {
    if (value === null || value === undefined) {
        return 0;
    }

    const numeric = Number(value);
    return Number.isNaN(numeric) ? 0 : numeric;
};

const normalizePagination = (limit?: string, offset?: string) => {
    const normalizedLimit = Math.min(Math.max(Number(limit ?? "25"), 1), 200);
    const normalizedOffset = Math.max(Number(offset ?? "0"), 0);
    return {
        limit: Number.isNaN(normalizedLimit) ? 25 : normalizedLimit,
        offset: Number.isNaN(normalizedOffset) ? 0 : normalizedOffset,
    };
};

const buildTransactionFilters = (query: {
    search?: string;
    category?: string;
    startDate?: string;
    endDate?: string;
}) => {
    const conditions = [];

    if (query.search?.trim()) {
        const searchTerm = `%${query.search.trim()}%`;
        conditions.push(
            sql`(${ilike(documentTable.description, searchTerm)} OR ${ilike(documentTable.receipient, searchTerm)})`,
        );
    }

    // if (query.category && categoryValues.includes(query.category as never)) {
    //     conditions.push(eq(documentTable.category, query.category as (typeof categoryValues)[number]));
    // }

    if (query.startDate) {
        const date = new Date(query.startDate);
        if (!Number.isNaN(date.getTime())) {
            conditions.push(gte(documentTable.createdAt, date));
        }
    }

    if (query.endDate) {
        const date = new Date(query.endDate);
        if (!Number.isNaN(date.getTime())) {
            conditions.push(lte(documentTable.createdAt, date));
        }
    }

    if (conditions.length === 0) {
        return undefined;
    }

    return and(...conditions);
};

export const documentController = new Elysia()
    .get(
        "/parse",
        async ({ set }) => {
            try {
                return await DocumentService.parseDocument();
            } catch (error) {
                set.status = 400;
                return {
                    message:
                        error instanceof Error
                            ? error.message
                            : "Invalid document format",
                };
            }
        },
        {
            response: {
                200: DocumentModal.parseDocumentResponse,
                400: DocumentModal.parseInvalid,
            },
        },
    )
    .post(
        "/documents/upload",
        async ({ body, set }) => {
            try {
                return await DocumentService.parseUploadedFile(body.file);
            } catch (error) {
                set.status = 400;
                return {
                    message:
                        error instanceof Error
                            ? error.message
                            : "Invalid document format",
                };
            }
        },
        {
            body: DocumentModal.uploadRequest,
            response: {
                200: DocumentModal.uploadResponse,
                400: DocumentModal.parseInvalid,
            },
        },
    )
    .get(
        "/transactions",
        async ({ query }) => {
            const { limit, offset } = normalizePagination(query.limit, query.offset);
            const where = buildTransactionFilters(query);

            const [rows, [{ total }]] = await Promise.all([
                db
                    .select()
                .from(documentTable)
                  .innerJoin(categoryTable, eq(documentTable.categoryId, categoryTable.id))
                    .where(where)
                    .orderBy(desc(documentTable.createdAt))
                    .limit(limit)
                    .offset(offset),
                db
                    .select({ total: count() })
                    .from(documentTable)
                    .where(where),
            ]);

            return {
                data: rows.map((row) => ({
                    id: row.id,
                    amount: toNumber(row.documents.amount),
                    receipient: row.documents.receipient,
                    description: row.documents.description,
                    category: row.categories.name,
                    createdAt: row.documents.createdAt,
                })),
                pagination: {
                    limit,
                    offset,
                    total: toNumber(total),
                },
            };
        },
        {
            query: DocumentModal.transactionsQuery,
            response: {
                200: DocumentModal.transactionsResponse,
            },
        },
    )
    .get(
        "/transactions/stats",
        async ({ query }) => {
            const where = buildTransactionFilters(query);

            const [result] = await db
                .select({
                    totalIncome:
                        sql<number>`COALESCE(SUM(CASE WHEN ${documentTable.amount} > 0 THEN ${documentTable.amount} ELSE 0 END), 0)`,
                    totalExpense:
                        sql<number>`COALESCE(SUM(CASE WHEN ${documentTable.amount} < 0 THEN ${documentTable.amount} ELSE 0 END), 0)`,
                    netTotal: sql<number>`COALESCE(SUM(${documentTable.amount}), 0)`,
                    transactionCount: count(),
                })
                .from(documentTable)
                .where(where);

            const totalIncome = toNumber(result?.totalIncome);
            const rawExpense = toNumber(result?.totalExpense);

            return {
                totalIncome,
                totalExpense: Math.abs(rawExpense),
                netTotal: toNumber(result?.netTotal),
                transactionCount: toNumber(result?.transactionCount),
            };
        },
        {
            query: DocumentModal.transactionsQuery,
            response: {
                200: DocumentModal.statsResponse,
            },
        },
    )
    .get(
        "/categories/summary",
        async ({ query }) => {
            const where = buildTransactionFilters(query);

            const rows = await db
                .select({
                    category: categoryTable.name,
                    total: sum(documentTable.amount),
                    count: count(),
                })
              .from(documentTable)
              .innerJoin(categoryTable, eq(documentTable.categoryId, categoryTable.id))
                .where(where)
                .groupBy(categoryTable.id)
                .orderBy(desc(sum(documentTable.amount)));

            return {
                data: rows.map((row) => ({
                    category: row.category,
                    total: toNumber(row.total),
                    count: toNumber(row.count),
                })),
            };
        },
        {
            query: DocumentModal.transactionsQuery,
            response: {
                200: DocumentModal.categorySummaryResponse,
            },
        },
    )
    .get(
        "/workers/status",
        async () => {
            const queueDepth = await redis.client?.llen("transactions");
            const workerHeartbeat = await redis.client?.get(config.WORKER_HEARTBEAT_KEY);
            const processedCountRaw = await redis.client?.get(
                config.WORKER_PROCESSED_COUNT_KEY,
            );

            return {
                queueDepth: toNumber(queueDepth),
                workerHeartbeat: workerHeartbeat ?? null,
                processedCount: toNumber(processedCountRaw),
            };
        },
        {
            response: {
                200: DocumentModal.workerStatusResponse,
            },
        },
    );