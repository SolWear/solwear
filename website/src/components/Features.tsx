"use client";
import GlassCard from "./ui/GlassCard";
import AnimatedHeading from "./ui/AnimatedHeading";

const features = [
  {
    icon: (
      <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <rect x="2" y="7" width="20" height="14" rx="2" />
        <path d="M6 7V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v2" />
        <circle cx="12" cy="14" r="2" />
        <path d="M12 12v-2M12 16v1" />
      </svg>
    ),
    title: "VISIBLE TECH",
    body: "See through. Know everything. The transparent case puts technology on display, not hidden behind it.",
  },
  {
    icon: (
      <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="7" height="7" rx="1" />
      </svg>
    ),
    title: "E-INK ICONS",
    body: "Ultra-low power. Always-on display. Crisp icons visible in direct sunlight — battery lasts weeks.",
  },
  {
    icon: (
      <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
        <rect x="5" y="2" width="14" height="20" rx="2" />
        <path d="M12 18h.01" />
        <path d="M19 10l3 2-3 2" />
        <path d="M22 12h-7" />
      </svg>
    ),
    title: "MOBILE COMPANION",
    body: "Sync, sign, send — from your wrist. The companion app bridges Solana to your daily life.",
  },
];

export default function Features() {
  return (
    <section className="py-28 px-6">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <p className="label-caps mb-3">capabilities</p>
          <AnimatedHeading
            as="h2"
            className="text-3xl md:text-5xl font-bold tracking-tight"
            muted="Built On-Chain."
          >
            Built Different.
          </AnimatedHeading>
        </div>

        <div className="grid md:grid-cols-3 gap-5">
          {features.map((f, i) => (
            <GlassCard key={f.title} className="p-8" delay={i * 0.1}>
              <div className="text-white/60 mb-5">{f.icon}</div>
              <h3 className="label-caps text-white mb-3">{f.title}</h3>
              <p className="text-white/50 text-sm leading-relaxed">{f.body}</p>
            </GlassCard>
          ))}
        </div>
      </div>
    </section>
  );
}
