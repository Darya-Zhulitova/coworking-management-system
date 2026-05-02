export interface UserLoginRequest {
  email: string;
  password: string;
}

export interface UserRegisterRequest {
  email: string;
  password: string;
  name: string;
  description?: string;
}

export interface UserAuthResponse {
  token: string;
  userId: number;
}
