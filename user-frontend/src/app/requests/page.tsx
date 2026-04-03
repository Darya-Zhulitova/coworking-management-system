import {ServiceRequestsPanel} from "@/components/features/service-requests/service-requests-panel";

export default function RequestsPage() {
  return (
    <div className="d-grid gap-4">
      <section className="card shadow-sm border-0">
        <div className="card-body p-4">
          <h2 className="h4 mb-1">Service requests</h2>
          <p className="text-body-secondary mb-0">Создание заявок и просмотр их текущих статусов.</p>
        </div>
      </section>
      <ServiceRequestsPanel/>
    </div>
  );
}
