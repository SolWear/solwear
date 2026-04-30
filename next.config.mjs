/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export',
  basePath: '/solwear',
  assetPrefix: '/solwear',
  images: {
    unoptimized: true,
  },
  trailingSlash: true,
};

export default nextConfig;
