const GRANT_LABELS: Record<string, string> = {
  COWORKING_READ: 'Просмотр коворкинга',
  COWORKING_EDIT: 'Редактирование коворкинга',
  FLOOR_READ: 'Просмотр этажей',
  FLOOR_EDIT: 'Редактирование этажей',
  PLACE_TYPE_READ: 'Просмотр типов мест',
  PLACE_TYPE_EDIT: 'Редактирование типов мест',
  PLACE_READ: 'Просмотр мест',
  PLACE_EDIT: 'Редактирование мест',
  TARIFF_READ: 'Просмотр тарифов',
  TARIFF_EDIT: 'Редактирование тарифов',
  SERVICE_REQUEST_TYPE_READ: 'Просмотр типов сервисных заявок',
  SERVICE_REQUEST_TYPE_EDIT: 'Редактирование типов сервисных заявок',
  ROLE_READ: 'Просмотр ролей',
  ROLE_EDIT: 'Редактирование ролей',
  ACCESS_READ: 'Просмотр сотрудников',
  ACCESS_EDIT: 'Редактирование сотрудников',
  SCHEDULE_READ: 'Просмотр расписания',
  SCHEDULE_EDIT: 'Редактирование расписания',
  USER_READ: 'Просмотр пользователей',
  USER_EDIT: 'Управление пользовательскими заявками',
  BOOKING_READ: 'Просмотр бронирований',
  BOOKING_EDIT: 'Управление бронированиями',
};

const ROLE_LABELS: Record<string, string> = {
  Owner: 'Владелец',
  OWNER: 'Владелец',
  Admin: 'Администратор',
  ADMIN: 'Администратор',
};

const MEMBERSHIP_STATUS_LABELS: Record<string, string> = {
  pending: 'Ожидает подтверждения',
  active: 'Активно',
  blocked: 'Заблокировано',
};

const PAY_REQUEST_STATUS_LABELS: Record<string, string> = {
  PENDING: 'Ожидает решения',
  APPROVED: 'Подтверждён',
  REJECTED: 'Отклонён',
};

const SERVICE_REQUEST_STATUS_LABELS: Record<string, string> = {
  new: 'Новая',
  in_progress: 'В работе',
  resolved: 'Закрыта',
  rejected: 'Отклонена',
};

const MESSAGE_AUTHOR_LABELS: Record<string, string> = {
  USER: 'Пользователь',
  ADMIN: 'Администратор',
  SYSTEM: 'Система',
};

const SCHEDULE_EXCEPTION_TYPE_LABELS: Record<string, string> = {
  Open: 'Открыто',
  Close: 'Закрыто',
  OPEN: 'Открыто',
  CLOSE: 'Закрыто',
};

const LEDGER_TYPE_LABELS: Record<string, string> = {
  DEPOSIT: 'Пополнение',
  WITHDRAWAL: 'Вывод средств',
  BOOKING_CHARGE: 'Оплата бронирования',
  CANCELLATION_REFUND: 'Возврат за отмену',
  DAY_CLOSURE_COMPENSATION: 'Компенсация за закрытие дня',
  MEMBERSHIP_BLOCK_COMPENSATION: 'Компенсация при блокировке участия',
  MANUAL_CREDIT: 'Ручное начисление',
  MANUAL_DEBIT: 'Ручное списание',
  SERVICE_REQUEST_CHARGE: 'Оплата сервисной заявки',
};

export function formatGrantLabel(value: string): string {
  return GRANT_LABELS[value] ?? value;
}

export function formatGrantList(values: string[]): string {
  return values.map(formatGrantLabel).join(', ');
}

export function formatRoleLabel(value: string | null | undefined): string {
  if (!value) return '';
  return ROLE_LABELS[value] ?? value;
}

export function formatMembershipStatus(value: string): string {
  return MEMBERSHIP_STATUS_LABELS[value] ?? value;
}

export function formatPayRequestStatus(value: string): string {
  return PAY_REQUEST_STATUS_LABELS[value] ?? value;
}

export function formatServiceRequestStatus(value: string): string {
  return SERVICE_REQUEST_STATUS_LABELS[value] ?? value;
}

export function formatMessageAuthorType(value: string): string {
  return MESSAGE_AUTHOR_LABELS[value] ?? value;
}

export function formatScheduleExceptionType(value: string): string {
  return SCHEDULE_EXCEPTION_TYPE_LABELS[value] ?? value;
}

export function formatLedgerType(value: string): string {
  return LEDGER_TYPE_LABELS[value] ?? value;
}

export function formatActiveStatus(active: boolean): string {
  return active ? 'Активно' : 'Неактивно';
}

const USER_DOMAIN_COMMAND_LABELS: Record<string, string> = {
  CLOSE_DAY_COMPENSATION: 'Компенсация за закрытие дня',
  PLACE_CLOSING_COMPENSATION: 'Компенсация за закрытие места',
  PLACE_DEACTIVATION_COMPENSATION: 'Компенсация за деактивацию места',
  MEMBERSHIP_BLOCK_COMPENSATION: 'Компенсация при блокировке участия',
};

export function formatUserDomainCommand(value: string): string {
  return USER_DOMAIN_COMMAND_LABELS[value] ?? value;
}

export function formatUserDomainCommandList(values: string[]): string {
  return values.map(formatUserDomainCommand).join(', ');
}
