import { Button } from "./ui/button";
import { ArrowRight, Calendar, Shield, Video } from "lucide-react";

export function CTASection() {
  return (
    <section className="py-20 lg:py-28 relative overflow-hidden">
      {/* Background gradients */}
      <div className="absolute inset-0 bg-gradient-to-br from-teal-500 to-violet-600"></div>
      <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PHBhdHRlcm4gaWQ9ImdyaWQiIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHBhdGggZD0iTSAxMCAwIEwgMCAwIDAgMTAiIGZpbGw9Im5vbmUiIHN0cm9rZT0id2hpdGUiIHN0cm9rZS13aWR0aD0iMC41IiBvcGFjaXR5PSIwLjEiLz48L3BhdHRlcm4+PC9kZWZzPjxyZWN0IHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JpZCkiLz48L3N2Zz4=')] opacity-20"></div>

      <div className="container mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div className="max-w-4xl mx-auto text-center">
          {/* Main content */}
          <div className="mb-12">
            <h2 className="text-3xl sm:text-4xl lg:text-5xl text-white mb-6">
              Launch HealthLink+ For Your Practice
            </h2>
            <p className="text-lg sm:text-xl text-white/90 max-w-2xl mx-auto">
              Flexible roles, instant OTP setup, secure Zoom integration. Everything you need to modernize your healthcare operations.
            </p>
          </div>

          {/* CTAs */}
          <div className="flex flex-col sm:flex-row gap-4 justify-center mb-12">
            <Button 
              size="lg" 
              className="bg-white text-slate-900 hover:bg-slate-100 shadow-xl text-lg h-14 px-8"
            >
              Start Free Trial
              <ArrowRight className="w-5 h-5 ml-2" />
            </Button>
            <Button 
              size="lg" 
              variant="outline" 
              className="border-2 border-white text-white hover:bg-white/10 text-lg h-14 px-8"
            >
              Schedule a Call
            </Button>
          </div>

          {/* Features highlight */}
          <div className="grid sm:grid-cols-3 gap-6 max-w-3xl mx-auto">
            <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6 border border-white/20">
              <Calendar className="w-8 h-8 text-white mx-auto mb-3" />
              <p className="text-white text-sm">Unlimited appointments</p>
            </div>
            <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6 border border-white/20">
              <Video className="w-8 h-8 text-white mx-auto mb-3" />
              <p className="text-white text-sm">Zoom integration included</p>
            </div>
            <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6 border border-white/20">
              <Shield className="w-8 h-8 text-white mx-auto mb-3" />
              <p className="text-white text-sm">HIPAA-compliant security</p>
            </div>
          </div>

          {/* Trust line */}
          <div className="mt-12 pt-8 border-t border-white/20">
            <p className="text-white/80 text-sm">
              Join 500+ healthcare providers already using HealthLink+ • No credit card required • Setup in 5 minutes
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
