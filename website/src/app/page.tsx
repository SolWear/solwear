import Nav from "@/components/Nav";
import Hero from "@/components/Hero";
import Features from "@/components/Features";
import NfcDemo from "@/components/NfcDemo";
import SystemSection from "@/components/SystemSection";
import GamesRoadmap from "@/components/GamesRoadmap";
import WatchShowcase from "@/components/WatchShowcase";
import About from "@/components/About";
import Preorder from "@/components/Preorder";
import Links from "@/components/Links";
import Footer from "@/components/Footer";

export default function Home() {
  return (
    <>
      <Nav />
      <main>
        <Hero />
        <NfcDemo />
        <Features />
        <SystemSection />
        <WatchShowcase />
        <GamesRoadmap />
        <About />
        <Preorder />
        <Links />
      </main>
      <Footer />
    </>
  );
}
