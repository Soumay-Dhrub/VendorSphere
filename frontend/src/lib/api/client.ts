
import axios, { type AxiosRequestConfig } from "axios";
import { apiClient, type ApiResponse } from "@/lib/api";
import type { PageParams, PageResponse } from "./types";

const FALLBACK_MESSAGE = "An unexpected error occurred";

export class ApiError extends Error {
  readonly status?: number;

  readonly fieldErrors?: Record<string, string>;

  constructor(
    message: string,
    status?: number,
    fieldErrors?: Record<string, string>,
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

export function unwrap<T>(envelope: ApiResponse<T> | null | undefined): T {
  assertSuccess(envelope);
  const data = envelope.data;
  if (data === null || data === undefined) {
    throw new ApiError(envelope.message ?? "Response carried no data");
  }
  return data;
}

export function unwrapEmpty(
  envelope: ApiResponse<unknown> | null | undefined,
): void {
  assertSuccess(envelope);
}

function assertSuccess<T>(
  envelope: ApiResponse<T> | null | undefined,
): asserts envelope is ApiResponse<T> {
  if (!envelope || envelope.success !== true) {
    throw new ApiError(envelope?.message ?? FALLBACK_MESSAGE);
  }
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) {
    return error;
  }
  if (axios.isAxiosError(error)) {
    const envelope = error.response?.data as ApiResponse<unknown> | undefined;
    return new ApiError(
      envelope?.message ?? error.message ?? FALLBACK_MESSAGE,
      error.response?.status,
      fieldErrorsOf(envelope),
    );
  }
  if (error instanceof Error) {
    return new ApiError(error.message);
  }
  return new ApiError(FALLBACK_MESSAGE);
}

function fieldErrorsOf(
  envelope: ApiResponse<unknown> | undefined,
): Record<string, string> | undefined {
  const data = envelope?.data;
  if (!data || typeof data !== "object" || Array.isArray(data)) {
    return undefined;
  }
  const entries = Object.entries(data as Record<string, unknown>).filter(
    (entry): entry is [string, string] => typeof entry[1] === "string",
  );
  return entries.length > 0 ? Object.fromEntries(entries) : undefined;
}

export function queryParams(
  input: Record<string, unknown> | undefined,
): Record<string, string | number | boolean> {
  const output: Record<string, string | number | boolean> = {};
  for (const [key, value] of Object.entries(input ?? {})) {
    if (value === undefined || value === null) continue;
    if (typeof value === "string") {
      const trimmed = value.trim();
      if (trimmed.length === 0) continue;
      output[key] = trimmed;
      continue;
    }
    if (typeof value === "number") {
      if (!Number.isFinite(value)) continue;
      output[key] = value;
      continue;
    }
    if (typeof value === "boolean") {
      output[key] = value;
    }
  }
  return output;
}

async function send<T>(config: AxiosRequestConfig): Promise<ApiResponse<T>> {
  try {
    const response = await apiClient.request<ApiResponse<T>>(config);
    return response.data;
  } catch (error) {
    throw toApiError(error);
  }
}

export async function apiGet<T>(
  url: string,
  params?: Record<string, unknown>,
): Promise<T> {
  return unwrap(await send<T>({ method: "GET", url, params: queryParams(params) }));
}

export async function apiGetPage<T>(
  url: string,
  params?: PageParams & Record<string, unknown>,
): Promise<PageResponse<T>> {
  return apiGet<PageResponse<T>>(url, params);
}

export async function apiPost<T>(url: string, body?: unknown): Promise<T> {
  return unwrap(await send<T>({ method: "POST", url, data: body }));
}

export async function apiPut<T>(url: string, body?: unknown): Promise<T> {
  return unwrap(await send<T>({ method: "PUT", url, data: body }));
}

export async function apiPatch<T>(url: string, body?: unknown): Promise<T> {
  return unwrap(await send<T>({ method: "PATCH", url, data: body }));
}

export async function apiPostEmpty(url: string, body?: unknown): Promise<void> {
  unwrapEmpty(await send<unknown>({ method: "POST", url, data: body }));
}

export async function apiPatchEmpty(url: string, body?: unknown): Promise<void> {
  unwrapEmpty(await send<unknown>({ method: "PATCH", url, data: body }));
}

export async function apiDelete(url: string): Promise<void> {
  unwrapEmpty(await send<unknown>({ method: "DELETE", url }));
}

export async function apiPostForm<T>(url: string, form: FormData): Promise<T> {
  return unwrap(
    await send<T>({
      method: "POST",
      url,
      data: form,
      headers: { "Content-Type": undefined },
    }),
  );
}

export function fileForm(
  file: File,
  fields?: Record<string, string | number | undefined | null>,
  fileField = "file",
): FormData {
  const form = new FormData();
  form.append(fileField, file);
  for (const [key, value] of Object.entries(fields ?? {})) {
    if (value === undefined || value === null) continue;
    form.append(key, String(value));
  }
  return form;
}
