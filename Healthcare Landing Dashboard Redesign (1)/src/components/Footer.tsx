import { Activity, Mail, Phone, MapPin } from "lucide-react";

export function Footer() {
  const footerSections = [
    {
      title: "Product",
      links: [
        { name: "Features", href: "#features" },
        { name: "How it works", href: "#how-it-works" },
        { name: "Integrations", href: "#integrations" },
        { name: "Pricing", href: "#pricing" }
      ]
    },
    {
      title: "Roles",
      links: [
        { name: "For Patients", href: "#patients" },
        { name: "For Doctors", href: "#doctors" },
        { name: "For Admins", href: "#admins" },
        { name: "For Clinics", href: "#clinics" }
      ]
    },
    {
      title: "Security & Compliance",
      links: [
        { name: "HIPAA Compliance", href: "#hipaa" },
        { name: "Data Security", href: "#security" },
        { name: "Privacy Policy", href: "#privacy" },
        { name: "Terms of Service", href: "#terms" }
      ]
    },
    {
      title: "Support",
      links: [
        { name: "Help Center", href: "#help" },
        { name: "Documentation", href: "#docs" },
        { name: "API Reference", href: "#api" },
        { name: "Contact Us", href: "#contact" }
      ]
    }
  ];

  return (
    <footer className="bg-slate-900 text-slate-300">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8 py-16">
        <div className="grid sm:grid-cols-2 lg:grid-cols-6 gap-12 mb-12">
          {/* Brand column */}
          <div className="lg:col-span-2">
            <div className="flex items-center gap-2 mb-4">
              <div className="w-10 h-10 bg-gradient-to-br from-teal-500 to-violet-600 rounded-xl flex items-center justify-center">
                <Activity className="w-6 h-6 text-white" />
              </div>
              <span className="text-xl text-white">HealthLink+</span>
            </div>
            <p className="text-slate-400 mb-6 max-w-sm">
              The all-in-one platform connecting patients, doctors, and admins through unified appointment management and telemedicine.
            </p>
            
            {/* Contact info */}
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <Mail className="w-4 h-4 text-teal-500" />
                <span className="text-sm">support@healthlinkplus.com</span>
              </div>
              <div className="flex items-center gap-2">
                <Phone className="w-4 h-4 text-teal-500" />
                <span className="text-sm">1-800-HEALTH-LINK</span>
              </div>
              <div className="flex items-center gap-2">
                <MapPin className="w-4 h-4 text-teal-500" />
                <span className="text-sm">San Francisco, CA</span>
              </div>
            </div>
          </div>

          {/* Footer sections */}
          {footerSections.map((section, index) => (
            <div key={index}>
              <h3 className="text-white mb-4">{section.title}</h3>
              <ul className="space-y-3">
                {section.links.map((link, linkIndex) => (
                  <li key={linkIndex}>
                    <a 
                      href={link.href} 
                      className="text-slate-400 hover:text-white transition text-sm"
                    >
                      {link.name}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* Bottom bar */}
        <div className="pt-8 border-t border-slate-800">
          <div className="flex flex-col md:flex-row items-center justify-between gap-4">
            <p className="text-sm text-slate-400">
              © 2025 HealthLink+. All rights reserved.
            </p>
            
            <div className="flex items-center gap-6">
              <a href="#" className="text-sm text-slate-400 hover:text-white transition">
                Privacy Policy
              </a>
              <a href="#" className="text-sm text-slate-400 hover:text-white transition">
                Terms of Service
              </a>
              <a href="#" className="text-sm text-slate-400 hover:text-white transition">
                Cookie Policy
              </a>
            </div>
          </div>

          {/* Trust badges */}
          <div className="flex flex-wrap items-center justify-center gap-6 mt-8">
            <div className="flex items-center gap-2 px-4 py-2 bg-slate-800 rounded-lg">
              <Activity className="w-4 h-4 text-teal-500" />
              <span className="text-xs text-slate-300">HIPAA Compliant</span>
            </div>
            <div className="flex items-center gap-2 px-4 py-2 bg-slate-800 rounded-lg">
              <Activity className="w-4 h-4 text-violet-500" />
              <span className="text-xs text-slate-300">SOC 2 Type II</span>
            </div>
            <div className="flex items-center gap-2 px-4 py-2 bg-slate-800 rounded-lg">
              <Activity className="w-4 h-4 text-blue-500" />
              <span className="text-xs text-slate-300">GDPR Ready</span>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
}
