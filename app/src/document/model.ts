import {
    t,
    type UnwrapSchema,
} from "elysia";

export const DocumentModal = {
    parseDocumentResponse: t.Object({
        message: t.String(),
        queuedBatches: t.Number(),
    }),
    uploadRequest: t.Object({
        file: t.File(),
    }),
    uploadResponse: t.Object({
        message: t.String(),
        fileName: t.String(),
        queuedBatches: t.Number(),
    }),
    transactionsQuery: t.Object({
        limit: t.Optional(t.String()),
        offset: t.Optional(t.String()),
        search: t.Optional(t.String()),
        category: t.Optional(t.String()),
        startDate: t.Optional(t.String()),
        endDate: t.Optional(t.String()),
    }),
    transaction: t.Object({
        id: t.String(),
        amount: t.Number(),
        receipient: t.Nullable(t.String()),
        description: t.String(),
        category: t.String(),
        createdAt: t.Date(),
    }),
    transactionsResponse: t.Object({
        data: t.Array(
            t.Object({
                id: t.String(),
                amount: t.Number(),
                receipient: t.Nullable(t.String()),
                description: t.String(),
                category: t.String(),
                createdAt: t.Date(),
            }),
        ),
        pagination: t.Object({
            limit: t.Number(),
            offset: t.Number(),
            total: t.Number(),
        }),
    }),
    statsResponse: t.Object({
        totalIncome: t.Number(),
        totalExpense: t.Number(),
        netTotal: t.Number(),
        transactionCount: t.Number(),
    }),
    categorySummaryResponse: t.Object({
        data: t.Array(
            t.Object({
                category: t.String(),
                total: t.Number(),
                count: t.Number(),
            }),
        ),
    }),
    workerStatusResponse: t.Object({
        queueDepth: t.Number(),
        workerHeartbeat: t.Nullable(t.String()),
        processedCount: t.Number(),
    }),
    parseInvalid: t.Object({
        message: t.String(),
    }),
};

export type DocumentModal = {
    [k in keyof typeof DocumentModal]: UnwrapSchema<(typeof DocumentModal)[k]>;
};