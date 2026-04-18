"use client";
import { motion } from "framer-motion";
import { ReactNode } from "react";

type Props = {
  children: string;
  className?: string;
  as?: "h1" | "h2" | "h3";
  muted?: string;
};

export default function AnimatedHeading({ children, className = "", as = "h2", muted }: Props) {
  const words = children.split(" ");
  const mutedWords = muted ? muted.split(" ") : [];

  const container = {
    hidden: {},
    visible: { transition: { staggerChildren: 0.08 } },
  };

  const item = {
    hidden: { opacity: 0, y: 24, filter: "blur(6px)" },
    visible: {
      opacity: 1,
      y: 0,
      filter: "blur(0px)",
      transition: { duration: 0.6, ease: [0.2, 0.8, 0.2, 1] as [number, number, number, number] },
    },
  };

  const Tag = motion[as] as typeof motion.h2;

  return (
    <Tag
      className={className}
      initial="hidden"
      whileInView="visible"
      viewport={{ once: true, margin: "-80px" }}
      variants={container}
    >
      {words.map((w, i) => (
        <motion.span key={i} className="inline-block mr-3" variants={item}>
          {w}
        </motion.span>
      ))}
      {mutedWords.length > 0 && (
        <span className="text-white/30">
          {mutedWords.map((w, i) => (
            <motion.span key={i} className="inline-block mr-3" variants={item}>
              {w}
            </motion.span>
          ))}
        </span>
      )}
    </Tag>
  );
}
