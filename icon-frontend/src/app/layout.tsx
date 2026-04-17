"use client";

import { authAtom } from "@/atoms/authAtom";
import Layout from "@/components/Layout";
import UserProvider from "@/components/UserProvider";
import { useAtom } from "jotai";
import { Geist, Geist_Mono } from "next/font/google";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Toaster } from "react-hot-toast";
import "./globals.css";
import { Providers } from "./providers";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const pathname = usePathname();
  const router = useRouter();
  const [auth] = useAtom(authAtom);
  const [hydrated, setHydrated] = useState(false); // 인증 전까지 체크/리다이렉트 금지

  useEffect(() => {
    setHydrated(true);
  }, []);

  const isLoginPage = pathname === "/login";

  useEffect(() => {
    if (!hydrated) return; // hydration 끝나기 전엔 아무것도 하지 않음
    if (!auth.isAuthenticated && !isLoginPage) {
      router.push("/login");
    }
  }, [hydrated, auth.isAuthenticated, isLoginPage, router]);

  return (
    <html lang="ko">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased text-gray-900`}
        suppressHydrationWarning
      >
        <Providers>
          <UserProvider>
            {isLoginPage ? children : <Layout>{children}</Layout>}
          </UserProvider>
          <Toaster position="top-center" />
        </Providers>
      </body>
    </html>
  );
}
