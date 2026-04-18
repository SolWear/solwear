"use client";
import { motion } from "framer-motion";
import AnimatedHeading from "./ui/AnimatedHeading";

const stats = [
  { value: "3", label: "builders" },
  { value: "5", label: "repos" },
  { value: "1", label: "working prototype" },
];

export default function About() {
  return (
    <section id="explore" className="py-28 px-6">
      <div className="max-w-4xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
        >
          <p className="label-caps mb-4">origin</p>
          <AnimatedHeading
            as="h2"
            className="text-3xl md:text-5xl font-bold tracking-tight mb-8"
            muted="Shipping to the World."
          >
            Built in Ukraine.
          </AnimatedHeading>
          <p className="text-white/50 text-lg leading-relaxed mb-10 max-w-2xl">
            Three engineers — hardware, firmware, and app — built SolWear in days for
            the Frontier Hackathon. One goal: put a Solana wallet on your wrist without
            sacrificing the look. A transparent body, an e-ink always-on display, and a
            companion app that signs transactions in seconds. Not a mockup. A real,
            working prototype.
          </p>

          {/* Stat chips */}
          <div className="flex flex-wrap gap-3">
            {stats.map((s, i) => (
              <motion.div
                key={s.label}
                className="glass px-5 py-3 flex items-baseline gap-2"
                initial={{ opacity: 0, scale: 0.9 }}
                whileInView={{ opacity: 1, scale: 1 }}
                viewport={{ once: true }}
                transition={{ duration: 0.4, delay: i * 0.1 }}
              >
                <span className="text-2xl font-bold text-white">{s.value}</span>
                <span className="label-caps">{s.label}</span>
              </motion.div>
            ))}
          </div>
        </motion.div>
      </div>
    </section>
  );
}
