import { ServiceRequestsPanel } from "@/components/features/service-requests/service-requests-panel";

export default function RequestsPage() {
  return (
    <div>
      <div className="panel">
        <h2>Service requests</h2>
      </div>
      <ServiceRequestsPanel />
    </div>
  );
}
