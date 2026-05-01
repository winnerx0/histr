import { useMemo, useState } from "react";
import {
  keepPreviousData,
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";
import {
  fetchCategorySummary,
  fetchStats,
  fetchTransactions,
  fetchWorkerStatus,
  uploadTransactionDocument,
} from "./api";

const PAGE_SIZE = 15;
const PIE_COLORS = [
  "#6d4fe8",
  "#1253c4",
  "#0a7a40",
  "#b87200",
  "#c4233e",
  "#0ea5e9",
  "#7c3aed",
  "#0891b2",
  "#15803d",
  "#d97706",
  "#be123c",
  "#0284c7",
  "#6366f1",
  "#059669",
  "#ea580c",
  "#7e22ce",
  "#0e7490",
  "#dc2626",
  "#4f46e5",
  "#16a34a",
  "#ca8a04",
  "#9333ea",
  "#0369a1",
  "#b91c1c",
];

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat("en-NG", {
    style: "currency",
    currency: "NGN",
    maximumFractionDigits: 2,
  }).format(amount);

export function App() {
  const queryClient = useQueryClient();
  const [offset, setOffset] = useState(0);
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState<string>("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadMessage, setUploadMessage] = useState<string>("");
  const [isDragOver, setIsDragOver] = useState(false);

  const transactionsQuery = useQuery({
    queryKey: ["transactions", offset, search, category],
    queryFn: () =>
      fetchTransactions({
        limit: PAGE_SIZE,
        offset,
        search,
        category: category || undefined,
      }),
    placeholderData: keepPreviousData,
  });

  const statsQuery = useQuery({
    queryKey: ["stats"],
    queryFn: fetchStats,
  });

  const summaryQuery = useQuery({
    queryKey: ["category-summary"],
    queryFn: fetchCategorySummary,
  });

  const statusQuery = useQuery({
    queryKey: ["worker-status"],
    queryFn: fetchWorkerStatus,
    refetchInterval: 4000,
  });

  const uploadMutation = useMutation({
    mutationFn: uploadTransactionDocument,
    onSuccess: (payload) => {
      setUploadMessage(
        `${payload.message} (${payload.queuedBatches} batch queued)`,
      );
      setSelectedFile(null);
      void queryClient.invalidateQueries({ queryKey: ["worker-status"] });
      void queryClient.invalidateQueries({ queryKey: ["transactions"] });
      void queryClient.invalidateQueries({ queryKey: ["stats"] });
      void queryClient.invalidateQueries({ queryKey: ["category-summary"] });
    },
    onError: (error) => {
      setUploadMessage(
        error instanceof Error ? error.message : "Upload failed",
      );
    },
  });

  const totalPages = useMemo(() => {
    const total = transactionsQuery.data?.pagination.total ?? 0;
    return Math.max(Math.ceil(total / PAGE_SIZE), 1);
  }, [transactionsQuery.data?.pagination.total]);

  const currentPage = Math.floor(offset / PAGE_SIZE) + 1;

  const categories = useMemo(() => {
    return (summaryQuery.data?.data ?? []).map((item) => item.category);
  }, [summaryQuery.data?.data]);

  const topCategories = useMemo(() => {
    return summaryQuery.data?.data ?? [];
  }, [summaryQuery.data?.data]);

  const pieSegments = useMemo(() => {
    const total = topCategories.reduce(
      (sum, item) => sum + Math.abs(item.total),
      0,
    );
    if (total === 0) return [];

    let cursor = 0;
    return topCategories.map((entry, index) => {
      const value = Math.abs(entry.total);
      const portion = (value / total) * 100;
      const start = cursor;
      const end = cursor + portion;
      cursor = end;
      return {
        ...entry,
        value,
        percentage: portion,
        color: PIE_COLORS[index % PIE_COLORS.length],
        start,
        end,
      };
    });
  }, [topCategories]);

  const pieBackground = useMemo(() => {
    if (pieSegments.length === 0) return "conic-gradient(var(--border) 0 100%)";
    const stops = pieSegments
      .map((s) => `${s.color} ${s.start}% ${s.end}%`)
      .join(", ");
    return `conic-gradient(${stops})`;
  }, [pieSegments]);

  const workerIsLive = useMemo(() => {
    if (!statusQuery.data?.workerHeartbeat) return false;
    const heartbeat = new Date(statusQuery.data.workerHeartbeat).getTime();
    if (Number.isNaN(heartbeat)) return false;
    return Date.now() - heartbeat < 20000;
  }, [statusQuery.data?.workerHeartbeat]);

  return (
    <div className="app-frame">
      <div className="page-shell">
        <main className="dashboard">
          <header className="dashboard-header">
            <div className="brand-block">
              <span className="brand-mark">H</span>
              <div>
                <p className="eyebrow">Financial Intelligence</p>
                <h1>Histr</h1>
              </div>
            </div>
            <div className="header-meta">
              <span
                className={`live-pill ${workerIsLive ? "is-live" : "is-idle"}`}
              >
                {workerIsLive ? "Worker Online" : "Worker Waiting"}
              </span>
              <p>Upload statements, track spending, and monitor processing.</p>
            </div>
          </header>

          <section className="kpi-grid">
            <article className="kpi-card income-card">
              <h3>Total Income</h3>
              <p>{formatCurrency(statsQuery.data?.totalIncome ?? 0)}</p>
            </article>
            <article className="kpi-card expense-card">
              <h3>Total Expense</h3>
              <p>{formatCurrency(statsQuery.data?.totalExpense ?? 0)}</p>
            </article>
            <article className="kpi-card net-card">
              <h3>Net Balance</h3>
              <p>{formatCurrency(statsQuery.data?.netTotal ?? 0)}</p>
            </article>
            <article className="kpi-card count-card">
              <h3>Transactions</h3>
              <p>{statsQuery.data?.transactionCount ?? 0}</p>
            </article>
          </section>

          <section className="dashboard-workspace">
            <aside className="insight-rail">
              <section className="panel upload-panel">
                <div className="panel-heading">
                  <h2 className="panel-title">Upload</h2>
                  <span className="panel-kicker">.xlsx / .csv</span>
                </div>
                <label
                  className={`file-picker ${isDragOver ? "drag-over" : ""}`}
                  htmlFor="file-input"
                  onDragOver={(e) => {
                    e.preventDefault();
                    setIsDragOver(true);
                  }}
                  onDragLeave={() => setIsDragOver(false)}
                  onDrop={(e) => {
                    e.preventDefault();
                    setIsDragOver(false);
                    const file = e.dataTransfer.files?.[0] ?? null;
                    setSelectedFile(file);
                    setUploadMessage("");
                  }}
                >
                  {selectedFile ? selectedFile.name : "Drop or browse file"}
                </label>
                <input
                  id="file-input"
                  type="file"
                  accept=".xlsx,.csv"
                  onChange={(event) => {
                    const file = event.target.files?.[0] ?? null;
                    setSelectedFile(file);
                    setUploadMessage("");
                  }}
                />
                <button
                  className="primary-btn"
                  disabled={!selectedFile || uploadMutation.isPending}
                  onClick={() => {
                    if (selectedFile) uploadMutation.mutate(selectedFile);
                  }}
                >
                  {uploadMutation.isPending ? "Uploading…" : "Upload & Queue"}
                </button>
                {uploadMessage ? (
                  <p className="upload-message">{uploadMessage}</p>
                ) : null}
              </section>

              <section className="panel status-panel">
                <div className="panel-heading">
                  <h2 className="panel-title">Processing</h2>
                  <span className="panel-kicker">
                    {workerIsLive ? "Live" : "Idle"}
                  </span>
                </div>
                <dl className="status-grid">
                  <div className="status-row">
                    <dt>Queue depth</dt>
                    <dd>{statusQuery.data?.queueDepth ?? 0}</dd>
                  </div>
                  <div className="status-row">
                    <dt>Processed</dt>
                    <dd>{statusQuery.data?.processedCount ?? 0}</dd>
                  </div>
                  <div className="status-row">
                    <dt>Heartbeat</dt>
                    <dd>{statusQuery.data?.workerHeartbeat ?? "—"}</dd>
                  </div>
                </dl>
              </section>

              <section className="panel category-panel">
                <div className="panel-heading">
                  <h2 className="panel-title">Top Categories</h2>
                  <span className="panel-kicker">{pieSegments.length}</span>
                </div>
                <div className="category-chart">
                  <div
                    className="pie-chart"
                    style={{ background: pieBackground }}
                  >
                    <div className="pie-center">
                      <span>Total</span>
                      <strong>
                        {formatCurrency(
                          pieSegments.reduce((sum, s) => sum + s.value, 0),
                        )}
                      </strong>
                    </div>
                  </div>

                  <div className="pie-legend">
                    {pieSegments.map((segment) => (
                      <div key={segment.category} className="legend-item">
                        <span
                          className="legend-dot"
                          style={{ backgroundColor: segment.color }}
                        />
                        <span className="legend-label">{segment.category}</span>
                        <span className="legend-value">
                          {Math.round(segment.percentage)}%
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </section>
            </aside>

            <section className="panel transactions-panel">
              <div className="table-header">
                <div>
                  <h2 className="panel-title">Transactions</h2>
                  <p className="table-subtitle">
                    {transactionsQuery.data?.pagination.total ?? 0} records
                    loaded
                  </p>
                </div>
                <div className="filters">
                  <input
                    placeholder="Search description or recipient"
                    value={search}
                    onChange={(event) => {
                      setOffset(0);
                      setSearch(event.target.value);
                    }}
                  />
                  <select
                    value={category}
                    onChange={(event) => {
                      setOffset(0);
                      setCategory(event.target.value);
                    }}
                  >
                    <option value="">All categories</option>
                    {categories.map((item) => (
                      <option key={item} value={item}>
                        {item}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Description</th>
                      <th>Recipient</th>
                      <th>Category</th>
                      <th>Amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(transactionsQuery.data?.data ?? []).map((item) => (
                      <tr key={item.id}>
                        <td>{new Date(item.createdAt).toLocaleDateString()}</td>
                        <td>{item.description}</td>
                        <td>{item.receipient ?? "—"}</td>
                        <td>
                          <span className="category-tag">{item.category}</span>
                        </td>
                        <td>
                          <span
                            className={`amount-pill ${item.amount < 0 ? "negative" : "positive"}`}
                          >
                            {formatCurrency(item.amount)}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="pagination">
                <button
                  className="secondary-btn"
                  disabled={currentPage <= 1}
                  onClick={() =>
                    setOffset((previous) => Math.max(previous - PAGE_SIZE, 0))
                  }
                >
                  ← Previous
                </button>
                <span>
                  Page {currentPage} / {totalPages}
                </span>
                <button
                  className="secondary-btn"
                  disabled={currentPage >= totalPages}
                  onClick={() => setOffset((previous) => previous + PAGE_SIZE)}
                >
                  Next →
                </button>
              </div>
            </section>
          </section>
        </main>
      </div>
    </div>
  );
}
