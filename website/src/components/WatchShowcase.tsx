"use client";
import { motion } from "framer-motion";
import Image from "next/image";
import AnimatedHeading from "./ui/AnimatedHeading";

export default function WatchShowcase() {
  const watches = [
    { src: "/solwear/watch-smoked.png", alt: "Smoked Transparent", name: "solwear", desc: "Smoked" },
    { src: "/solwear/watch-b.png", alt: "Variant B", name: "solwear", desc: "Edition B" },
    { src: "/solwear/watch-g.png", alt: "Variant G", name: "solwear", desc: "Edition G" },
    { src: "/solwear/watch-m.png", alt: "Variant M", name: "solwear", desc: "Edition M" },
    { src: "/solwear/watch-v.png", alt: "Variant V", name: "solwear", desc: "Edition V" }
  ];

  return (
    <section className="py-28 px-6 overflow-hidden">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16">
          <p className="label-caps mb-3">variants</p>
          <AnimatedHeading
            as="h2"
            className="text-3xl md:text-5xl font-bold tracking-tight"
            muted="One Purpose."
          >
            Multiple Faces.
          </AnimatedHeading>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-10">
          {watches.map((watch, i) => (
            <motion.div
              key={i}
              className="flex flex-col items-center gap-6"
              initial={{ opacity: 0, y: 40 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: "-80px" }}
              transition={{ duration: 0.6, delay: i * 0.1 }}
            >
              <div className="glass p-10 flex justify-center items-center w-full min-h-[350px]">
                <Image
                  src={watch.src}
                  alt={watch.alt}
                  width={220}
                  height={275}
                  className="drop-shadow-2xl object-contain max-h-full"
                />
              </div>
              <div className="text-center">
                <p className="text-white font-medium mb-1">{watch.name}</p>
                <p className="label-caps">{watch.desc}</p>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
