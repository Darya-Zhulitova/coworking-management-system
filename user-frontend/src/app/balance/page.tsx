import {BalancePanel} from "@/components/features/balance/balance-panel";

export default function BalancePage() {
  return (
    <div className="d-grid gap-4">
      <section className="card shadow-sm border-0">
        <div className="card-body p-4">
          <h2 className="h4 mb-1">Balance page</h2>
          <p className="text-body-secondary mb-0">Баланс, финансовые запросы и история операций.</p>
        </div>
      </section>
      <BalancePanel/>
    </div>
  );
}
