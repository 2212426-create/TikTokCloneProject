import { PlaySquare, ShieldCheck, Loader2 } from 'lucide-react';
import { useAuth } from '../components/auth-provider';

export function LoginPage() {
  const { signInWithGoogle, loading, error } = useAuth();

  return (
    <div className="min-h-screen bg-surface flex items-center justify-center p-4 relative overflow-hidden">
      {/* Background decorations */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-[-20%] left-[-10%] w-[500px] h-[500px] rounded-full bg-primary/5 blur-3xl"></div>
        <div className="absolute bottom-[-20%] right-[-10%] w-[600px] h-[600px] rounded-full bg-secondary/5 blur-3xl"></div>
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] rounded-full bg-primary/3 blur-[100px]"></div>
      </div>

      {/* Login Card */}
      <div className="relative w-full max-w-md">
        <div className="bg-surface-low border border-outline-variant/20 rounded-2xl shadow-2xl overflow-hidden">
          {/* Top gradient line */}
          <div className="h-[2px] bg-gradient-to-r from-primary via-secondary to-primary"></div>
          
          <div className="p-10">
            {/* Logo & Brand */}
            <div className="text-center mb-10">
              <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-primary/10 border border-primary/20 mb-5">
                <PlaySquare className="w-8 h-8 text-primary" fill="currentColor" />
              </div>
              <h1 className="font-headline text-3xl font-bold text-on-surface mb-2">TopTop Admin</h1>
              <p className="text-on-surface-variant font-body text-sm">Hệ thống quản trị nội dung</p>
            </div>

            {/* Role requirement notice */}
            <div className="flex items-start gap-3 p-4 rounded-xl bg-surface-high/50 border border-outline-variant/10 mb-8">
              <ShieldCheck className="w-5 h-5 text-primary mt-0.5 flex-shrink-0" />
              <div>
                <p className="font-label text-sm text-on-surface font-medium">Yêu cầu phân quyền</p>
                <p className="font-label text-xs text-on-surface-variant mt-1">
                  Chỉ tài khoản có vai trò <strong className="text-primary">Admin</strong> hoặc <strong className="text-secondary-container">Moderator</strong> mới được truy cập.
                </p>
              </div>
            </div>

            {/* Error message */}
            {error && (
              <div className="p-4 rounded-xl bg-error/10 border border-error/20 mb-6">
                <p className="font-label text-sm text-error">{error}</p>
              </div>
            )}

            {/* Google Sign In Button */}
            <button
              onClick={signInWithGoogle}
              disabled={loading}
              className="w-full flex items-center justify-center gap-3 px-6 py-4 bg-surface border border-outline-variant/30 rounded-xl font-label text-base font-medium text-on-surface hover:bg-surface-high hover:border-primary/30 hover:shadow-lg hover:shadow-primary/10 transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed group"
            >
              {loading ? (
                <Loader2 className="w-5 h-5 animate-spin text-primary" />
              ) : (
                <svg className="w-5 h-5" viewBox="0 0 24 24">
                  <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4"/>
                  <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                  <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
                  <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
                </svg>
              )}
              <span>{loading ? 'Đang đăng nhập...' : 'Đăng nhập bằng Google'}</span>
            </button>

            {/* Footer */}
            <p className="text-center text-on-surface-variant font-label text-xs mt-8">
              Sử dụng tài khoản Google đã được cấp quyền bởi quản trị viên hệ thống.
            </p>
          </div>
        </div>

        {/* Bottom branding */}
        <p className="text-center text-on-surface-variant/50 font-label text-xs mt-6">
          TopTop Management v1.0 · Firebase Authentication
        </p>
      </div>
    </div>
  );
}
