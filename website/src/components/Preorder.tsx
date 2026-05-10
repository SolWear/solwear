"use client";

import { FormEvent, useState } from "react";
import { motion } from "framer-motion";
import RedDot from "./ui/RedDot";
import AnimatedHeading from "./ui/AnimatedHeading";

export default function Preorder() {
  const [submitted, setSubmitted] = useState(false);
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const dynamicEnabled = process.env.NEXT_PUBLIC_SOLWEAR_DYNAMIC === "1";

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);

    if (!dynamicEnabled) {
      setError("Live signup runs on the Docker site. For now, follow SolWear on X for launch updates.");
      return;
    }

    setPending(true);
    try {
      const res = await fetch("/api/notify", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email }),
      });
      if (!res.ok) throw new Error("Failed to subscribe");
      setSubmitted(true);
    } catch {
      setError("Something went wrong. Please try again.");
    } finally {
      setPending(false);
    }
  };

  return (
    <section id="purchase" className="px-6 py-28">
      <div className="mx-auto max-w-3xl text-center">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.35, ease: "easeOut" }}
        >
          <div className="mb-6 flex items-center justify-center gap-3">
            <RedDot size={10} />
            <p className="label-caps">updates</p>
            <RedDot size={10} />
          </div>

          <AnimatedHeading as="h2" className="mb-4 text-4xl font-bold md:text-6xl">
            Build log, launch notes, and prototype drops.
          </AnimatedHeading>

          <p className="mx-auto mb-12 max-w-md text-base text-white/50">
            Join the SolWear update list or follow the public build on X.
          </p>

          {submitted ? (
            <motion.div
              className="glass mx-auto max-w-sm px-8 py-6 text-sm text-white/70"
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
            >
              <RedDot size={8} className="mx-auto mb-3" />
              You are on the list.
            </motion.div>
          ) : (
            <form onSubmit={handleSubmit} className="mx-auto max-w-sm text-left">
              <label htmlFor="notify-email" className="mb-2 block text-xs font-semibold text-white/70">
                Email updates
              </label>
              <div className="flex flex-col gap-3 sm:flex-row">
                <input
                  id="notify-email"
                  type="email"
                  autoComplete="email"
                  spellCheck={false}
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  className="min-h-11 flex-1 border border-white/15 bg-white/5 px-4 py-3 text-sm text-white outline-none transition-colors placeholder:text-white/30 focus:border-white/40"
                />
                <button
                  type="submit"
                  disabled={pending}
                  className="focus-ring min-h-11 bg-white px-6 py-3 text-sm font-semibold text-black transition-colors hover:bg-white/90 disabled:opacity-60"
                >
                  {pending ? "Submitting..." : "Notify me"}
                </button>
              </div>
            </form>
          )}
          {error && <p className="mt-4 text-xs text-solwear-red">{error}</p>}
        </motion.div>
      </div>
    </section>
  );
}
