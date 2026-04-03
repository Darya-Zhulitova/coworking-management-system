import {MembershipsPanel} from "@/components/features/memberships/memberships-panel";

export default function BalancePage() {
  return (
    <div className="d-grid gap-4">
      <section className="card shadow-sm border-0">
        <div className="card-body p-4">
          <h2 className="h4 mb-1">Memberships</h2>
          <p className="text-body-secondary mb-0">Управление участием пользователя в коворкингах.</p>
        </div>
      </section>
      <MembershipsPanel/>
    </div>
  );
}
