import StarLogo from "./ui/StarLogo";

export default function Footer() {
  return (
    <footer className="border-t border-white/10 py-10 px-6">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-6">
        <div className="flex items-center gap-2 text-white/80">
          <StarLogo size={16} />
          <span className="text-sm font-semibold tracking-[0.15em] lowercase">solwear</span>
        </div>

        <p className="label-caps text-center">Portable. Light. Always with you.</p>

        <p className="label-caps">© 2026 SolWear</p>
      </div>
    </footer>
  );
}
