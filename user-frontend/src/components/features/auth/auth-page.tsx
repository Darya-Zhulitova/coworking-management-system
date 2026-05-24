'use client';

import Link from 'next/link';
import { FormEvent, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useToast } from '@/components/ui/toast-provider';

interface AuthPageProps {
  mode: 'login' | 'register';
}

interface LoginFormState {
  email: string;
  password: string;
}

interface RegisterFormState extends LoginFormState {
  name: string;
  description: string;
}

export function AuthPage({ mode }: AuthPageProps) {
  const isLogin = mode === 'login';
  const router = useRouter();
  const searchParams = useSearchParams();
  const toast = useToast();
  const [loginState, setLoginState] = useState<LoginFormState>({ email: '', password: '' });
  const [registerState, setRegisterState] = useState<RegisterFormState>({
    name: '',
    email: '',
    description: '',
    password: ''
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);

    const path = isLogin ? '/api/user/login' : '/api/user/register';
    const payload = isLogin ? loginState : registerState;

    try {
      const response = await fetch(path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify(payload),
      });
      const data = (await response.json().catch(() => null)) as { message?: string } | null;
      if (!response.ok) {
        throw new Error(data?.message || (isLogin ? 'Не удалось войти в систему.' : 'Не удалось создать аккаунт.'));
      }
      const nextUrl = searchParams.get('next');
      router.replace(nextUrl && nextUrl.startsWith('/') ? nextUrl : '/');
      router.refresh();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : (isLogin ? 'Не удалось войти в систему.' : 'Не удалось создать аккаунт.'));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-md-8 col-lg-6 col-xl-5">
        <div className="card shadow-sm border-0">
          <div className="card-body p-4 p-lg-5">
            <div className="mb-4">
              <h1 className="h3 mb-2">{isLogin ? 'Вход' : 'Регистрация'}</h1>
              <p className="text-body-secondary mb-0">
                {isLogin
                  ? 'Войдите, чтобы работать с коворкингами, бронированиями и сервисными заявками.'
                  : 'Создайте аккаунт, чтобы подключаться к коворкингам и управлять своими услугами.'}
              </p>
            </div>

            <form className="d-grid gap-3" onSubmit={handleSubmit}>
              {!isLogin ? (
                <div>
                  <label className="form-label">Имя</label>
                  <input
                    className="form-control"
                    placeholder="Например, Дарья Морозова"
                    value={registerState.name}
                    onChange={(event) => setRegisterState((state) => ({ ...state, name: event.target.value }))}
                    required
                  />
                </div>
              ) : null}

              <div>
                <label className="form-label">Электронная почта</label>
                <input
                  className="form-control"
                  type="email"
                  placeholder="you@example.com"
                  value={isLogin ? loginState.email : registerState.email}
                  onChange={(event) =>
                    isLogin
                      ? setLoginState((state) => ({ ...state, email: event.target.value }))
                      : setRegisterState((state) => ({ ...state, email: event.target.value }))
                  }
                  required
                />
              </div>

              {!isLogin ? (
                <div>
                  <label className="form-label">О себе</label>
                  <textarea
                    className="form-control"
                    rows={3}
                    placeholder="Коротко расскажите о себе и своих рабочих предпочтениях."
                    value={registerState.description}
                    onChange={(event) => setRegisterState((state) => ({ ...state, description: event.target.value }))}
                  />
                </div>
              ) : null}

              <div>
                <label className="form-label">Пароль</label>
                <input
                  className="form-control"
                  type="password"
                  placeholder="Не менее 6 символов"
                  value={isLogin ? loginState.password : registerState.password}
                  onChange={(event) =>
                    isLogin
                      ? setLoginState((state) => ({ ...state, password: event.target.value }))
                      : setRegisterState((state) => ({ ...state, password: event.target.value }))
                  }
                  minLength={6}
                  required
                />
              </div>


              <button type="submit" className="btn btn-primary btn-lg" disabled={isSubmitting}>
                {isSubmitting ? (isLogin ? 'Выполняется вход...' : 'Создаем аккаунт...') : (isLogin ? 'Войти' : 'Создать аккаунт')}
              </button>
            </form>

            <div className="mt-4 text-body-secondary small">
              {isLogin ? (
                <>
                  Нет аккаунта? <Link
                  href={searchParams.get('next') ? `/register?next=${encodeURIComponent(searchParams.get('next') ?? '')}` : '/register'}>Зарегистрироваться</Link>
                </>
              ) : (
                <>
                  Уже есть аккаунт? <Link
                  href={searchParams.get('next') ? `/login?next=${encodeURIComponent(searchParams.get('next') ?? '')}` : '/login'}>Войти</Link>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
