"use client";

import { motion, useReducedMotion } from "framer-motion";
import Image from "next/image";
import RedDot from "./ui/RedDot";

export default function Hero() {
  const reduceMotion = useReducedMotion();

  return (
    <section id="watch" className="relative min-h-[92vh] overflow-hidden bg-solwear-bg">
      <Image
        src="/sticker.webp"
        alt="Transparent SolWear prototype watches"
        fill
        priority
        sizes="100vw"
        className="object-contain object-[72%_52%] opacity-70 md:object-right"
      />
      <div className="absolute inset-0 bg-[linear-gradient(90deg,#050505_0%,rgba(5,5,5,0.92)_34%,rgba(5,5,5,0.56)_68%,rgba(5,5,5,0.22)_100%)]" />
      <div className="absolute inset-x-0 bottom-0 h-40 bg-gradient-to-t from-solwear-bg to-transparent" />

      <div className="relative mx-auto flex min-h-[92vh] max-w-7xl flex-col justify-center px-6 pb-24 pt-28">
        <motion.div
          initial={reduceMotion ? false : { opacity: 0, y: 16 }}
          animate={reduceMotion ? undefined : { opacity: 1, y: 0 }}
          transition={{ duration: 0.35, ease: "easeOut" }}
          className="max-w-3xl"
        >
          <div className="mb-7 flex items-center gap-3">
            <RedDot size={8} />
            <span className="label-caps text-white/50">Solana wallet on your wrist</span>
          </div>

          <h1 className="hero-text text-5xl uppercase text-white md:text-7xl lg:text-8xl">
            SolWear
          </h1>
          <p className="mt-6 max-w-xl text-base leading-7 text-white/68 md:text-lg">
            A transparent smartwatch wallet for Solana. Tap the phone, wake the watch,
            show the sync effect, and bring the wallet into the mobile app.
          </p>

          <div className="mt-9 flex flex-col gap-3 sm:flex-row">
            <button
              onClick={() => document.querySelector("#nfc")?.scrollIntoView({ behavior: "smooth" })}
              className="focus-ring min-h-11 bg-white px-6 py-3 text-sm font-semibold text-black transition-colors hover:bg-white/90"
            >
              See NFC sync
            </button>
            <button
              onClick={() => document.querySelector("#purchase")?.scrollIntoView({ behavior: "smooth" })}
              className="focus-ring min-h-11 border border-white/20 px-6 py-3 text-sm font-semibold text-white/80 transition-colors hover:border-white/45 hover:text-white"
            >
              Join updates
            </button>
          </div>
        </motion.div>

        <div className="absolute bottom-6 left-6 right-6 grid gap-3 border-t border-white/10 pt-5 text-xs text-white/55 md:grid-cols-3">
          <span>PN532 over I2C</span>
          <span>ESP32-S3 SolWearOS</span>
          <span>Android NFC companion</span>
        </div>
      </div>
    </section>
  );
}
