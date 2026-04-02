import {NextResponse} from "next/server";
import {getBalanceOverview} from "@/lib/mock-balance-data";

export async function GET() {
  return NextResponse.json(getBalanceOverview());
}
