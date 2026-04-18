interface RedDotProps {
  size?: number;
  className?: string;
}

export default function RedDot({ size = 10, className = "" }: RedDotProps) {
  return (
    <span
      className={`inline-block rounded-full bg-[#e0000f] animate-pulse-red ${className}`}
      style={{ width: size, height: size, flexShrink: 0 }}
      aria-hidden="true"
    />
  );
}
