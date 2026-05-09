import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

export const metadata: Metadata = {
  title: "SolWear - Your Solana Wallet on Your Wrist",
  description:
    "A transparent smartwatch with a built-in Solana wallet, SolWearOS, Android NFC sync, and a live community pinboard.",
  keywords: ["solwear", "solana", "smartwatch", "crypto wallet", "wearable", "web3"],
  metadataBase: new URL("https://solwear.tech"),
  openGraph: {
    title: "SolWear - Your Solana Wallet on Your Wrist",
    description: "A transparent smartwatch wallet for Solana with NFC sync.",
    type: "website",
    siteName: "SolWear",
    url: "https://solwear.tech",
  },
  twitter: {
    card: "summary_large_image",
    title: "SolWear",
    description: "A transparent smartwatch wallet for Solana with NFC sync.",
  },
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className={inter.variable}>
      <head>
        {process.env.NODE_ENV === "production" && (
          <>
            <script async src="https://www.googletagmanager.com/gtag/js?id=G-TVB92JGYXE"></script>
            <script
              dangerouslySetInnerHTML={{
                __html: `
                  window.dataLayer = window.dataLayer || [];
                  function gtag(){dataLayer.push(arguments);}
                  gtag('js', new Date());
                  gtag('config', 'G-TVB92JGYXE');
                `,
              }}
            />
          </>
        )}
      </head>
      <body className="bg-solwear-bg text-white antialiased">{children}</body>
    </html>
  );
}
