"use client";

import GlassCard from "./ui/GlassCard";
import AnimatedHeading from "./ui/AnimatedHeading";

const features = [
  {
    title: "Tap effect first",
    body: "The MVP treats NFC contact as the hero moment: watch and phone both react before wallet data is shown.",
  },
  {
    title: "Wallet on-device",
    body: "SolWearOS keeps the signer on the watch while the Android app handles balance, network, and transaction UX.",
  },
  {
    title: "Black and white",
    body: "The canonical SolWear look is stripped back: black shell, white type, red status dot, tiny Solana accents.",
  },
];

export default function Features() {
  return (
    <section className="px-6 py-24">
      <div className="mx-auto max-w-7xl">
        <div className="mb-12 max-w-2xl">
          <p className="label-caps mb-3">mvp focus</p>
          <AnimatedHeading as="h2" className="text-3xl font-bold md:text-5xl">
            Make the wrist tap undeniable.
          </AnimatedHeading>
        </div>

        <div className="grid gap-5 md:grid-cols-3">
          {features.map((feature, i) => (
            <GlassCard key={feature.title} className="p-6" delay={i * 0.05}>
              <div className="mb-5 h-1 w-12 bg-white" />
              <h3 className="mb-3 text-lg font-semibold text-white">{feature.title}</h3>
              <p className="text-sm leading-6 text-white/55">{feature.body}</p>
            </GlassCard>
          ))}
        </div>
      </div>
    </section>
  );
}
