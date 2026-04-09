import {MembershipStatus, PayRequestStatus, ServiceRequestStatus} from '@/lib/types';

type StatusValue = MembershipStatus | PayRequestStatus | ServiceRequestStatus;

const styles: Record<StatusValue, string> = {
  active: 'text-bg-success',
  pending: 'text-bg-warning',
  blocked: 'text-bg-danger',
  Pending: 'text-bg-warning',
  Approved: 'text-bg-success',
  Rejected: 'text-bg-danger',
  new: 'text-bg-primary',
  in_progress: 'text-bg-info',
  resolved: 'text-bg-success',
  rejected: 'text-bg-danger',
};

const labels: Record<StatusValue, string> = {
  active: 'Активно',
  pending: 'Ожидание',
  blocked: 'Заблокировано',
  Pending: 'На рассмотрении',
  Approved: 'Подтверждено',
  Rejected: 'Отклонено',
  new: 'Новая',
  in_progress: 'В работе',
  resolved: 'Закрыта',
  rejected: 'Отклонена',
};

export function StatusBadge({status}: { status: StatusValue }) {
  return <span className={`badge ${styles[status]}`}>{labels[status]}</span>;
}
