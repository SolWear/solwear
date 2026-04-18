"use client";
import { useState } from "react";
import { motion } from "framer-motion";
import RedDot from "./ui/RedDot";
import AnimatedHeading from "./ui/AnimatedHeading";

export default function Preorder() {
  const [submitted, setSubmitted] = useState(false);
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setPending(true);
    await new Promise((r) => setTimeout(r, 400));
    setSubmitted(true);
    setPending(false);
  };

  return (
    <section id="purchase" className="py-28 px-6">
      <div className="max-w-3xl mx-auto text-center">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
        >
          <div className="flex items-center justify-center gap-3 mb-6">
            <RedDot size={10} />
            <p className="label-caps">pre-order</p>
            <RedDot size={10} />
          </div>

          <AnimatedHeading
            as="h2"
            className="text-4xl md:text-6xl font-bold tracking-tight mb-4"
          >
            Own the Future of Crypto
          </AnimatedHeading>

          <p className="text-[5rem] font-bold text-white/10 leading-none mb-6 select-none">
            $—
          </p>

          <p className="text-white/50 text-base mb-12 max-w-md mx-auto">
            Pre-orders open after Frontier Hackathon results are announced.
            Be the first to know.
          </p>

          {submitted ? (
            <motion.div
              className="glass px-8 py-6 text-white/70 text-sm max-w-sm mx-auto"
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
            >
              <RedDot size={8} className="mx-auto mb-3" />
              You&apos;re on the list. We&apos;ll reach out as soon as results are in.
            </motion.div>
          ) : (
            <form
              onSubmit={handleSubmit}
              className="flex flex-col sm:flex-row gap-3 max-w-sm mx-auto"
            >
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="your@email.com"
                className="flex-1 bg-white/5 border border-white/15 text-white placeholder-white/30 px-4 py-3 text-sm outline-none focus:border-white/40 transition-colors"
              />
              <button
                type="submit"
                disabled={pending}
                className="px-6 py-3 bg-white text-black text-sm font-semibold tracking-wider hover:bg-white/90 transition-colors whitespace-nowrap disabled:opacity-60"
              >
                {pending ? "SUBMITTING…" : "NOTIFY ME"}
              </button>
            </form>
          )}
          {error && (
            <p className="mt-4 text-xs text-[#e0000f]">{error}</p>
          )}
        </motion.div>
      </div>
    </section>
  );
}
