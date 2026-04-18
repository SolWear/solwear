"use client";
import GlassCard from "./ui/GlassCard";
import AnimatedHeading from "./ui/AnimatedHeading";

const socials = [
  {
    title: "Twitter / X",
    desc: "Build updates. Shipping log. Dev notes.",
    href: "https://x.com/SolWear_",
    tag: "@SolWear_",
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
        <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
      </svg>
    ),
  },
  {
    title: "Instagram",
    desc: "Hardware photos. Behind-the-scenes.",
    href: "https://instagram.com/so1wear",
    tag: "@so1wear",
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.6">
        <rect x="3" y="3" width="18" height="18" rx="5" />
        <circle cx="12" cy="12" r="4" />
        <circle cx="17.5" cy="6.5" r="1.2" fill="currentColor" />
      </svg>
    ),
  },
  {
    title: "TikTok",
    desc: "The watch in motion. Short cuts, raw footage.",
    href: "https://tiktok.com/@solwear",
    tag: "@solwear",
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor">
        <path d="M19.59 6.69a4.83 4.83 0 0 1-3.77-4.25V2h-3.45v13.67a2.89 2.89 0 0 1-5.2 1.74 2.89 2.89 0 0 1 2.31-4.64 2.93 2.93 0 0 1 .88.13V9.4a6.84 6.84 0 0 0-1-.05A6.33 6.33 0 0 0 5.8 20.1a6.34 6.34 0 0 0 10.86-4.43v-7a8.16 8.16 0 0 0 4.77 1.52v-3.4a4.85 4.85 0 0 1-1.84-.1z" />
      </svg>
    ),
  },
];

const Arrow = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.4">
    <path d="M2 12 L12 2 M5 2 H12 V9" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

export default function Links() {
  return (
    <section id="app" className="py-28 px-6">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <p className="label-caps mb-3">community</p>
          <AnimatedHeading
            as="h2"
            className="text-3xl md:text-5xl font-bold tracking-tight"
          >
            Follow the Build.
          </AnimatedHeading>
        </div>

        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {socials.map((s, i) => (
            <GlassCard key={s.title} className="p-6" href={s.href} delay={i * 0.08}>
              <div className="flex items-start justify-between mb-4">
                <span className="text-white/60 group-hover:text-white transition-colors">
                  {s.icon}
                </span>
                <span className="text-white/40 group-hover:text-white/80 transition-colors">
                  <Arrow />
                </span>
              </div>
              <h3 className="text-white font-semibold mb-1">{s.title}</h3>
              <p className="label-caps mb-2">{s.tag}</p>
              <p className="text-white/40 text-xs leading-relaxed">{s.desc}</p>
            </GlassCard>
          ))}
        </div>
      </div>
    </section>
  );
}
