import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, beforeEach } from "vitest";

/**
 * Node exposes an experimental global `localStorage` that resolves to `undefined` unless the
 * runtime is started with `--localstorage-file`, and that global shadows the one jsdom
 * provides. Installing a standard in-memory Storage keeps `window.localStorage` behaving like
 * a browser, which `src/lib/api.ts` relies on for the access token and the current user.
 */
class MemoryStorage implements Storage {
  private entries = new Map<string, string>();

  get length() {
    return this.entries.size;
  }

  clear() {
    this.entries.clear();
  }

  getItem(key: string) {
    return this.entries.has(key) ? (this.entries.get(key) as string) : null;
  }

  key(index: number) {
    return Array.from(this.entries.keys())[index] ?? null;
  }

  removeItem(key: string) {
    this.entries.delete(key);
  }

  setItem(key: string, value: string) {
    this.entries.set(key, String(value));
  }
}

function installStorage(name: "localStorage" | "sessionStorage") {
  if (window[name]) {
    return;
  }
  Object.defineProperty(window, name, {
    value: new MemoryStorage(),
    configurable: true,
    writable: false,
  });
}

installStorage("localStorage");
installStorage("sessionStorage");

beforeEach(() => {
  window.localStorage.clear();
  window.sessionStorage.clear();
});

afterEach(() => {
  cleanup();
});
