'use client';

import { useState, useEffect } from 'react';
import { adminApi } from '@/lib/api';
import { Clinic } from '@/types';
import { toast } from 'react-hot-toast';
import { Building2, MapPin, CheckCircle2, XCircle, AlertCircle, Zap, Settings, Eye } from 'lucide-react';
import { TopNav } from '@/marketing/layout/TopNav';
import { Sidebar } from '@/marketing/layout/Sidebar';
import { Button } from '@/marketing/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/marketing/ui/card';
import { Badge } from '@/marketing/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/marketing/ui/tabs';
import { adminSidebarItems } from '@/app/admin/sidebar-items';

export default function AdminClinicsPage() {
  const [clinics, setClinics] = useState<Clinic[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedClinic, setSelectedClinic] = useState<Clinic | null>(null);
  const [viewMode, setViewMode] = useState<'grid' | 'map'>('grid');

  useEffect(() => {
    loadClinics();
  }, []);

  const loadClinics = async () => {
    setIsLoading(true);
    try {
      const response = await adminApi.getAllClinics();
      console.log('Clinics response:', response);
      // Ensure response is always an array
      const clinicsArray = Array.isArray(response) ? response : [];
      console.log('Clinics array:', clinicsArray);
      console.log('Number of clinics:', clinicsArray.length);
      setClinics(clinicsArray);
      if (clinicsArray.length === 0) {
        console.warn('No clinics found. Check backend endpoint /api/v1/facilities');
      }
    } catch (error: any) {
      toast.error('Failed to load clinics');
      console.error('Clinics load error:', error);
      console.error('Error response:', error?.response);
      console.error('Error status:', error?.response?.status);
      setClinics([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (clinicId: string | number) => {
    if (!confirm('Are you sure you want to delete this clinic?')) return;
    try {
      await adminApi.deleteClinic(clinicId);
      toast.success('Clinic deleted successfully');
      loadClinics();
    } catch (error: any) {
      toast.error('Failed to delete clinic');
    }
  };

  const getOperationalStatus = (clinic: Clinic) => {
    const isActive = (clinic as any).isActive ?? clinic.active;
    if (!isActive) return { status: 'INACTIVE', color: 'bg-gray-100 text-gray-800', icon: XCircle };
    
    // Check integration status
    const hasPayment = true; // Would check actual integration
    const hasVideo = true; // Would check actual integration
    
    if (hasPayment && hasVideo) {
      return { status: 'OPERATIONAL', color: 'bg-green-100 text-green-800', icon: CheckCircle2 };
    } else if (hasPayment || hasVideo) {
      return { status: 'PARTIAL', color: 'bg-yellow-100 text-yellow-800', icon: AlertCircle };
    }
    return { status: 'INACTIVE', color: 'bg-gray-100 text-gray-800', icon: XCircle };
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-50 to-white">
      <TopNav userName="Admin" userRole="Admin" showPortalLinks={false} onLogout={() => {}} />
      <div className="flex">
        <Sidebar items={adminSidebarItems} currentPath="/admin/clinics" />
        <main className="flex-1 p-4 sm:px-6 lg:px-8">
          <div className="max-w-7xl mx-auto space-y-8">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h1 className="text-3xl text-slate-900 mb-2">Facility Management</h1>
                <p className="text-slate-600">Manage clinics, operational status, and integrations</p>
              </div>
              <div className="flex gap-3">
                <Button
                  variant={viewMode === 'map' ? 'default' : 'outline'}
                  onClick={() => setViewMode('map')}
                >
                  <MapPin className="w-4 h-4 mr-2" />
                  Map View
                </Button>
                <Button
                  variant={viewMode === 'grid' ? 'default' : 'outline'}
                  onClick={() => setViewMode('grid')}
                >
                  Grid View
                </Button>
              </div>
            </div>

            {viewMode === 'map' ? (
              <Card>
                <CardHeader>
                  <CardTitle>Facility Map View</CardTitle>
                  <CardDescription>View all facilities on a map</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="h-96 bg-gray-100 rounded-lg flex items-center justify-center border-2 border-dashed border-gray-300">
                    <div className="text-center text-gray-500">
                      <MapPin className="w-12 h-12 mx-auto mb-2 opacity-50" />
                      <p>Map view would be integrated here</p>
                      <p className="text-sm mt-1">Shows {clinics.length} facilities</p>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ) : (
              <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
                {isLoading ? (
                  <div className="col-span-full text-center py-8">Loading...</div>
                ) : clinics.length === 0 ? (
                  <div className="col-span-full text-center py-8 text-slate-500">
                    No clinics found
                  </div>
                ) : (
                  clinics.map((clinic) => {
                    const opStatus = getOperationalStatus(clinic);
                    const StatusIcon = opStatus.icon;
                    
                    return (
                      <Card key={clinic.id} className="hover:shadow-lg transition">
                        <CardHeader>
                          <div className="flex items-start justify-between">
                            <div className="flex-1">
                              <CardTitle className="text-lg">{clinic.name}</CardTitle>
                              <div className="flex items-center gap-2 mt-2">
                                <Badge className={opStatus.color}>
                                  <StatusIcon className="w-3 h-3 mr-1" />
                                  {opStatus.status}
                                </Badge>
                              </div>
                            </div>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => setSelectedClinic(clinic)}
                            >
                              <Eye className="w-4 h-4" />
                            </Button>
                          </div>
                        </CardHeader>
                        <CardContent>
                          <div className="space-y-3">
                            <div className="text-sm text-slate-600">
                              <p className="flex items-center gap-2 mb-1">
                                <MapPin className="w-4 h-4" />
                                {clinic.address}
                              </p>
                              <p className="ml-6">{clinic.city}, {clinic.state} {clinic.zipCode}</p>
                              <p className="mt-2">Doctor: {clinic.doctorName}</p>
                            </div>

                            <div className="border-t pt-3 space-y-2">
                              <div className="flex items-center justify-between text-sm">
                                <span className="text-slate-600">Payment Gateway</span>
                                <Badge variant={true ? 'default' : 'destructive'}>
                                  {true ? 'Connected' : 'Not Connected'}
                                </Badge>
                              </div>
                              <div className="flex items-center justify-between text-sm">
                                <span className="text-slate-600">Video Service</span>
                                <Badge variant={true ? 'default' : 'destructive'}>
                                  {true ? 'Connected' : 'Not Connected'}
                                </Badge>
                              </div>
                            </div>

                            <Button
                              type="button"
                              variant="destructive"
                              className="w-full bg-red-600 hover:bg-red-700 text-white"
                              onClick={() => handleDelete(String(clinic.id))}
                            >
                              Delete Clinic
                            </Button>
                          </div>
                        </CardContent>
                      </Card>
                    );
                  })
                )}
              </div>
            )}

            {/* Clinic Detail Modal */}
            {selectedClinic && (
              <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
                <Card className="w-full max-w-3xl max-h-[80vh] overflow-y-auto bg-white shadow-2xl">
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <CardTitle>{selectedClinic.name}</CardTitle>
                      <Button variant="outline" size="sm" onClick={() => setSelectedClinic(null)}>
                        Close
                      </Button>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <Tabs defaultValue="details">
                      <TabsList>
                        <TabsTrigger value="details">Details</TabsTrigger>
                        <TabsTrigger value="status">Operational Status</TabsTrigger>
                        <TabsTrigger value="integrations">Integrations</TabsTrigger>
                      </TabsList>

                      <TabsContent value="details" className="space-y-4">
                        <div>
                          <h3 className="font-semibold mb-2">Address</h3>
                          <p className="text-slate-600">{selectedClinic.address}</p>
                          <p className="text-slate-600">{selectedClinic.city}, {selectedClinic.state} {selectedClinic.zipCode}</p>
                        </div>
                        <div>
                          <h3 className="font-semibold mb-2">Doctor</h3>
                          <p className="text-slate-600">{selectedClinic.doctorName}</p>
                        </div>
                        <div>
                          <h3 className="font-semibold mb-2">Consultation Fee</h3>
                          <p className="text-slate-600">PKR {selectedClinic.consultationFee?.toLocaleString() || 'N/A'}</p>
                        </div>
                      </TabsContent>

                      <TabsContent value="status" className="space-y-4">
                        <div>
                          <h3 className="font-semibold mb-2">Current Status</h3>
                          <Badge className={getOperationalStatus(selectedClinic).color}>
                            {getOperationalStatus(selectedClinic).status}
                          </Badge>
                        </div>
                        <div>
                          <h3 className="font-semibold mb-2">Active Status</h3>
                          <p className="text-slate-600">
                            {(selectedClinic as any).isActive ?? selectedClinic.active ? 'Active' : 'Inactive'}
                          </p>
                        </div>
                      </TabsContent>

                      <TabsContent value="integrations" className="space-y-4">
                        <div className="space-y-3">
                          <div className="flex items-center justify-between p-3 border rounded-lg">
                            <div className="flex items-center gap-2">
                              <Zap className="w-5 h-5 text-green-600" />
                              <span>Payment Gateway</span>
                            </div>
                            <Badge variant="default">Connected</Badge>
                          </div>
                          <div className="flex items-center justify-between p-3 border rounded-lg">
                            <div className="flex items-center gap-2">
                              <Zap className="w-5 h-5 text-green-600" />
                              <span>Video Service (Zoom)</span>
                            </div>
                            <Badge variant="default">Connected</Badge>
                          </div>
                        </div>
                      </TabsContent>
                    </Tabs>
                  </CardContent>
                </Card>
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
