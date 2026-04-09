import Link from 'next/link';

export function AuthPage({mode}: { mode: 'login' | 'register' }) {
  const isLogin = mode === 'login';

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-md-8 col-lg-5">
        <div className="card shadow-sm border-0">
          <div className="card-body p-4 p-lg-5">
            <div className="mb-4">
              <h1 className="h3 mb-2">{isLogin ? 'Вход в аккаунт' : 'Создание аккаунта'}</h1>
              <p className="text-body-secondary mb-0">
                {isLogin
                  ? 'Войдите, чтобы выбрать коворкинг и продолжить работу.'
                  : 'Создайте аккаунт пользователя, чтобы подключаться к коворкингам по приглашению.'}
              </p>
            </div>

            <form className="d-grid gap-3">
              {!isLogin && (
                <div>
                  <label className="form-label">Имя</label>
                  <input className="form-control" placeholder="Дарья Морозова"/>
                </div>
              )}
              <div>
                <label className="form-label">Email</label>
                <input className="form-control" type="email" placeholder="you@example.com"/>
              </div>
              {!isLogin && (
                <div>
                  <label className="form-label">О себе</label>
                  <textarea className="form-control" rows={3} placeholder="Несколько слов о ваших рабочих предпочтениях."/>
                </div>
              )}
              <div>
                <label className="form-label">Пароль</label>
                <input className="form-control" type="password" placeholder="••••••••"/>
              </div>
              <button type="button" className="btn btn-primary btn-lg">
                {isLogin ? 'Войти' : 'Создать аккаунт'}
              </button>
            </form>

            <div className="mt-4 text-body-secondary small">
              {isLogin ? (
                <>Нет аккаунта? <Link href="/register">Зарегистрироваться</Link></>
              ) : (
                <>Уже есть аккаунт? <Link href="/login">Войти</Link></>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
