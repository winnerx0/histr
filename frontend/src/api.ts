export type Transaction = {
  id: string;
  amount: number;
  receipient: string | null;
  description: string;
  category: string;
  createdAt: string;
};

export type TransactionsResponse = {
  data: Transaction[];
  pagination: {
    limit: number;
    offset: number;
    total: number;
  };
};

export type StatsResponse = {
  totalIncome: number;
  totalExpense: number;
  netTotal: number;
  transactionCount: number;
};

export type CategorySummaryResponse = {
  data: {
    category: string;
    total: number;
    count: number;
  }[];
};

export type WorkerStatusResponse = {
  queueDepth: number;
  workerHeartbeat: string | null;
  processedCount: number;
};

const apiBase =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:5000/api/v1";

const asQuery = (params: Record<string, string | number | undefined>) => {
  const query = new URLSearchParams();

  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") {
      query.set(key, String(value));
    }
  }

  const serialized = query.toString();
  return serialized ? `?${serialized}` : "";
};

export const fetchTransactions = async (params: {
  limit: number;
  offset: number;
  search: string;
  category?: string;
}) => {
  const response = await fetch(
    `${apiBase}/transactions${asQuery({
      limit: params.limit,
      offset: params.offset,
      search: params.search,
      category: params.category,
    })}`,
  );

  if (!response.ok) {
    throw new Error("Unable to load transactions");
  }

  return (await response.json()) as TransactionsResponse;
};

export const fetchStats = async () => {
  const response = await fetch(`${apiBase}/transactions/stats`);

  if (!response.ok) {
    throw new Error("Unable to load dashboard stats");
  }

  return (await response.json()) as StatsResponse;
};

export const fetchCategorySummary = async () => {
  const response = await fetch(`${apiBase}/categories/summary`);

  if (!response.ok) {
    throw new Error("Unable to load category summary");
  }

  return (await response.json()) as CategorySummaryResponse;
};

export const fetchWorkerStatus = async () => {
  const response = await fetch(`${apiBase}/workers/status`);

  if (!response.ok) {
    throw new Error("Unable to load worker status");
  }

  return (await response.json()) as WorkerStatusResponse;
};

export const uploadTransactionDocument = async (file: File) => {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${apiBase}/documents/upload`, {
    method: "POST",
    body: formData,
  });

  const payload = await response.json();

  if (!response.ok) {
    throw new Error(payload.message ?? "Upload failed");
  }

  return payload as { message: string; fileName: string; queuedBatches: number };
};
