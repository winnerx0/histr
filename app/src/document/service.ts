import XLSX from "xlsx";
import { DocumentModal } from "./model";
import { mkdir, readdir } from "node:fs/promises";
import path from "path";
import { redis } from "..";
import { config } from "../config";

const TRANSACTIONS_QUEUE = "transactions";

export class DocumentService {
  static async parseDocument(): Promise<
    DocumentModal["parseDocumentResponse"]
  > {
    const legacyDocumentsPath = path.resolve(process.cwd(), "documents");

    await mkdir(legacyDocumentsPath, { recursive: true });

    const workbookPaths = (await readdir(legacyDocumentsPath))
      .filter((file) => file.match(/\.(xlsx|csv)$/i))
      .map((file) => path.join(legacyDocumentsPath, file));

    const workbooks = workbookPaths.map((workbookPath) => XLSX.readFile(workbookPath));

    let queuedBatches = 0;

    for (const workbook of workbooks) {
      const queued = await this.queueWorkbook(workbook);
      queuedBatches += queued;
    }

    return {
      message: "Documents parsed successfully",
      queuedBatches,
    };
  }

  static async parseUploadedFile(
    file: File,
  ): Promise<DocumentModal["uploadResponse"]> {
    if (!file.name.match(/\.(xlsx|csv)$/i)) {
      throw new Error("Only .xlsx and .csv files are supported");
    }

    if (file.size > config.MAX_UPLOAD_SIZE_MB * 1024 * 1024) {
      throw new Error(
        `File exceeds ${config.MAX_UPLOAD_SIZE_MB}MB size limit`,
      );
    }

    const buffer = Buffer.from(await file.arrayBuffer());
    const workbook = XLSX.read(buffer, { type: "buffer" });
    const queuedBatches = await this.queueWorkbook(workbook);

    return {
      message: "File parsed and queued for processing",
      fileName: file.name,
      queuedBatches,
    };
  }

  private static async queueWorkbook(workbook: XLSX.WorkBook): Promise<number> {
    const sheetName = workbook.SheetNames[0];
    const sheet = workbook.Sheets[sheetName];

    if (!sheet) {
      return 0;
    }

    const data = XLSX.utils.sheet_to_json(sheet, { header: 1 }) as string[][];

    if (data.length === 0) {
      return 0;
    }

    await redis.client?.lpush(TRANSACTIONS_QUEUE, JSON.stringify(data));

    return 1;
  }
}
