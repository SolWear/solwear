/** @type {import('next').NextConfig} */
const isStaticExport = process.env.SOLWEAR_STATIC_EXPORT === '1';

const nextConfig = {
  ...(isStaticExport ? { output: 'export' } : {}),
  images: {
    unoptimized: true,
  },
  trailingSlash: true,
  ...(!isStaticExport ? {
    async headers() {
      return [
      {
        source: '/(.*)',
        headers: [
          { key: 'X-Frame-Options', value: 'DENY' },
          { key: 'X-Content-Type-Options', value: 'nosniff' },
          { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
          { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=()' },
          {
            key: 'Content-Security-Policy',
            value: [
              "default-src 'self'",
              "script-src 'self' 'unsafe-inline' https://www.googletagmanager.com https://www.google-analytics.com",
              "style-src 'self' 'unsafe-inline'",
              "img-src 'self' data: https:",
              "connect-src 'self' https://api.x.com https://www.google-analytics.com",
              "frame-ancestors 'none'",
              "base-uri 'self'",
              "form-action 'self'",
            ].join('; '),
          },
        ],
      },
      ];
    },
  } : {}),
};

export default nextConfig;
