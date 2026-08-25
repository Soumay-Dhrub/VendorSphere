import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import { Providers } from "@/components/providers";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "VendorSphere | B2B Procurement Management",
  description:
    "Centralized procurement platform for vendors, RFQs, quotations, purchase orders, deliveries, and invoices.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="en"
      suppressHydrationWarning
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body
      suppressHydrationWarning
      className="min-h-full flex flex-col bg-slate-950 text-slate-100"
    >
        <script
          dangerouslySetInnerHTML={{
            __html: `try{if(localStorage.getItem("vs-theme")==="light"){document.documentElement.classList.add("light");document.body.classList.add("light")}}catch(e){}`,
          }}
        />
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
