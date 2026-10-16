/// Hằng số chuỗi văn bản cho ứng dụng ViFarm
class AppStrings {
  AppStrings._();

  static const String appName = 'ViFarm';

  // Login / Register / Auth
  static const String hintPassword = 'Enter your password';
  static const String hintConfirm = 'Confirm your password';
  static const String btnChoiceGoogle = 'Using Google Account';
  static const String btnChoiceFacebook = 'Using facebook account';
  static const String btnSignout = 'Đăng xuất';
  static const String signUp = 'Sign Up';
  static const String signIn = 'Sign In';
  static const String signUpAlt = 'Already have an account? Sign in.';
  static const String signInAlt = 'Don\'t have an account yet? Sign up.';
  static const String hintEnterEmail = 'Enter your email';
  static const String requestAccountTitle = 'You are anonymous';
  static const String requestAccountMessage = 'Would you like you sign up or sign in?';

  // Home / Feed
  static const String home = 'Home';
  static const String friends = 'Friends';
  static const String inbox = 'Inbox';
  static const String profile = 'Profile';
  static const String following = 'Following';
  static const String forYou = 'For you';

  // Post / Video Upload
  static const String hintDescription = 'Enter your description';
  static const String post = 'Post';

  // Search
  static const String search = 'Search';
  static const String actionSearch = 'Search...';

  // Edit Profile / Settings
  static const String changePhoto = 'Change Photo';
  static const String edit = 'Edit';
  static const String apply = 'Apply';
  static const String save = 'Save';
  static const String nameLabel = 'Name:';
  static const String usernameLabel = 'Username:';
  static const String emailLabel = 'Email:';
  static const String phoneLabel = 'Phone:';
  static const String birthdateLabel = 'Birthdate:';

  // Notifications Templates
  static const String templateFollow = 'started following you';
  static const String templateComment = 'commented on your video';
  static const String templateLike = 'liked your video';
  static const String templateChat = 'sent you a message';

  // Errors / Success Messages
  static const String errorPassword = 'Password only include alphabet character and number';
  static const String errorExistedEmail = 'Account with this email has existed';
  static const String errorConfirm = 'Confirm password is not correct';
  static const String errorVerify = 'Verify Failed. Try again!';
  static const String errorSignin = 'Account does not exist. Please sign up!';
  static const String errorOldPassword = 'Current password is incorrect';
  static const String errorChangePasswordNotSupported = 'This account cannot change password here';
  static const String errorUsername = 'Usernames can contain letters, numbers, "_" but the first character must not be a number and the minimum length is 3';
  static const String errorBirthdate = 'Birthdate is invalid or under 16 years old';
  static const String existUsername = 'This username has existed';
  static const String errorUndefined = 'Something is wrong. Please try again!';
  static const String errorUploadVideo = 'Video must have duration less than 15s and maximum resolution is 720p';

  static const String successfulSignin = 'Sign in successfully';
  static const String successfulChangePassword = 'Change password successfully';
}
