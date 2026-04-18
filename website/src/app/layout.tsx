import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

export const metadata: Metadata = {
  title: "SolWear — Your Solana Wallet on Your Wrist",
  description:
    "The world's first transparent smartwatch with a built-in Solana wallet. Built by a team of 3 at Frontier Hackathon.",
  keywords: ["solwear", "solana", "smartwatch", "crypto wallet", "wearable", "web3"],
  openGraph: {
    title: "SolWear — Your Solana Wallet on Your Wrist",
    description: "The world's first transparent smartwatch with a built-in Solana wallet.",
    type: "website",
    siteName: "SolWear",
  },
  twitter: {
    card: "summary_large_image",
    title: "SolWear",
    description: "The world's first transparent smartwatch with a built-in Solana wallet.",
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
        {/* Google tag (gtag.js) */}
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
      </head>
      <body className="bg-solwear-bg text-white antialiased">{children}</body>
    </html>
  );
}
