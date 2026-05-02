import { MembershipStatus, PayRequestStatus, ServiceRequestStatus } from '@/lib/types';

type StatusValue = MembershipStatus | PayRequestStatus | ServiceRequestStatus;

const styles: Record<StatusValue, string> = {
  active: 'text-bg-success',
  pending: 'text-bg-secondary',
  blocked: 'text-bg-danger',
  Pending: 'text-bg-secondary',
  Approved: 'text-bg-success',
  Rejected: 'text-bg-danger',
  new: 'text-bg-primary',
  in_progress: 'text-bg-secondary',
  resolved: 'text-bg-success',
  rejected: 'text-bg-danger',
};

const labels: Record<StatusValue, string> = {
  active: 'Активно',
  pending: 'Ожидает подтверждения',
  blocked: 'Доступ заблокирован',
  Pending: 'На рассмотрении',
  Approved: 'Подтверждено',
  Rejected: 'Отклонено',
  new: 'Новая',
  in_progress: 'В работе',
  resolved: 'Закрыта',
  rejected: 'Отклонена',
};

export function StatusBadge({ status }: { status: StatusValue }) {
  return <span className={`badge fw-medium ${styles[status]}`}>{labels[status]}</span>;
}
