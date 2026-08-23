import { AppShell } from "@/components/app-shell";

/**
 * Layout of the authenticated area. Every screen in the `(app)` group renders inside the
 * shell, which owns the sidebar navigation, the header and the notification bell.
 */
export default function AppLayout({ children }: LayoutProps<"/">) {
  return <AppShell>{children}</AppShell>;
}
