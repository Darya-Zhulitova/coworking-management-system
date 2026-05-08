export interface AppContextDto {
  id: number;
  email: string;
  name: string;
  role: string | null;
  grants: string[];
  coworkingId: number | null;
  coworkingName: string | null;
}
