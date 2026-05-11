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
  parseLocalDocuments,
  uploadTransactionDocument,
} from "./api";
import { useAuth } from "./auth";

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

const buttonBase =
  "rounded-lg border px-3.5 py-2 text-sm font-medium transition-colors disabled:cursor-default disabled:opacity-50";
const primaryButton = `${buttonBase} border-gray-900 bg-gray-900 text-white enabled:hover:border-gray-800 enabled:hover:bg-gray-800`;
const secondaryButton = `${buttonBase} border-gray-200 bg-white text-gray-900 enabled:hover:border-gray-300`;
const inputClass =
  "w-full rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-900 outline-none transition focus:border-gray-900 focus:ring-4 focus:ring-gray-900/10";
const panelClass = "rounded-lg border border-gray-200 bg-white p-4";
const panelHeadingClass = "mb-3.5 flex items-center justify-between";
const panelTitleClass = "text-sm font-semibold";
const panelKickerClass = "text-xs text-gray-500";
const kpiCardClass =
  "grid gap-2 rounded-lg border border-gray-200 bg-white p-4";
const kpiLabelClass =
  "text-xs font-medium uppercase tracking-[0.06em] text-gray-500";
const tableHeadCellClass =
  "sticky top-0 border-b border-gray-200 bg-gray-50 px-3 py-2.5 text-left text-xs font-medium uppercase tracking-[0.06em] text-gray-500";
const tableCellClass = "border-b border-gray-200 px-3 py-2.5 text-sm";

const formatCurrency = (amount: number) =>
  new Intl.NumberFormat("en-NG", {
    style: "currency",
    currency: "NGN",
    maximumFractionDigits: 2,
  }).format(amount);

export function App() {
  const queryClient = useQueryClient();
  const { username, profilePicture, logout } = useAuth();
  const [pageNo, setPageNo] = useState(0);
  const [search, setSearch] = useState("");
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploadMessage, setUploadMessage] = useState<string>("");
  const [isDragOver, setIsDragOver] = useState(false);

  const transactionsQuery = useQuery({
    queryKey: ["transactions", pageNo, search],
    queryFn: () =>
      fetchTransactions({
        limit: PAGE_SIZE,
        pageNo,
        search,
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

  const invalidateData = () => {
    void queryClient.invalidateQueries({ queryKey: ["worker-status"] });
    void queryClient.invalidateQueries({ queryKey: ["transactions"] });
    void queryClient.invalidateQueries({ queryKey: ["stats"] });
    void queryClient.invalidateQueries({ queryKey: ["category-summary"] });
  };

  const uploadMutation = useMutation({
    mutationFn: uploadTransactionDocument,
    onSuccess: (payload) => {
      setUploadMessage(
        `${payload.message} (${payload.queuedBatches} batch queued)`,
      );
      setSelectedFile(null);
      invalidateData();
    },
    onError: (error) => {
      setUploadMessage(
        error instanceof Error ? error.message : "Upload failed",
      );
    },
  });

  const parseMutation = useMutation({
    mutationFn: parseLocalDocuments,
    onSuccess: (payload) => {
      setUploadMessage(
        `${payload.message} (${payload.queuedBatches} batches queued)`,
      );
      invalidateData();
    },
    onError: (error) => {
      setUploadMessage(error instanceof Error ? error.message : "Parse failed");
    },
  });

  const totalPages = useMemo(() => {
    const total = transactionsQuery.data?.pagination.total ?? 0;
    return Math.max(Math.ceil(total / PAGE_SIZE), 1);
  }, [transactionsQuery.data?.pagination.total]);

  const currentPage = pageNo + 1;
  const displayName = username ?? "Account";
  const userInitial = displayName.trim().charAt(0).toUpperCase() || "A";

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
    if (pieSegments.length === 0) return "conic-gradient(#e5e7eb 0 100%)";
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
    <div className="min-h-screen">
      <div className="mx-auto w-full max-w-[1200px] p-6 max-sm:p-4">
        <main className="grid gap-6">
          <header className="flex flex-col gap-4 border-b border-gray-200 pb-4 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <span className="grid h-9 w-9 place-items-center rounded-lg bg-gray-900 font-bold text-white">
                H
              </span>
              <div>
                <p className="text-xs uppercase tracking-[0.08em] text-gray-500">
                  Financial Dashboard
                </p>
                <h1 className="text-[1.4rem] font-bold">Histr</h1>
              </div>
            </div>
            <div className="flex flex-wrap items-center gap-3">
              <span
                className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium ${
                  workerIsLive
                    ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                    : "border-gray-200 text-gray-500"
                }`}
              >
                <span
                  className={`h-1.5 w-1.5 rounded-full ${
                    workerIsLive ? "bg-emerald-700" : "bg-gray-500"
                  }`}
                  aria-hidden="true"
                />
                {workerIsLive ? "Worker online" : "Worker idle"}
              </span>
              <span className="inline-flex min-w-0 items-center gap-2 text-sm text-gray-500">
                {profilePicture ? (
                  <img
                    className="h-7 w-7 shrink-0 rounded-full border border-gray-200 object-cover"
                    src={profilePicture}
                    alt=""
                    referrerPolicy="no-referrer"
                  />
                ) : (
                  <span
                    className="grid h-7 w-7 shrink-0 place-items-center rounded-full border border-gray-200 bg-gray-100 text-xs font-bold text-gray-900"
                    aria-hidden="true"
                  >
                    {userInitial}
                  </span>
                )}
                <span className="max-w-56 truncate max-sm:max-w-40">
                  {displayName}
                </span>
              </span>
              <button className={secondaryButton} onClick={logout}>
                Sign out
              </button>
            </div>
          </header>

          <section className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <article className={kpiCardClass}>
              <h3 className={kpiLabelClass}>Total Income</h3>
              <p className="text-xl font-bold text-emerald-700">
                {formatCurrency(statsQuery.data?.totalIncome ?? 0)}
              </p>
            </article>
            <article className={kpiCardClass}>
              <h3 className={kpiLabelClass}>Total Expense</h3>
              <p className="text-xl font-bold text-red-700">
                {formatCurrency(statsQuery.data?.totalExpense ?? 0)}
              </p>
            </article>
            <article className={kpiCardClass}>
              <h3 className={kpiLabelClass}>Net Balance</h3>
              <p className="text-xl font-bold">
                {formatCurrency(statsQuery.data?.netTotal ?? 0)}
              </p>
            </article>
            <article className={kpiCardClass}>
              <h3 className={kpiLabelClass}>Transactions</h3>
              <p className="text-xl font-bold">
                {statsQuery.data?.transactionCount ?? 0}
              </p>
            </article>
          </section>

          <section className="grid gap-4 lg:grid-cols-[minmax(280px,320px)_minmax(0,1fr)]">
            <aside className="grid content-start gap-4">
              <section className={`${panelClass} grid gap-3`}>
                <div className={panelHeadingClass}>
                  <h2 className={panelTitleClass}>Upload</h2>
                  <span className={panelKickerClass}>.xlsx / .csv</span>
                </div>
                <label
                  className={`grid min-h-24 cursor-pointer place-items-center rounded-lg border border-dashed p-4 text-center text-sm transition-colors ${
                    isDragOver
                      ? "border-gray-900 bg-neutral-100 text-gray-900"
                      : "border-gray-300 text-gray-500 hover:border-gray-900 hover:bg-neutral-100 hover:text-gray-900"
                  }`}
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
                  className="hidden"
                  type="file"
                  accept=".xlsx,.csv"
                  onChange={(event) => {
                    const file = event.target.files?.[0] ?? null;
                    setSelectedFile(file);
                    setUploadMessage("");
                  }}
                />
                <button
                  className={primaryButton}
                  disabled={!selectedFile || uploadMutation.isPending}
                  onClick={() => {
                    if (selectedFile) uploadMutation.mutate(selectedFile);
                  }}
                >
                  {uploadMutation.isPending ? "Uploading…" : "Upload"}
                </button>
                <button
                  className={secondaryButton}
                  disabled={parseMutation.isPending}
                  onClick={() => {
                    setUploadMessage("");
                    parseMutation.mutate();
                  }}
                >
                  {parseMutation.isPending
                    ? "Parsing…"
                    : "Parse local documents"}
                </button>
                {uploadMessage ? (
                  <p className="text-sm text-emerald-700">{uploadMessage}</p>
                ) : null}
              </section>

              <section className={panelClass}>
                <div className={panelHeadingClass}>
                  <h2 className={panelTitleClass}>Processing</h2>
                  <span className={panelKickerClass}>
                    {workerIsLive ? "Live" : "Idle"}
                  </span>
                </div>
                <dl className="grid gap-2">
                  <div className="flex justify-between gap-2 text-sm">
                    <dt className="text-gray-500">Queue depth</dt>
                    <dd className="font-medium">
                      {statusQuery.data?.queueDepth ?? 0}
                    </dd>
                  </div>
                  <div className="flex justify-between gap-2 text-sm">
                    <dt className="text-gray-500">Processed</dt>
                    <dd className="font-medium">
                      {statusQuery.data?.processedCount ?? 0}
                    </dd>
                  </div>
                  <div className="flex justify-between gap-2 text-sm">
                    <dt className="text-gray-500">Heartbeat</dt>
                    <dd className="font-medium">
                      {statusQuery.data?.workerHeartbeat ?? "—"}
                    </dd>
                  </div>
                </dl>
              </section>

              <section className={panelClass}>
                <div className={panelHeadingClass}>
                  <h2 className={panelTitleClass}>Top Categories</h2>
                  <span className={panelKickerClass}>{pieSegments.length}</span>
                </div>
                <div className="grid justify-items-center gap-4">
                  <div
                    className="relative aspect-square w-[min(160px,100%)] rounded-full"
                    style={{ background: pieBackground }}
                  >
                    <div className="absolute inset-[26%] grid place-content-center rounded-full bg-white text-center">
                      <span className="text-[0.65rem] uppercase tracking-[0.06em] text-gray-500">
                        Total
                      </span>
                      <strong className="text-xs">
                        {formatCurrency(
                          pieSegments.reduce((sum, s) => sum + s.value, 0),
                        )}
                      </strong>
                    </div>
                  </div>

                  <div className="grid max-h-[200px] w-full gap-1.5 overflow-auto">
                    {pieSegments.map((segment) => (
                      <div
                        key={segment.category}
                        className="grid grid-cols-[9px_1fr_auto] items-center gap-2 text-sm"
                      >
                        <span
                          className="h-[9px] w-[9px] rounded-full"
                          style={{ backgroundColor: segment.color }}
                        />
                        <span className="truncate">{segment.category}</span>
                        <span className="text-xs text-gray-500">
                          {Math.round(segment.percentage)}%
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </section>
            </aside>

            <section className={panelClass}>
              <div className="mb-3.5 flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <h2 className={panelTitleClass}>Transactions</h2>
                  <p className="mt-1 text-sm text-gray-500">
                    {transactionsQuery.data?.pagination.total ?? 0} records
                  </p>
                </div>
                <div className="flex gap-2 max-sm:w-full">
                  <input
                    className={`${inputClass} sm:w-[220px]`}
                    placeholder="Search description or recipient"
                    value={search}
                    onChange={(event) => {
                      setPageNo(0);
                      setSearch(event.target.value);
                    }}
                  />
                </div>
              </div>

              <div className="overflow-auto rounded-lg border border-gray-200">
                <table className="w-full min-w-[720px] border-collapse">
                  <thead>
                    <tr>
                      <th className={tableHeadCellClass}>Date</th>
                      <th className={tableHeadCellClass}>Description</th>
                      <th className={tableHeadCellClass}>Recipient</th>
                      <th className={tableHeadCellClass}>Category</th>
                      <th className={tableHeadCellClass}>Amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(transactionsQuery.data?.data ?? []).map((item) => (
                      <tr
                        key={item.id}
                        className="last:[&>td]:border-b-0 hover:bg-neutral-50"
                      >
                        <td className={tableCellClass}>
                          {new Date(item.createdAt).toLocaleDateString()}
                        </td>
                        <td className={tableCellClass}>{item.description}</td>
                        <td className={tableCellClass}>
                          {item.recipient ?? "—"}
                        </td>
                        <td className={tableCellClass}>
                          <span className="inline-block rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-500">
                            {item.category}
                          </span>
                        </td>
                        <td className={tableCellClass}>
                          <span
                            className={`font-semibold tabular-nums ${
                              item.amount < 0
                                ? "text-red-700"
                                : "text-emerald-700"
                            }`}
                          >
                            {formatCurrency(item.amount)}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="mt-3.5 flex items-center justify-between gap-4 text-sm text-gray-500">
                <button
                  className={secondaryButton}
                  disabled={currentPage <= 1}
                  onClick={() =>
                    setPageNo((previous) => Math.max(previous - 1, 0))
                  }
                >
                  ← Previous
                </button>
                <span>
                  Page {currentPage} of {totalPages}
                </span>
                <button
                  className={secondaryButton}
                  disabled={currentPage >= totalPages}
                  onClick={() => setPageNo((previous) => previous + 1)}
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
