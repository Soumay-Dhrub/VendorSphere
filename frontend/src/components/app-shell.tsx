"use client";

import { LogOut } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { isCurrentNavItem, navItemsForRoles } from "@/components/app-nav";
import { NotificationBell } from "@/components/notification-bell";
import { Button } from "@/components/ui/button";
import { logout } from "@/lib/api";
import { playClick, playNotificationPop, setSoundsEnabled, soundsEnabled } from "@/lib/sound";
import { useStoredUser } from "@/lib/hooks/auth";

export function AppShell({ children }: { children: React.ReactNode }) {
  const user = useStoredUser();
  const pathname = usePathname();
  const router = useRouter();
  const queryClient = useQueryClient();
  const [soundOn, setSoundOn] = useState(true);
  const [light, setLight] = useState(false);

  // Restore persisted preferences and play a pop when unread notifications arrive.
  useEffect(() => {
    try {
      setSoundOn(localStorage.getItem("vs-sounds") !== "off");
      const light = localStorage.getItem("vs-theme") === "light";
      setLight(light);
      document.documentElement.classList.toggle("light", light);
    } catch {}
  }, []);

  useEffect(() => {
    if (user === null) {
      router.replace("/login");
    }
  }, [user, router]);

  if (user === undefined) {
    return (
      <p role="status" className="p-8 text-sm text-slate-400">
        Loading your workspace…
      </p>
    );
  }

  // Signed out: the effect above is navigating to /login, so no chrome is rendered.
  if (user === null) {
    return null;
  }

  const navItems = navItemsForRoles(user.roles);

  async function handleSignOut() {
    playClick();
    await logout();
    // Server state belongs to the user who just left.
    queryClient.clear();
    router.replace("/login");
  }

  return (
    <div className="flex min-h-full flex-1 flex-col md:flex-row">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-50 focus:rounded-md focus:bg-emerald-500 focus:px-3 focus:py-2 focus:text-sm focus:font-medium focus:text-slate-950"
      >
        Skip to main content
      </a>

      <aside className="shrink-0 border-b border-slate-800 bg-slate-950 md:w-64 md:border-r md:border-b-0">
        <div className="flex items-center gap-3 px-4 py-4">
          <span
            aria-hidden="true"
            className="flex size-9 items-center justify-center rounded-xl bg-emerald-500/15 text-sm font-semibold text-emerald-400 ring-1 ring-emerald-500/30"
          >
            VS
          </span>
          <span className="text-sm font-semibold tracking-tight text-white">
            VendorSphere
          </span>
        </div>

        <nav aria-label="Primary" className="px-2 pb-3">
          <ul className="flex gap-1 overflow-x-auto md:flex-col md:overflow-x-visible">
            {navItems.map((item) => {
              const current = isCurrentNavItem(item.href, pathname);
              const Icon = item.icon;
              return (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    aria-current={current ? "page" : undefined}
                    className={`flex items-center gap-2.5 rounded-md px-3 py-2 text-sm whitespace-nowrap transition-colors focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none ${
                      current
                        ? "bg-slate-800 font-medium text-white"
                        : "text-slate-300 hover:bg-slate-900 hover:text-white"
                    }`}
                  >
                    <Icon className="size-4 shrink-0" aria-hidden={true} />
                    {item.label}
                  </Link>
                </li>
              );
            })}
          </ul>
        </nav>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="border-b border-slate-800 bg-slate-950/80 backdrop-blur">
          <div className="flex items-center justify-between gap-4 px-4 py-3 sm:px-6">
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-white">
                {user.firstName} {user.lastName}
              </p>
              <p className="truncate text-xs text-slate-400">
                {user.roles.length > 0 ? user.roles.join(", ") : user.email}
              </p>
            </div>
            <div className="flex items-center gap-2">
              <NotificationBell />
              <Button
                variant="outline"
                size="sm"
                aria-label="Toggle sound effects"
                title="Toggle sound effects"
                onClick={() => {
                  const next = !soundsEnabled();
                  setSoundsEnabled(next);
                  if (next) playClick();
                  setSoundOn(next);
                }}
              >
                {soundOn ? "🔊" : "🔇"}
              </Button>
              <Button
                variant="outline"
                size="sm"
                aria-label="Toggle light mode"
                title="Toggle light mode"
                onClick={() => {
                  playClick();
                  const root = document.documentElement;
                  const light = root.classList.toggle("light");
                  try {
                    localStorage.setItem("vs-theme", light ? "light" : "dark");
                  } catch {}
                  setLight(light);
                }}
              >
                {light ? "☀️" : "🌙"}
              </Button>
              <Button variant="outline" size="sm" onClick={handleSignOut}>
                <LogOut aria-hidden="true" />
                Sign out
              </Button>
            </div>
          </div>
        </header>

        <main id="main-content" className="flex-1 px-4 py-8 sm:px-6">
          {children}
        </main>
      </div>
    </div>
  );
}
