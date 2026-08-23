import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppShell } from "./app-shell";
import { notificationKeys } from "@/lib/hooks/keys";

const replace = vi.fn();
let pathname = "/dashboard";

vi.mock("next/navigation", () => ({
  usePathname: () => pathname,
  useRouter: () => ({ replace, push: vi.fn(), back: vi.fn(), forward: vi.fn() }),
}));

function signIn(roles: string[]) {
  window.localStorage.setItem(
    "user",
    JSON.stringify({
      id: "11111111-1111-1111-1111-111111111111",
      organizationId: "22222222-2222-2222-2222-222222222222",
      departmentId: null,
      email: "officer@example.com",
      firstName: "Ada",
      lastName: "Lovelace",
      phone: null,
      active: true,
      roles,
      lastLoginAt: null,
      createdAt: "2026-01-01T00:00:00Z",
    }),
  );
}

function renderShell(unreadCount?: number) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, staleTime: Infinity } },
  });
  if (unreadCount !== undefined) {
    queryClient.setQueryData(notificationKeys.unreadCount(), { unreadCount });
  }

  return render(
    <QueryClientProvider client={queryClient}>
      <AppShell>
        <p>Screen content</p>
      </AppShell>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  replace.mockClear();
  pathname = "/dashboard";
});

describe("AppShell", () => {
  it("sends a visitor without a session to the sign-in screen instead of rendering chrome", () => {
    renderShell(0);

    expect(replace).toHaveBeenCalledWith("/login");
    expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
    expect(screen.queryByText("Screen content")).not.toBeInTheDocument();
  });

  it("renders the navigation landmark, the screen content and the signed-in user", () => {
    signIn(["PROCUREMENT_OFFICER"]);
    renderShell(0);

    expect(replace).not.toHaveBeenCalled();
    expect(screen.getByRole("navigation", { name: "Primary" })).toBeInTheDocument();
    expect(screen.getByRole("main")).toContainElement(screen.getByText("Screen content"));
    expect(screen.getByText("Ada Lovelace")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sign out" })).toBeInTheDocument();
  });

  it("marks the navigation item of the current area with aria-current", () => {
    signIn(["ADMIN"]);
    pathname = "/vendors/abc-123";
    renderShell(0);

    expect(screen.getByRole("link", { name: "Vendors" })).toHaveAttribute(
      "aria-current",
      "page",
    );
    expect(screen.getByRole("link", { name: "Dashboard" })).not.toHaveAttribute(
      "aria-current",
    );
  });

  it("hides navigation items the roles of the user cannot reach", () => {
    signIn(["REQUESTER"]);
    renderShell(0);

    expect(screen.getByRole("link", { name: "Purchase requests" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Invoices" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Audit logs" })).not.toBeInTheDocument();
  });

  it("names the notification bell with the unread count", () => {
    signIn(["FINANCE"]);
    renderShell(3);

    expect(
      screen.getByRole("link", { name: "Notifications, 3 unread notifications" }),
    ).toBeInTheDocument();
  });

  it("names the notification bell when nothing is unread", () => {
    signIn(["FINANCE"]);
    renderShell(0);

    expect(
      screen.getByRole("link", { name: "Notifications, no unread notifications" }),
    ).toBeInTheDocument();
  });
});
