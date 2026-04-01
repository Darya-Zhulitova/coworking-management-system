import type {AccountTransaction, BalanceOverview, FinanceRequest, FinanceRequestStatus,} from "./types";

type MockState = {
  account: {
    membershipId: number;
    balance: string;
    currency: string;
    updatedAt: string;
  };
  financeRequests: FinanceRequest[];
  transactions: AccountTransaction[];
};

const state: MockState = {
  account: {
    membershipId: 1,
    balance: "5800.00",
    currency: "RUB",
    updatedAt: "2026-03-30T12:20:00.000Z",
  },
  financeRequests: [
    {
      id: 101,
      membershipId: 1,
      type: "DEPOSIT",
      amount: "3000.00",
      status: "COMPLETED",
      comment: "Пополнение для брони на следующую неделю",
      createdAt: "2026-03-18T09:30:00.000Z",
      processedAt: "2026-03-18T10:00:00.000Z",
    },
    {
      id: 102,
      membershipId: 1,
      type: "WITHDRAW",
      amount: "1200.00",
      status: "REJECTED",
      comment: "Ошибочный запрос",
      createdAt: "2026-03-21T15:10:00.000Z",
      processedAt: "2026-03-21T16:00:00.000Z",
    },
    {
      id: 103,
      membershipId: 1,
      type: "WITHDRAW",
      amount: "800.00",
      status: "PENDING",
      comment: "Вывести остаток депозита",
      createdAt: "2026-03-31T08:45:00.000Z",
      processedAt: null,
    },
  ],
  transactions: [
    {
      id: 201,
      membershipId: 1,
      type: "DEPOSIT_APPROVED",
      amount: "3000.00",
      description: "Менеджер подтвердил пополнение",
      createdAt: "2026-03-18T10:00:00.000Z",
    },
    {
      id: 202,
      membershipId: 1,
      type: "BOOKING_DEBIT",
      amount: "-1800.00",
      description: "Списание за бронь места A-12 на 3 рабочих дня",
      createdAt: "2026-03-20T11:15:00.000Z",
    },
    {
      id: 203,
      membershipId: 1,
      type: "COMPENSATION",
      amount: "600.00",
      description: "Компенсация за закрытый день 2026-03-25",
      createdAt: "2026-03-24T17:40:00.000Z",
    },
    {
      id: 204,
      membershipId: 1,
      type: "BOOKING_REFUND",
      amount: "4000.00",
      description: "Возврат по отмененной брони B-05",
      createdAt: "2026-03-29T14:05:00.000Z",
    },
  ],
};

function recalculateBalance() {
  const balance = state.transactions.reduce((sum, transaction) => {
    return sum + Number(transaction.amount);
  }, 0);

  state.account.balance = balance.toFixed(2);
  state.account.updatedAt = new Date().toISOString();
}

function sortByDateDesc<T extends { createdAt: string }>(items: T[]) {
  return [...items].sort((a, b) => Number(new Date(b.createdAt)) - Number(new Date(a.createdAt)));
}

export function getBalanceOverview(): BalanceOverview {
  recalculateBalance();

  return {
    account: {...state.account},
    pendingCount: state.financeRequests.filter((request) => request.status === "PENDING").length,
    financeRequests: sortByDateDesc(state.financeRequests),
    transactions: sortByDateDesc(state.transactions),
  };
}

export function createFinanceRequest(input: {
  membershipId: number;
  type: "DEPOSIT" | "WITHDRAW";
  amount: string;
  comment: string;
}): FinanceRequest {
  if (input.membershipId !== state.account.membershipId) {
    throw new Error("Membership не найден в моковых данных.");
  }

  const amount = Number(input.amount);

  if (!Number.isFinite(amount) || amount <= 0) {
    throw new Error("Сумма должна быть больше 0.");
  }

  recalculateBalance();

  if (input.type === "WITHDRAW") {
    const pendingWithdrawTotal = state.financeRequests
      .filter((request) => request.type === "WITHDRAW" && request.status === "PENDING")
      .reduce((sum, request) => sum + Number(request.amount), 0);

    const availableBalance = Number(state.account.balance) - pendingWithdrawTotal;

    if (amount > availableBalance) {
      throw new Error("Недостаточно средств для создания запроса на вывод.");
    }
  }

  const request: FinanceRequest = {
    id: Math.max(...state.financeRequests.map((item) => item.id)) + 1,
    membershipId: input.membershipId,
    type: input.type,
    amount: amount.toFixed(2),
    status: "PENDING",
    comment: input.comment.trim(),
    createdAt: new Date().toISOString(),
    processedAt: null,
  };

  state.financeRequests.push(request);

  return request;
}
