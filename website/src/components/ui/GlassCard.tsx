"use client";
import { motion } from "framer-motion";
import { ReactNode } from "react";

interface GlassCardProps {
  children: ReactNode;
  className?: string;
  href?: string;
  delay?: number;
}

export default function GlassCard({ children, className = "", href, delay = 0 }: GlassCardProps) {
  const base =
    "glass transition-all duration-300 hover:border-white/25 hover:bg-white/[0.08] " +
    className;

  const content = (
    <motion.div
      className={base}
      initial={{ opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-60px" }}
      transition={{ duration: 0.5, delay, ease: "easeOut" }}
    >
      {children}
    </motion.div>
  );

  if (href) {
    return (
      <a href={href} target="_blank" rel="noopener noreferrer" className="group block">
        {content}
      </a>
    );
  }
  return content;
}
