// =============================================
// Types đồng bộ với App Android (Firestore)
// =============================================

// --- User ---
export type UserRole = 'admin' | 'moderator' | 'viewer' | 'user';
export type UserStatus = 'active' | 'banned' | 'warned';

export interface User {
  userId: string;
  username: string;
  avatarUrl?: string;
  email: string;
  phone?: string;
  birthdate?: string;
  isPrivate: boolean;
  followers: number;
  following: number;
  likes: number;
  // Admin-only fields
  role: UserRole;
  status: UserStatus;
  createdAt: number; // timestamp
}

// --- Video (đồng bộ với Video.java) ---
export type ModerationStatus = 'pending' | 'approved' | 'rejected';

export interface Video {
  videoId: string;
  videoUri: string;
  authorId: string;
  username: string;
  description: string;
  timestamp: number;
  totalLikes: number;
  totalComments: number;
  watchCount: number;
  // Admin-only moderation fields
  moderationStatus: ModerationStatus;
  aiFlagged: boolean;
  aiConfidence: number;
  aiReviewed?: boolean;
  rejectedReason?: string;
  reviewedBy?: string;
}

// --- Report ---
export type ReportTargetType = 'video' | 'user' | 'comment';
export type ReportStatus = 'pending' | 'resolved' | 'dismissed';

export interface Report {
  id: string;
  reporterId: string;
  targetType: ReportTargetType;
  targetId: string;
  reason: string;
  details: string;
  status: ReportStatus;
  createdAt: number;
  handledBy?: string;
}

// --- Audit Log ---
export interface AuditLog {
  id: string;
  adminId: string;
  action: string;
  targetId: string;
  details: Record<string, string>;
  createdAt: number;
}
