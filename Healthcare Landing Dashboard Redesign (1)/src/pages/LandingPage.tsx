import { Hero } from "../components/Hero";
import { RoleCards } from "../components/RoleCards";
import { HowItWorks } from "../components/HowItWorks";
import { FeaturesGrid } from "../components/FeaturesGrid";
import { ProductShowcase } from "../components/ProductShowcase";
import { Testimonials } from "../components/Testimonials";
import { CTASection } from "../components/CTASection";
import { Footer } from "../components/Footer";

export function LandingPage() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 via-white to-blue-50">
      <Hero />
      <RoleCards />
      <HowItWorks />
      <FeaturesGrid />
      <ProductShowcase />
      <Testimonials />
      <CTASection />
      <Footer />
    </div>
  );
}
