import {NextRequest, NextResponse} from "next/server";
import {createFinanceRequest} from "@/lib/mock-balance-data";

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();

    const financeRequest = createFinanceRequest({
      membershipId: Number(body.membershipId),
      type: body.type,
      amount: String(body.amount),
      comment: String(body.comment ?? ""),
    });

    return NextResponse.json(financeRequest, {status: 201});
  } catch (error) {
    return NextResponse.json(
      {
        message: error instanceof Error ? error.message : "Не удалось создать финансовый запрос.",
      },
      {status: 400}
    );
  }
}
